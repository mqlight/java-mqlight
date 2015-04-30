/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ibm.mqlight.api.impl;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.Type;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;

import com.github.oxo42.stateless4j.StateMachine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.ClientOptions;
import com.ibm.mqlight.api.ClientState;
import com.ibm.mqlight.api.CompletionListener;
import com.ibm.mqlight.api.DestinationListener;
import com.ibm.mqlight.api.NetworkException;
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.NonBlockingClientListener;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.ReplacedException;
import com.ibm.mqlight.api.SendOptions;
import com.ibm.mqlight.api.StartingException;
import com.ibm.mqlight.api.StateException;
import com.ibm.mqlight.api.StoppedException;
import com.ibm.mqlight.api.SubscribeOptions;
import com.ibm.mqlight.api.SubscribedException;
import com.ibm.mqlight.api.UnsubscribedException;
import com.ibm.mqlight.api.callback.CallbackService;
import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointService;
import com.ibm.mqlight.api.impl.callback.CallbackExceptionNotification;
import com.ibm.mqlight.api.impl.callback.CallbackPromiseImpl;
import com.ibm.mqlight.api.impl.callback.FlushResponse;
import com.ibm.mqlight.api.impl.callback.ThreadPoolCallbackService;
import com.ibm.mqlight.api.impl.endpoint.BluemixEndpointService;
import com.ibm.mqlight.api.impl.endpoint.EndpointPromiseImpl;
import com.ibm.mqlight.api.impl.endpoint.EndpointResponse;
import com.ibm.mqlight.api.impl.endpoint.ExhaustedResponse;
import com.ibm.mqlight.api.impl.endpoint.SingleEndpointService;
import com.ibm.mqlight.api.impl.engine.CloseRequest;
import com.ibm.mqlight.api.impl.engine.CloseResponse;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;
import com.ibm.mqlight.api.impl.engine.DeliveryResponse;
import com.ibm.mqlight.api.impl.engine.DisconnectNotification;
import com.ibm.mqlight.api.impl.engine.DrainNotification;
import com.ibm.mqlight.api.impl.engine.Engine;
import com.ibm.mqlight.api.impl.engine.EngineConnection;
import com.ibm.mqlight.api.impl.engine.OpenRequest;
import com.ibm.mqlight.api.impl.engine.OpenResponse;
import com.ibm.mqlight.api.impl.engine.SendRequest;
import com.ibm.mqlight.api.impl.engine.SendResponse;
import com.ibm.mqlight.api.impl.engine.SubscribeRequest;
import com.ibm.mqlight.api.impl.engine.SubscribeResponse;
import com.ibm.mqlight.api.impl.engine.UnsubscribeRequest;
import com.ibm.mqlight.api.impl.engine.UnsubscribeResponse;
import com.ibm.mqlight.api.impl.network.NettyNetworkService;
import com.ibm.mqlight.api.impl.timer.CancelResponse;
import com.ibm.mqlight.api.impl.timer.PopResponse;
import com.ibm.mqlight.api.impl.timer.TimerPromiseImpl;
import com.ibm.mqlight.api.impl.timer.TimerServiceImpl;
import com.ibm.mqlight.api.logging.FFDCProbeId;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;
import com.ibm.mqlight.api.network.NetworkService;
import com.ibm.mqlight.api.timer.TimerService;

public class NonBlockingClientImpl extends NonBlockingClient implements FSMActions, Component, CallbackService {

    static {
        LogbackLogging.setup();
    }
    private static final Logger logger = LoggerFactory.getLogger(NonBlockingClientImpl.class);

    private final EndpointService endpointService;
    private final CallbackService callbackService;
    private final ComponentImpl engine;
    private final TimerService timer;
    private final GsonBuilder gsonBuilder;
    private final Gson gson;

    private final StateMachine<NonBlockingClientState, NonBlockingClientTrigger> stateMachine;

    static final Class<?>[] validPropertyValueTypes = new Class[] {
        Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, byte[].class, Byte[].class, String.class
    };

    private final LinkedList<InternalStart<?>> pendingStarts = new LinkedList<>();
    private final LinkedList<InternalStop<?>> pendingStops = new LinkedList<>();
    private final String clientId;
    private TimerPromiseImpl timerPromise = null;
    private final LinkedList<QueueableWork> pendingWork = new LinkedList<>();

    private volatile String serviceUri = null;

    private Endpoint currentEndpoint = null;
    private EngineConnection currentConnection = null;
    private final HashMap<SendRequest, InternalSend<?>> outstandingSends = new HashMap<>();

    private final NonBlockingClientListenerWrapper<?> clientListener;

    private boolean remakingInboundLinks = false;

    private int undrainedSends = 0;
    private boolean pendingDrain = false;

    private boolean stoppedByUser = false;
    private ClientException lastException = null;

    long retryDelay = 0;

    private final Set<DeliveryRequest> pendingDeliveries = Collections.synchronizedSet(new HashSet<DeliveryRequest>());

    // topic pattern -> information about subscribed destination
    private final HashMap<SubscriptionTopic, SubData> subscribedDestinations = new HashMap<>();

    static class SubData {
        private enum State {
            BROKEN,         // A link attach has previously been attempted - but the client's connection to the server is currently broken
            ATTACHING,      // A link attach request has been sent - we're waiting to hear back.
            ESTABLISHED,    // We've received a link attach response - subscription is active.
            DETATCHING     // A link detatch request has been sent - we're waiting to hear back.
        }
        State state = State.ATTACHING;
        private final LinkedList<QueueableWork> pending = new LinkedList<>();
        final DestinationListenerWrapper<?> listener;
        private final QOS qos;
        private final int credit;
        private final boolean autoConfirm;
        private final int ttl;

        InternalSubscribe<?> inProgressSubscribe;
        InternalUnsubscribe<?> inProgressUnsubscribe;

        public SubData(DestinationListenerWrapper<?> listener, QOS qos, int credit, boolean autoConfirm, int ttl) {
            this.listener = listener;
            this.qos = qos;
            this.credit = credit;
            this.autoConfirm = autoConfirm;
            this.ttl = ttl;
        }
    }

    protected String generateClientId() {
        SecureRandom sr = new SecureRandom();
        String i = Integer.toHexString(sr.nextInt());
        while(i.length() < 8) i = "0" + i;
        return "AUTO_" + i.substring(0, 7);
    }

    protected <T> NonBlockingClientImpl(EndpointService endpointService,
            CallbackService callbackService,
            ComponentImpl engine,
            TimerService timerService,
            GsonBuilder gsonBuilder,
            ClientOptions options,
            NonBlockingClientListener<T>listener,
            T context) {
        final String methodName = "<init>";
        logger.entry(this, methodName, callbackService, engine, timerService, gsonBuilder, options, listener, context);

        if (endpointService == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("EndpointService cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        if (callbackService == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("CallbackService cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        if (timerService == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("TimerService cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        if (context instanceof NonBlockingClientListener) {
          final IllegalArgumentException exception = new IllegalArgumentException(
                "context cannot be of type NonBlockingClientListener");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        this.endpointService = endpointService;
        this.callbackService = callbackService;
        this.engine = engine;
        this.timer = timerService;
        this.gsonBuilder = gsonBuilder == null ? new GsonBuilder() : gsonBuilder;
        this.gson = this.gsonBuilder.create();
        if (options == null) options = defaultClientOptions;
        clientId = options.getId() != null ? options.getId() : generateClientId();
        logger.setClientId(clientId);
        clientListener = new NonBlockingClientListenerWrapper<T>(this, listener, context);
        stateMachine = NonBlockingFSMFactory.newStateMachine(this);
        endpointService.lookup(new EndpointPromiseImpl(this));
        logger.exit(this, methodName);
    }

    public <T> NonBlockingClientImpl(EndpointService endpointService,
                                     CallbackService callbackService,
                                     NetworkService networkService,
                                     TimerService timerService,
                                     GsonBuilder gsonBuilder,
                                     ClientOptions options,
                                     NonBlockingClientListener<T>listener,
                                     T context) {
        this(endpointService, callbackService, new Engine(networkService, timerService), timerService, gsonBuilder, options, listener, context);
    }

    public <T> NonBlockingClientImpl(String service, ClientOptions options, NonBlockingClientListener<T> listener, T context) {
        this(service == null ? new BluemixEndpointService()
                : new SingleEndpointService(service,
                        options == null ? null : options.getUser(),
                                options == null ? null : options.getPassword(),
                                        options == null ? null : options.getCertificateFile(),
                                                options == null ? true : options.getVerifyName()),
                new ThreadPoolCallbackService(5), new NettyNetworkService(),
                new TimerServiceImpl(), null, options, listener, context);
    }

    @Override
    public String getId() {
        return clientId;
    }

    @Override
    public String getService() {
        return serviceUri;
    }

    private volatile ClientState externalState = ClientState.STARTING;

    @Override
    public ClientState getState() {
        return externalState;
    }

    @Override
    public <T> boolean send(String topic, String data, Map<String, Object> properties,
            SendOptions sendOptions, CompletionListener<T> listener, T context)
            throws StoppedException {
        final String methodName = "send";
        logger.entry(this, methodName, topic, data, properties, sendOptions, listener, context);

        if (data == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("data cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        org.apache.qpid.proton.message.Message protonMsg = Proton.message();
        protonMsg.setBody(new AmqpValue(data));

        final boolean result = send(topic, protonMsg, properties, sendOptions == null ? defaultSendOptions : sendOptions, listener, context);

        logger.exit(this, methodName, result);

        return result;
    }

    @Override
    public <T> boolean send(String topic, ByteBuffer data, Map<String, Object> properties,
            SendOptions sendOptions, CompletionListener<T> listener, T context)
            throws StoppedException {
        final String methodName = "send";
       logger.entry(this, methodName, topic, data, properties, sendOptions, listener, context);

        if (data == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("data cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        org.apache.qpid.proton.message.Message protonMsg = Proton.message();
        int pos = data.position();
        byte[] dataBytes = new byte[data.remaining()];
        data.get(dataBytes);
        data.position(pos);
        protonMsg.setBody(new AmqpValue(new Binary(dataBytes)));
        final boolean result = send(topic, protonMsg, properties, sendOptions == null ? defaultSendOptions : sendOptions, listener, context);

        logger.exit(this, methodName, result);

        return result;
    }

    @Override
    public <T> boolean send(String topic, Object json,
            Map<String, Object> properties, SendOptions sendOptions,
            CompletionListener<T> listener, T context) throws StoppedException {
        final String methodName = "send";
        logger.entry(this, methodName, topic, json, properties, sendOptions, listener, context);

        String jsonString;
        synchronized(gson) {
            jsonString = gson.toJson(json);
        }
        final boolean result = sendJson(topic, jsonString, properties, sendOptions, listener, context);

        logger.exit(this, methodName, result);

        return result;
    }

    @Override
    public <T> boolean send(String topic, Object json, Type type,
            Map<String, Object> properties, SendOptions sendOptions,
            CompletionListener<T> listener, T context) throws StoppedException {
        final String methodName = "send";
        logger.entry(this, methodName, topic, json, type, properties, sendOptions, listener, context);

        String jsonString;
        synchronized(gson) {
            jsonString = gson.toJson(json, type);
        }
        final boolean result = sendJson(topic, jsonString, properties, sendOptions, listener, context);

        logger.exit(this, methodName, result);

        return result;
    }

    @Override
    public <T> boolean sendJson(String topic, String json,
            Map<String, Object> properties, SendOptions sendOptions,
            CompletionListener<T> listener, T context)
    throws StoppedException {
        final String methodName = "sendJson";
        logger.entry(this, methodName, topic, json, properties, sendOptions, listener, context);

        org.apache.qpid.proton.message.Message protonMsg = Proton.message();
        protonMsg.setBody(new AmqpValue(json));
        protonMsg.setContentType("application/json");
        final boolean result = send(topic, protonMsg, properties, sendOptions == null ? defaultSendOptions : sendOptions, listener, context);

        logger.exit(this, methodName, result);

        return result;
    }

    private static final Map<String, String> immutable = new HashMap<String, String>() {
        // we need to do URI encoding, so we have a set of immutable characters that should not be encoded
        // these are '/' and the RFC 2396 unreserved characters ("-", "_", ".", "!", "~", "*", "'", "(" and ")")
        // and we also have the additional encoding of '+' to '%20' that URLEncoder doesn't seem to do
        private static final long serialVersionUID = -6961093296676437685L;
        {
            put("%2F", "/");
            put("%2D", "-");
            put("%5F", "_");
            put("%2E", ".");
            put("%21", "!");
            put("%7E", "~");
            put("%2A", "*");
            put("%27", "'");
            put("%28", "(");
            put("%29", ")");
        }
    };

    protected static boolean isValidPropertyValue(Object value) {
        final String methodName = "isValidPropertyValue";
        logger.entry(methodName, value);
        if (value == null) {
          logger.exit(methodName, true);
          return true;
        }
        for (int i = 0; i < validPropertyValueTypes.length; ++i) {
            if (validPropertyValueTypes[i].isAssignableFrom(value.getClass())) {
                logger.exit(methodName, true);
                return true;
            }
        }
        logger.exit(methodName, false);
        return false;
    }

    private <T> boolean send(String topic, org.apache.qpid.proton.message.Message protonMsg,
                                       Map<String, Object> properties,
                                       SendOptions sendOptions, CompletionListener<T> listener, T context) throws StoppedException {
        final String methodName = "send";
        logger.entry(this, methodName, topic, protonMsg, properties, sendOptions, listener, context);

        if (topic == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("topic cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }

        protonMsg.setAddress("amqp:///" + topic);
        protonMsg.setTtl(sendOptions.getTtl());
        Map<String, Object> amqpProperties = new HashMap<>();
        if ((properties != null) && !properties.isEmpty()) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (!isValidPropertyValue(entry.getValue())) {
                    final IllegalArgumentException exception = new IllegalArgumentException(
                            "Property key '"
                                    + entry.getKey()
                                    + "' specifies a value '"
                                    + ((entry.getValue() == null) ? "null"
                                            : entry.getValue().toString())
                                    + "' which is not of a supported type");

                  logger.throwing(this, methodName, exception);
                  throw exception;
                }
                if (entry.getValue() instanceof Byte[]) {
                    final Byte[] src = (Byte[]) entry.getValue();
                    byte[] copy = new byte[src.length];
                    for (int i = 0; i < src.length; i++) {
                        final Byte b = src[i];
                        copy[i] = (b == null) ? 0 : (b.byteValue());
                    }
                    amqpProperties.put(entry.getKey(), new Binary(copy));
                } else if (entry.getValue() instanceof byte[]) {
                    byte[] copy = new byte[((byte[])entry.getValue()).length];
                    System.arraycopy(entry.getValue(), 0, copy, 0, copy.length);
                    amqpProperties.put(entry.getKey(), new Binary(copy));
                } else {
                    amqpProperties.put(entry.getKey(), entry.getValue());
                }
            }
            protonMsg.setApplicationProperties(new ApplicationProperties(amqpProperties));
        }

        byte data[] = new byte[2 * 1024];
        int length;
        while (true) {
            try {
                length = protonMsg.encode(data,  0,  data.length);
                break;
            } catch(BufferOverflowException boe) {
                data = new byte[data.length * 2];
            }
        }

        final ByteBuf buf = io.netty.buffer.Unpooled.wrappedBuffer(data);
        InternalSend<T> is = new InternalSend<T>(this, topic, sendOptions.getQos(), buf, length);
        ++undrainedSends;
        tell(is, this);

        try {
          is.future.setListener(callbackService, listener, context);
        } catch (StoppedException e) {
          logger.throwing(this, methodName, e);
          throw e;
        } catch (StateException e) {
          IllegalStateException exception = new IllegalStateException("Unexpected state exception", e);
          logger.ffdc(methodName, FFDCProbeId.PROBE_001, exception, this);
          logger.throwing(this, methodName, e);
          throw exception;
        }

        boolean result = undrainedSends < 2;
        pendingDrain |= !result;

        logger.exit(this, methodName, result);

        return result;
    }

    @Override
    public <T> NonBlockingClient start(CompletionListener<T> listener, T context) throws StoppedException {
        final String methodName = "start";
        logger.entry(this, methodName, listener, context);

        InternalStart<T> is = new InternalStart<T>(this);
        try {
          is.future.setListener(callbackService, listener, context);
        } catch (StoppedException e) {
          logger.throwing(this, methodName, e);
          throw e;
        } catch (StateException e) {
          IllegalStateException exception = new IllegalStateException("Unexpected state exception", e);
          logger.ffdc(methodName, FFDCProbeId.PROBE_002, exception, this);
          logger.throwing(this, methodName, e);
          throw exception;
        }

        tell(is, this);

        logger.entry(this, methodName, this);

        return this;
    }

    @Override
    public <T> void stop(CompletionListener<T> listener, T context) throws StartingException {
        final String methodName = "stop";
        logger.entry(this, methodName, listener, context);

        InternalStop<T> is = new InternalStop<T>(this);
        try {
          is.future.setListener(callbackService, listener, context);
        } catch (StartingException e) {
          logger.throwing(this, methodName, e);
          throw e;
        } catch (StateException e) {
          IllegalStateException exception = new IllegalStateException("Unexpected state exception", e);
          logger.ffdc(methodName, FFDCProbeId.PROBE_003, exception, this);
          logger.throwing(this, methodName, e);
          throw exception;
        }

        tell(is, this);

        logger.exit(this, methodName);
    }

    @Override
    public <T> NonBlockingClient subscribe(String topicPattern,
            SubscribeOptions subOptions, DestinationListener<T> destListener,
            CompletionListener<T> compListener, T context)
            throws SubscribedException, StoppedException, IllegalArgumentException {
        final String methodName = "subscribe";
        logger.entry(this, methodName, topicPattern, subOptions, destListener, compListener, context);

        if (topicPattern == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("Topic pattern cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        if (destListener == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("DestinationListener cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        if (subOptions == null) subOptions = defaultSubscribeOptions;
        final SubscriptionTopic subTopic = new SubscriptionTopic(topicPattern, subOptions.getShareName());
        boolean autoConfirm = subOptions.getAutoConfirm() || subOptions.getQOS() == QOS.AT_MOST_ONCE;
        InternalSubscribe<T> is =
                new InternalSubscribe<T>(this, subTopic, subOptions.getQOS(), subOptions.getCredit(), autoConfirm, (int) Math.round(subOptions.getTtl() / 1000.0), gsonBuilder, destListener, context);
        tell(is, this);

        try {
          is.future.setListener(callbackService, compListener, context);
        } catch (SubscribedException|StoppedException e) {
          logger.throwing(this, methodName, e);
          throw e;
        } catch (StateException e) {
          IllegalStateException exception = new IllegalStateException("Unexpected state exception", e);
          logger.ffdc(methodName, FFDCProbeId.PROBE_004, exception, this);
          logger.throwing(this, methodName, e);
          throw exception;
        }

        logger.exit(this, methodName, this);

        return this;
    }

    @Override
    public <T> NonBlockingClient unsubscribe(String topicPattern, String share, int ttl, CompletionListener<T> listener, T context)
    throws UnsubscribedException, StoppedException, IllegalArgumentException {
        final String methodName = "unsubscribe";
        logger.entry(this, methodName, topicPattern, share, ttl, listener, context);

        if (topicPattern == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("Topic pattern cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        if ((share != null) && share.contains(":")) {
          final IllegalArgumentException exception = new IllegalArgumentException("Share name cannot contain a colon (:) character");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        if (ttl != 0) {
          final IllegalArgumentException exception = new IllegalArgumentException("TTL cannot be non-zero");
          logger.throwing(this, methodName, exception);
          throw exception;
        }

        InternalUnsubscribe<T> us = new InternalUnsubscribe<T>(this, topicPattern, share, ttl == 0);
        tell(us, this);

        try {
          us.future.setListener(callbackService, listener, context);
        } catch (UnsubscribedException|StoppedException e) {
          logger.throwing(this, methodName, e);
          throw e;
        } catch (StateException e) {
          IllegalStateException exception = new IllegalStateException("Unexpected state exception", e);
          logger.ffdc(methodName, FFDCProbeId.PROBE_005, exception, this);
          logger.throwing(this, methodName, e);
          throw exception;
        }

        logger.exit(this, methodName, this);

        return this;
    }

    @Override
    public <T> NonBlockingClient unsubscribe(String topicPattern, String share, CompletionListener<T> listener, T context)
    throws UnsubscribedException, StoppedException {
        final String methodName = "unsubscribe";
        logger.entry(this, methodName, topicPattern, share, listener, context);

        if (topicPattern == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("Topic pattern cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        if ((share != null) && share.contains(":")) {
          final IllegalArgumentException exception = new IllegalArgumentException("Share name cannot contain a colon (:) character");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        InternalUnsubscribe<T> us = new InternalUnsubscribe<T>(this, topicPattern, share, false);
        tell(us, this);

        try {
          us.future.setListener(callbackService, listener, context);
        } catch (UnsubscribedException|StoppedException e) {
          logger.throwing(this, methodName, e);
          throw e;
        } catch (StateException e) {
          IllegalStateException exception = new IllegalStateException("Unexpected state exception", e);
          logger.ffdc(methodName, FFDCProbeId.PROBE_006, exception, this);
          logger.throwing(this, methodName, e);
          throw exception;
        }

        logger.exit(this, methodName, this);

        return this;
    }

    protected void onReceive(Message message) {
        final String methodName = "onReceive";
        logger.entry(this, methodName, message);

        if (message instanceof EndpointResponse) {
            EndpointResponse er = (EndpointResponse)message;
            if (er.exception != null) {
                if (lastException == null) lastException = er.exception;
                stateMachine.fire(NonBlockingClientTrigger.EP_RESP_FATAL);
            } else {
                currentEndpoint = er.endpoint;
                stateMachine.fire(NonBlockingClientTrigger.EP_RESP_OK);
            }
        } else if (message instanceof ExhaustedResponse) {
            retryDelay = ((ExhaustedResponse)message).delay;
            stateMachine.fire(NonBlockingClientTrigger.EP_RESP_EXHAUSTED);
        } else if (message instanceof OpenResponse) {
            OpenResponse or = (OpenResponse)message;
            if (or.exception != null) {
                if (lastException == null) lastException = or.exception;
                if (or.exception instanceof com.ibm.mqlight.api.SecurityException) {
                    stateMachine.fire(NonBlockingClientTrigger.OPEN_RESP_FATAL);
                } else {
                    stateMachine.fire(NonBlockingClientTrigger.OPEN_RESP_RETRY);
                }
            } else {
                currentConnection = or.connection;
                stateMachine.fire(NonBlockingClientTrigger.OPEN_RESP_OK);
            }
        } else if (message instanceof InternalSend) {
            InternalSend<?> is = (InternalSend<?>)message;
            NonBlockingClientState state = stateMachine.getState();
            if (NonBlockingClientState.acceptingWorkStates.contains(state)) {
                SendRequest sr = new SendRequest(currentConnection, is.topic, is.buf, is.length, is.qos);
                outstandingSends.put(sr, is);
                engine.tell(sr, this);
            } else if (NonBlockingClientState.queueingWorkStates.contains(state)) {
                pendingWork.addLast(is);
            } else {  // Assume state is in NonBlockingClientState.sendFail
                is.future.setFailure(new StoppedException("Cannot send messages because the client is in stopped state"));
            }

        } else if (message instanceof SendResponse) {
            SendResponse sr = (SendResponse)message;
            InternalSend<?> is = outstandingSends.remove(sr.request);
            if (is != null) {
                if (sr.cause == null) {
                    is.future.setSuccess(null);
                } else {
                    is.future.setFailure(sr.cause);
                }
            }
        } else if (message instanceof InternalStart) {
            pendingStarts.addLast((InternalStart<?>)message);
            stateMachine.fire(NonBlockingClientTrigger.START);
        } else if (message instanceof InternalStop) {
            pendingStops.addLast((InternalStop<?>)message);
            stateMachine.fire(NonBlockingClientTrigger.STOP);
        } else if (message instanceof CloseResponse) {
            currentConnection = null;
            stateMachine.fire(NonBlockingClientTrigger.CLOSE_RESP);
        } else if (message instanceof PopResponse) {
            timerPromise = null;
            stateMachine.fire(NonBlockingClientTrigger.TIMER_RESP_POP);
        } else if (message instanceof CancelResponse) {
            timerPromise = null;
            stateMachine.fire(NonBlockingClientTrigger.TIMER_RESP_CANCEL);
        } else if (message instanceof InternalSubscribe) {
            InternalSubscribe<?> is = (InternalSubscribe<?>)message;
            NonBlockingClientState state = stateMachine.getState();
            if (NonBlockingClientState.acceptingWorkStates.contains(state)) {
                SubData sd = subscribedDestinations.get(is.topic);
                if (sd == null) {
                    // Not already subscribed - so subscribe...
                    SubscribeRequest sr = new SubscribeRequest(currentConnection, is.topic, is.qos, is.credit, is.ttl);
                    sd = new SubData(is.destListener, is.qos, is.credit, is.autoConfirm, is.ttl);
                    sd.inProgressSubscribe = is;
                    sd.state = SubData.State.ATTACHING;
                    subscribedDestinations.put(is.topic, sd);
                    engine.tell(sr, this);
                } else if (sd.pending.isEmpty()) {
                    // Already subscribed - no pending actions on the subscription.
                    if (sd.state == SubData.State.ATTACHING || sd.state == SubData.State.ESTABLISHED) {
                        // Operation fails because it is attempting to subscribed to an already subscribed destination
                        String[] topicElements = is.topic.split();
                        String errMsg = "Cannot subscribe because the client is already subscribed to topic '" + topicElements[0] + "'";
                        if (topicElements[1] != null) {
                            errMsg = errMsg + " and share '" + topicElements[1] + "'.";
                        }
                        is.future.setFailure(new SubscribedException(errMsg));
                    } else {
                        // Add to pending actions - so operation is attempted when current link is detatched.
                        sd.pending.addLast(is);
                    }
                } else {
                    // Already subscribed to the destination - but there are pending actions relating to
                    // the subscription.  So queue this at the end, so it is processed in order with the
                    // other pending actions.
                    sd.pending.addLast(is);
                }
            } else if (NonBlockingClientState.queueingWorkStates.contains(state)) {
                pendingWork.add(is);
            } else { // Assume state is in NonBlockingClientState.rejectingWorkStates
                is.future.setFailure(new StoppedException("Cannot subscribe because the client is in stopped state"));
            }

        } else if (message instanceof SubscribeResponse) {
            SubscribeResponse sr = (SubscribeResponse)message;
            SubData sd = subscribedDestinations.get(sr.topic);
            if (sr.error != null) logger.ffdc(methodName, FFDCProbeId.PROBE_007, sr.error , sr, this);
            if (sd != null) {
                if (sd.inProgressSubscribe != null) {
                    sd.inProgressSubscribe.future.setSuccess(null);
                    sd.inProgressSubscribe = null;
                }
                sd.state = SubData.State.ESTABLISHED;
                // Replay any pending operations on the subscription
                while(!sd.pending.isEmpty()) {
                    Message m = (Message) sd.pending.removeFirst();
                    tell(m, m.getSender());
                }

                // If the client is in the process of re-making its in-bound links - see if this process is now complete...
                if (remakingInboundLinks) {
                    boolean allRemade = true;
                    for (SubData data : subscribedDestinations.values()) {
                        if (data.state != SubData.State.ESTABLISHED) {
                            allRemade = false;
                            break;
                        }
                    }
                    if (allRemade) {
                        remakingInboundLinks = false;
                        stateMachine.fire(NonBlockingClientTrigger.SUBS_REMADE);
                    }
                }
            }
        } else if (message instanceof InternalUnsubscribe) {
            InternalUnsubscribe<?> iu = (InternalUnsubscribe<?>)message;
            final SubscriptionTopic amqpTopic = new SubscriptionTopic(iu.topicPattern, iu.share);
            SubData sd = subscribedDestinations.get(amqpTopic);
            NonBlockingClientState state = stateMachine.getState();

            if (NonBlockingClientState.acceptingWorkStates.contains(state)) {
                if (sd == null) {
                    String errMsg = "Client is not subscribed to topic '" + iu.topicPattern + "'";
                    if (iu.share != null) {
                        errMsg += " and share '" + iu.share + "'";
                    }
                    UnsubscribedException se = new UnsubscribedException(errMsg);
                    iu.future.setFailure(se);
                } else if (sd.pending.isEmpty()) {
                    if (sd.state == SubData.State.ATTACHING) {
                        pendingWork.addLast(iu);
                    } else if (sd.state == SubData.State.DETATCHING) {
                      UnsubscribedException se = new UnsubscribedException("Client is not subscribed to " +
                                ((iu.share == null || "".equals(iu.share)) ? "private" : "shared") +
                                "destination " + iu.topicPattern);
                        iu.future.setFailure(se);
                    } else if (sd.state == SubData.State.ESTABLISHED) {
                        sd.state = SubData.State.DETATCHING;
                        sd.inProgressUnsubscribe = iu;
                        engine.tell(new UnsubscribeRequest(currentConnection, amqpTopic, iu.zeroTtl), this);
                    }
                } else {
                    // Subscription already has pending operations - so to preserve ordering
                    // queue this unsubscribe operation to the end of the list of pending operations.
                    sd.pending.addLast(iu);
                }
            } else if (NonBlockingClientState.queueingWorkStates.contains(state)) {
                pendingWork.addLast(iu);
            } else { // NonBlockingClientState.rejectingWorkStates.contains(state)
                iu.future.setFailure(new StoppedException("Cannot unsubscribe because the client is in stopped state"));
            }
        } else if (message instanceof UnsubscribeResponse) {
            // This needs to be tolerant of receiving an unsubscribe response before we've issued an
            // unsubscribe request (in the case that the server closes the link)
            UnsubscribeResponse ur = (UnsubscribeResponse)message;
            SubData sd = subscribedDestinations.remove(ur.topic);
            String[] parts = ur.topic.split();
            sd.listener.onUnsubscribed(callbackService, parts[0], parts[1], ur.error);
            if (sd.inProgressUnsubscribe != null) {
                sd.inProgressUnsubscribe.future.setSuccess(null);
                sd.inProgressUnsubscribe = null;
            }
            while(!sd.pending.isEmpty()) {
                Message m = (Message) sd.pending.removeFirst();
                tell(m, m.getSender());  // Put this back into the queue of events
            }

            // If the client is in the process of re-making its in-bound links - see if this process is now complete...
            if (remakingInboundLinks) {
                boolean allRemade = true;
                for (SubData data : subscribedDestinations.values()) {
                    if (data.state != SubData.State.ESTABLISHED) {
                        allRemade = false;
                        break;
                    }
                }
                if (allRemade) {
                    remakingInboundLinks = false;
                    stateMachine.fire(NonBlockingClientTrigger.SUBS_REMADE);
                }
            }
        } else if (message instanceof DeliveryRequest) {
            DeliveryRequest dr = (DeliveryRequest)message;
            final SubData subData = subscribedDestinations.get(new SubscriptionTopic(dr.topicPattern));
            if (dr.qos == QOS.AT_LEAST_ONCE) {
                pendingDeliveries.add(dr);
            }
            subData.listener.onDelivery(callbackService, dr, subData.qos, subData.autoConfirm);
        } else if (message instanceof DisconnectNotification) {
            remakingInboundLinks = false;
            DisconnectNotification dn = (DisconnectNotification)message;

            final Throwable error = dn.error;
            if (error instanceof ReplacedException) {
                if (lastException == null) lastException = (ReplacedException) error;
                stateMachine.fire(NonBlockingClientTrigger.REPLACED);
            } else if (error instanceof com.ibm.mqlight.api.SecurityException) {
                if (lastException == null) lastException = (com.ibm.mqlight.api.SecurityException) error;
                stateMachine.fire(NonBlockingClientTrigger.OPEN_RESP_FATAL);
            } else if (error instanceof ClientException) {
                if (lastException == null) lastException = (ClientException) error;
                stateMachine.fire(NonBlockingClientTrigger.NETWORK_ERROR);
            } else if (error != null) {
                if (lastException == null) lastException = new NetworkException(error.getMessage(), error.getCause());
                stateMachine.fire(NonBlockingClientTrigger.NETWORK_ERROR);
            }
        } else if (message instanceof FlushResponse) {
            stateMachine.fire(NonBlockingClientTrigger.INBOUND_WORK_COMPLETE);
        } else if (message instanceof DrainNotification) {
            undrainedSends = 0;
            if (pendingDrain) {
                pendingDrain = false;
                clientListener.onDrain(callbackService);
            }
        } else if (message instanceof CallbackExceptionNotification) {
            Exception exception = ((CallbackExceptionNotification)message).exception;
            logger.data(this, methodName, "Exception thrown from inside callback", exception);
            logger.error("Exception thrown from inside callback", exception);
            stateMachine.fire(NonBlockingClientTrigger.STOP);
            if (lastException == null) {
                if (exception instanceof ClientException) {
                    lastException = (ClientException)exception;
                } else {
                    lastException = new ClientException("Exception thrown from inside callback", exception);
                }
            }
        } else {
            logger.data("Unexpected message received {} from {} ", message, message.getSender());
        }

        logger.exit(this, methodName);
    }

    @Override
    public void startTimer() {
        final String methodName = "startTimer";
        logger.entry(this, methodName);

        if (timerPromise != null) logger.ffdc(methodName, FFDCProbeId.PROBE_008, new Exception("timer already active"), this);

        timerPromise = new TimerPromiseImpl(this, null);
        timer.schedule(retryDelay, timerPromise);

        logger.exit(this, methodName);
    }

    @Override
    public void openConnection() {
        final String methodName = "openConnection";
        logger.entry(this, methodName);

        engine.tell(new OpenRequest(currentEndpoint, clientId), this);

        logger.exit(this, methodName);
    }

    @Override
    public void closeConnection() {
        final String methodName = "closeConnection";
        logger.entry(this, methodName);

        pendingDeliveries.clear();
        engine.tell(new CloseRequest(currentConnection), this);

        logger.exit(this, methodName);
    }

    @Override
    public void cancelTimer() {
        final String methodName = "cancelTimer";
        logger.entry(this, methodName);

        if (timerPromise != null) {
            TimerPromiseImpl tmp = timerPromise;
            timerPromise = null;
            timer.cancel(tmp);
        }

        logger.exit(this, methodName);
    }

    @Override
    public void requestEndpoint() {
        final String methodName = "requestEndpoint";
        logger.entry(this, methodName);

        endpointService.lookup(new EndpointPromiseImpl(this));

        logger.exit(this, methodName);
    }

    @Override
    public void remakeInboundLinks() {
        final String methodName = "remakeInboundLinks";
        logger.entry(this, methodName);

        if (subscribedDestinations.isEmpty()) {
            stateMachine.fire(NonBlockingClientTrigger.SUBS_REMADE);
        } else {
            remakingInboundLinks = true;
            for (Map.Entry<SubscriptionTopic, SubData>entry : subscribedDestinations.entrySet()) {
                SubData data = entry.getValue();
                data.state = SubData.State.ATTACHING;
                SubscribeRequest sr = new SubscribeRequest(currentConnection, entry.getKey(), data.qos, data.credit, data.ttl);
                engine.tell(sr, this);
            }
        }

        logger.exit(this, methodName);
    }


    @Override
    public void blessEndpoint() {
        final String methodName = "blessEndpoint";
        logger.entry(this, methodName);

        serviceUri = (currentEndpoint.useSsl() ? "amqps://" : "amqp://") +
                     currentEndpoint.getHost() + ":" + currentEndpoint.getPort();
        retryDelay = 0;
        endpointService.onSuccess(currentEndpoint);

        logger.exit(this, methodName);
    }

    @Override
    public void cleanup() {
        final String methodName = "cleanup";
        logger.entry(this, methodName);

        pendingDeliveries.clear();

        // Fire a drain notification if required.
        undrainedSends = 0;
        if (pendingDrain) {
            pendingDrain = false;
            clientListener.onDrain(callbackService);
        }

        // Flush any pending subscribe operations into pending work queue
        for (Map.Entry<SubscriptionTopic, SubData> entry : subscribedDestinations.entrySet()) {
            SubData subData = entry.getValue();
            if (subData.inProgressSubscribe != null) {
                subData.inProgressSubscribe.future.setFailure(new StoppedException("Cannot subscribe because the client is in stopped state"));
                subData.inProgressSubscribe = null;
            }
            if (subData.state == SubData.State.ESTABLISHED) {
                String parts[] = entry.getKey().split();
                subData.listener.onUnsubscribed(callbackService, parts[0], parts[1], null);
            }
            if (subData.inProgressUnsubscribe != null) {
                subData.inProgressUnsubscribe.future.setFailure(new StoppedException("Cannot unsubscribe because the client is in stopped state"));
                subData.inProgressUnsubscribe = null;
            }
            while (!subData.pending.isEmpty()) {
                pendingWork.addLast(subData.pending.removeFirst());
            }
        }
        subscribedDestinations.clear();

        // For any inflight sends - fail AT_LEAST_ONCE, succeed AT_MOST_ONCE
        for (InternalSend<?> send : outstandingSends.values()) {
            if (send.qos == QOS.AT_MOST_ONCE) {
                send.future.setSuccess(null);
            } else {
                send.future.setFailure(new StoppedException("Cannot send messages because the client is in stopped state"));
            }
        }

        // Fail any pending work
        for (QueueableWork work : pendingWork) {
            if (work instanceof InternalSend<?>) {
                InternalSend<?> is = (InternalSend<?>)work;
                StoppedException stoppedException = new StoppedException("Cannot send messages because the client is in stopped state");
                is.future.setFailure(stoppedException);
            } else if (work instanceof InternalSubscribe<?>) {
                InternalSubscribe<?> is = (InternalSubscribe<?>)work;
                StoppedException stoppedException = new StoppedException("Cannot subscribe because the client is in stopped state");
                is.future.setFailure(stoppedException);
            } else {  // work instanceof InternalUnsubscribe
                InternalUnsubscribe<?> iu  = (InternalUnsubscribe<?>)work;
                StoppedException stoppedException = new StoppedException("Cannot unsubscribe because the client is in stopped state");
                iu.future.setFailure(stoppedException);
            }
        }

        timerPromise = null;
        currentConnection = null;
        remakingInboundLinks = false;
        serviceUri = null;

        // Ask the callback service to notify us when it has completed any previously
        // requested callback invocations (via a FlushResponse message to the onReceive() method)
        callbackService.run(new Runnable() {
            @Override
            public void run() {}
        }, this, new CallbackPromiseImpl(this, false));

        logger.exit(this, methodName);
    }

    @Override
    public void failPendingStops() {
        final String methodName = "failPendingStops";
        logger.entry(this, methodName);

        while(!pendingStops.isEmpty()) {
            InternalStop<?> stop = pendingStops.removeFirst();
            stop.future.setFailure(new StartingException("Cannot stop client because of a subsequent start request"));
        }

        logger.exit(this, methodName);
    }

    @Override
    public void succeedPendingStops() {
        final String methodName = "succeedPendingStops";
        logger.entry(this, methodName);

        while(!pendingStops.isEmpty()) {
            InternalStop<?> stop = pendingStops.removeFirst();
            stop.future.setSuccess(null);
        }

        logger.exit(this, methodName);
    }

    @Override
    public void failPendingStarts() {
        final String methodName = "failPendingStarts";
        logger.entry(this, methodName);

        while(!pendingStarts.isEmpty()) {
            InternalStart<?> start = pendingStarts.removeFirst();
            start.future.setFailure(new StoppedException("Cannot start client because of a subsequent stop request"));
        }

        logger.exit(this, methodName);
    }

    @Override
    public void succeedPendingStarts() {
        final String methodName = "succeedPendingStarts";
        logger.entry(this, methodName);

        while(!pendingStarts.isEmpty()) {
            InternalStart<?> start = pendingStarts.removeFirst();
            start.future.setSuccess(null);
        }

        logger.exit(this, methodName);
    }

    @Override
    public void processQueuedActions() {
        final String methodName = "processQueuedActions";
        logger.entry(this, methodName);

        while (!pendingWork.isEmpty()) {
            tell((Message)pendingWork.removeFirst(), this);
        }

        logger.exit(this, methodName);
    }

    @Override
    public void eventStarting() {
        final String methodName = "eventStarting";
        logger.entry(this, methodName);

        stoppedByUser = false;
        lastException = null;
        externalState = ClientState.STARTING;

        logger.exit(this, methodName);
    }

    @Override
    public void eventUserStopping() {
        final String methodName = "eventUserStopping";
        logger.entry(this, methodName);

        externalState = ClientState.STOPPING;

        logger.exit(this, methodName);
    }

    @Override
    public void eventSystemStopping() {
        final String methodName = "eventSystemStopping";
        logger.entry(this, methodName);

        // Need to be careful because sometimes the client can be stopped by the user and then
        // a system problem be detected (in which case we get a user stopping followed by a
        // system stopping event - and should discard any error associated with the
        // system stopping event)...
        externalState = ClientState.STOPPING;
        if (lastException == null) stoppedByUser = true;

        logger.exit(this, methodName);
    }

    @Override
    public void eventStopped() {
        final String methodName = "eventStopped";
        logger.entry(this, methodName);

        externalState = ClientState.STOPPED;
        clientListener.onStopped(callbackService, stoppedByUser ? null : lastException);
        stoppedByUser = false;
        lastException = null;

        logger.exit(this, methodName);
    }

    @Override
    public void eventStarted() {
        final String methodName = "eventStarted";
        logger.entry(this, methodName);

        externalState = ClientState.STARTED;
        clientListener.onStarted(callbackService);

        logger.exit(this, methodName);
    }

    @Override
    public void eventRetrying() {
        final String methodName = "eventRetrying";
        logger.entry(this, methodName);

        externalState = ClientState.RETRYING;
        clientListener.onRetrying(callbackService, stoppedByUser ? null : lastException);
        lastException = null;

        logger.exit(this, methodName);
    }

    @Override
    public void eventRestarted() {
        final String methodName = "eventRestarted";
        logger.entry(this, methodName);

        externalState = ClientState.STARTED;
        clientListener.onRestarted(callbackService);

        logger.exit(this, methodName);
    }

    @Override
    public void breakInboundLinks() {
        final String methodName = "breakInboundLinks";
        logger.entry(this, methodName);

        pendingDeliveries.clear();

        undrainedSends = 0;
        if (pendingDrain) {
            pendingDrain = false;
            clientListener.onDrain(callbackService);
        }
        for (InternalSend<?> sendRequest : outstandingSends.values()) {
            if (sendRequest.qos == QOS.AT_MOST_ONCE) {
                // We don't know if the message made it or not - but based on this QOS - we have to assume it did...
                sendRequest.future.setSuccess(null);
            } else {
                // And for this QOS - we can be pessimistic and assume it didn't...
                pendingWork.addLast(sendRequest);
            }
        }
        outstandingSends.clear();

        for (Map.Entry<SubscriptionTopic, SubData>entry : subscribedDestinations.entrySet()) {
            SubData subData = entry.getValue();
            while(!subData.pending.isEmpty()) {
                pendingWork.addLast(subData.pending.getFirst());
            }
            subData.state = SubData.State.BROKEN;
        }

        logger.exit(this, methodName);
    }

    /**
     * Pass a {@link DeliveryResponse} back to the engine which will settle the delivery
     * (in the AT_LEAST_ONCE case) and flow deliveryCount++ and link-credit to the remote end
     *
     * @param request
     *            the {@link DeliveryRequest} to process.
     * @return true == it might have worked, false == it really didn't work!
     */
    protected boolean doDelivery(DeliveryRequest request) {
        final String methodName = "doDelivery";
        logger.entry(this, methodName, request);

        final boolean result = (request.qos == QOS.AT_MOST_ONCE || pendingDeliveries.remove(request));
        if (result) {
            engine.tell(new DeliveryResponse(request), this);
        }

        logger.exit(this, methodName, result);
        return result;
    }

    private final ComponentImpl component = new ComponentImpl() {
      @Override
      protected void onReceive(Message message) {
        NonBlockingClientImpl.this.onReceive(message);
      }
    };

    @Override
    public void tell(Message message, Component self) {
      component.tell(message, self);
    }

    @Override
    public void run(Runnable runnable, Object orderingCtx, Promise<Void> promise) {
      callbackService.run(runnable, orderingCtx, promise);
    }
}
