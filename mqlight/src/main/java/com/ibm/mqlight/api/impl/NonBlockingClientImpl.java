/*
 *   <copyright
 *   notice="oco-source"
 *   pids="5725-P60"
 *   years="2015"
 *   crc="1438874957" >
 *   IBM Confidential
 *
 *   OCO Source Materials
 *
 *   5724-H72
 *
 *   (C) Copyright IBM Corp. 2015
 *
 *   The source code for the program is not published
 *   or otherwise divested of its trade secrets,
 *   irrespective of what has been deposited with the
 *   U.S. Copyright Office.
 *   </copyright>
 */

package com.ibm.mqlight.api.impl;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.oxo42.stateless4j.StateMachine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.ClientOptions;
import com.ibm.mqlight.api.ClientState;
import com.ibm.mqlight.api.CompletionListener;
import com.ibm.mqlight.api.DestinationListener;
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.NonBlockingClientListener;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.ReplacedException;
import com.ibm.mqlight.api.SendOptions;
import com.ibm.mqlight.api.StateException;
import com.ibm.mqlight.api.SubscribeOptions;
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
import com.ibm.mqlight.api.network.NetworkService;
import com.ibm.mqlight.api.timer.TimerService;

public class NonBlockingClientImpl extends NonBlockingClient implements FSMActions {

    static {
        LogbackLogging.setup();
    }
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EndpointService endpointService;
    private final CallbackService callbackService;
    private final Component engine;
    private final TimerService timer;
    private final GsonBuilder gsonBuilder;
    private final Gson gson;

    private final StateMachine<NonBlockingClientState, NonBlockingClientTrigger> stateMachine;

    static final Class<?>[] validPropertyValueTypes = new Class[] {
        Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, byte[].class, String.class
    };

    private final LinkedList<InternalStart<?>> pendingStarts = new LinkedList<>();
    private final LinkedList<InternalStop<?>> pendingStops = new LinkedList<>();
    private final String clientId;
    private TimerPromiseImpl timerPromise = null;
    private final LinkedList<QueueableWork> pendingWork = new LinkedList<>();

    private volatile String serviceUri = null;

    private Endpoint currentEndpoint = null;
    private EngineConnection currentConnection = null;
    private HashMap<SendRequest, InternalSend<?>> outstandingSends = new HashMap<>();

    private final NonBlockingClientListenerWrapper<?> clientListener;

    private boolean remakingInboundLinks = false;

    private int undrainedSends = 0;
    private boolean pendingDrain = false;

    private boolean stoppedByUser = false;
    private ClientException lastException = null;

    long retryDelay = 0;

    private Set<DeliveryRequest> pendingDeliveries = Collections.synchronizedSet(new HashSet<DeliveryRequest>());

    // topic pattern -> information about subscribed destination
    private final HashMap<String, SubData> subscribedDestinations = new HashMap<>();

    static class SubData {
        private enum State {
            BROKEN,         // A link attach has previously been attempted - but the client's connection to the server is currently broken
            ATTACHING,      // A link attach request has been sent - we're waiting to hear back.
            ESTABLISHED,    // We've received a link attach response - subscription is active.
            DETATCHING     // A link detatch request has been sent - we're waiting to hear back.
        }
        State state = State.ATTACHING;
        private LinkedList<QueueableWork> pending = new LinkedList<>();
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
            Component engine,
            TimerService timerService,
            GsonBuilder gsonBuilder,
            ClientOptions options,
            NonBlockingClientListener<T>listener,
            T context) {
        if (endpointService == null) throw new IllegalArgumentException("EndpointService cannot be null");
        if (callbackService == null) throw new IllegalArgumentException("CallbackService cannot be null");
        if (timerService == null) throw new IllegalArgumentException("TimerService cannot be null");
        this.endpointService = endpointService;
        this.callbackService = callbackService;
        this.engine = engine;
        this.timer = timerService;
        this.gsonBuilder = gsonBuilder == null ? new GsonBuilder() : gsonBuilder;
        this.gson = this.gsonBuilder.create();
        if (options == null) options = defaultClientOptions;
        clientId = options.getId() != null ? options.getId() : generateClientId();
        clientListener = new NonBlockingClientListenerWrapper<T>(this, listener, context);
        stateMachine = NonBlockingFSMFactory.newStateMachine(this);
        endpointService.lookup(new EndpointPromiseImpl(this));
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
        this(service == null ? new BluemixEndpointService() : new SingleEndpointService(service,  options == null ? null : options.getUser(),  options == null ? null : options.getPassword()),
             new ThreadPoolCallbackService(5),
             new NettyNetworkService(),
             new TimerServiceImpl(),
             null,
             options,
             listener,
             context);
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
            throws StateException {
        if (data == null) throw new IllegalArgumentException("data cannot be null");
        org.apache.qpid.proton.message.Message protonMsg = Proton.message();
        protonMsg.setBody(new AmqpValue(data));
        return send(topic, protonMsg, properties, sendOptions == null ? defaultSendOptions : sendOptions, listener, context);
    }

    @Override
    public <T> boolean send(String topic, ByteBuffer data, Map<String, Object> properties,
            SendOptions sendOptions, CompletionListener<T> listener, T context)
            throws StateException {
        if (data == null) throw new IllegalArgumentException("data cannot be null");
        org.apache.qpid.proton.message.Message protonMsg = Proton.message();
        int pos = data.position();
        byte[] dataBytes = new byte[data.remaining()];
        data.get(dataBytes);
        data.position(pos);
        protonMsg.setBody(new AmqpValue(new Binary(dataBytes)));
        return send(topic, protonMsg, properties, sendOptions == null ? defaultSendOptions : sendOptions, listener, context);
    }

    @Override
    public <T> boolean send(String topic, Object json,
            Map<String, Object> properties, SendOptions sendOptions,
            CompletionListener<T> listener, T context) throws StateException {
        String jsonString;
        synchronized(gson) {
            jsonString = gson.toJson(json);
        }
        return sendJson(topic, jsonString, properties, sendOptions, listener, context);
    }

    @Override
    public <T> boolean send(String topic, Object json, Type type,
            Map<String, Object> properties, SendOptions sendOptions,
            CompletionListener<T> listener, T context) throws StateException {
        String jsonString;
        synchronized(gson) {
            jsonString = gson.toJson(json, type);
        }
        return sendJson(topic, jsonString, properties, sendOptions, listener, context);
    }

    @Override
    public <T> boolean sendJson(String topic, String json,
            Map<String, Object> properties, SendOptions sendOptions,
            CompletionListener<T> listener, T context)
    throws StateException {
        org.apache.qpid.proton.message.Message protonMsg = Proton.message();
        protonMsg.setBody(new AmqpValue(json));
        protonMsg.setContentType("application/json");
        return send(topic, protonMsg, properties, sendOptions == null ? defaultSendOptions : sendOptions, listener, context);
    }

    protected static String encodeTopic(String unencodedTopic) throws IllegalArgumentException {
        String[] topicFragments = unencodedTopic.split("(?=(?!^)/)|(?<=/)");
        StringBuilder amqpAddress = new StringBuilder("amqp:///");
        for (int i = 0; i < topicFragments.length; ++i) {
            if ("/".equals(topicFragments[i])) {
                amqpAddress.append("/");
            } else {
                try {
                    amqpAddress.append(URLEncoder.encode(topicFragments[i], "UTF-8"));
                } catch(UnsupportedEncodingException e) {
                    throw new IllegalArgumentException("topic cannot be encoded into URL encoded UTF-8", e);
                }

            }
        }
        return amqpAddress.toString();
    }

    protected static boolean isValidPropertyValue(Object value) {
        if (value == null) {
            return true;
        }
        for (int i = 0; i < validPropertyValueTypes.length; ++i) {
            if (validPropertyValueTypes[i].isAssignableFrom(value.getClass())) {
                return true;
            }
        }
        return false;
    }

    private <T> boolean send(String topic, org.apache.qpid.proton.message.Message protonMsg,
                                       Map<String, Object> properties,
                                       SendOptions sendOptions, CompletionListener<T> listener, T context) {
        if (topic == null) throw new IllegalArgumentException("topic cannot be null");

        protonMsg.setAddress(encodeTopic(topic));
        protonMsg.setTtl(sendOptions.getTtl());
        Map<String, Object> amqpProperties = new HashMap<>();
        if ((properties != null) && !properties.isEmpty()) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (!isValidPropertyValue(entry.getValue())) {
                    throw new IllegalArgumentException("Property key '" + entry.getKey() + " specifies a value which is not of a supported type");
                }
                if (entry.getValue() instanceof byte[]) {
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

        InternalSend<T> is = new InternalSend<T>(this, topic, sendOptions.getQos(), data, length);
        ++undrainedSends;
        tell(is, this);
        is.future.setListener(callbackService, listener, context);
        boolean result = undrainedSends < 2;
        pendingDrain |= !result;
        return result;
    }

    @Override
    public <T> NonBlockingClient start(CompletionListener<T> listener, T context) {
        InternalStart<T> is = new InternalStart<T>(this);
        is.future.setListener(callbackService, listener, context);
        tell(is, this);
        return this;
    }

    @Override
    public <T> void stop(CompletionListener<T> listener, T context) {
        InternalStop<T> is = new InternalStop<T>(this);
        is.future.setListener(callbackService, listener, context);
        tell(is, this);
    }

    private final String buildAmqpTopicName(String topicPattern, String shareName) {
        String subTopic;
        if (shareName == null || "".equals(shareName)) {
            subTopic = "private:" + topicPattern;
        } else {
            if (shareName.contains(":")) throw new IllegalArgumentException();
            subTopic = "share:" + shareName + ":" + topicPattern;
        }
        return subTopic;
    }

    @Override
    public <T> NonBlockingClient subscribe(String topicPattern,
            SubscribeOptions subOptions, DestinationListener<T> destListener,
            CompletionListener<T> compListener, T context)
            throws StateException, IllegalArgumentException {
        if (topicPattern == null) throw new IllegalArgumentException("Topic pattern cannot be null");
        if (destListener == null) throw new IllegalArgumentException("DestinationListener cannot be null");
        if (subOptions == null) subOptions = defaultSubscribeOptions;
        String subTopic = buildAmqpTopicName(topicPattern, subOptions.getShareName());
        boolean autoConfirm = subOptions.getAutoConfirm() || subOptions.getQOS() == QOS.AT_MOST_ONCE;
        InternalSubscribe<T> is =
                new InternalSubscribe<T>(this, subTopic, subOptions.getQOS(), subOptions.getCredit(), autoConfirm, (int)(subOptions.getTtl() / 1000L), gsonBuilder, destListener, context);
        tell(is, this);

        is.future.setListener(callbackService, compListener, context);
        return this;
    }

    @Override
    public <T> NonBlockingClient unsubscribe(String topicPattern, String share, int ttl, CompletionListener<T> listener, T context)
    throws StateException, IllegalArgumentException {
        if (topicPattern == null) throw new IllegalArgumentException("Topic pattern cannot be null");
        if (ttl != 0) throw new IllegalArgumentException("TTL cannot be non-zero");

        InternalUnsubscribe<T> us = new InternalUnsubscribe<T>(this, topicPattern, share, ttl == 0);
        tell(us, this);

        us.future.setListener(callbackService, listener, context);
        return this;
    }

    @Override
    public <T> NonBlockingClient unsubscribe(String topicPattern, String share, CompletionListener<T> listener, T context)
    throws StateException {
        if (topicPattern == null) throw new IllegalArgumentException("Topic pattern cannot be null");
        InternalUnsubscribe<T> us = new InternalUnsubscribe<T>(this, topicPattern, share, false);
        tell(us, this);

        us.future.setListener(callbackService, listener, context);
        return this;
    }

    protected static String[] crackLinkName(String linkName) {
        String topicPattern;
        String share;
        if (linkName.startsWith("share:")) {
            share = linkName.substring("share:".length());
            topicPattern = share.substring(share.indexOf(':')+1);
            share = share.substring(0, share.indexOf(':'));
        } else {
            topicPattern = linkName.substring("private:".length());
            share = null;
        }
        return new String[] {topicPattern, share};
    }

    @Override
    protected void onReceive(Message message) {
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
                if ((or.exception.getMessage() != null) &&
                    (or.exception.getMessage().toLowerCase().contains("sasl") || or.exception.getMessage().toLowerCase().contains("failedloginexception"))) {

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
                SendRequest sr = new SendRequest(currentConnection, is.topic, is.data, is.length, is.qos);
                outstandingSends.put(sr, is);
                engine.tell(sr, this);
            } else if (NonBlockingClientState.queueingWorkStates.contains(state)) {
                pendingWork.addLast(is);
            } else {  // Assume state is in NonBlockingClientState.sendFail
                is.future.postFailure(callbackService, new StateException("Cannot send messages because the client is in stopped state"));
            }

        } else if (message instanceof SendResponse) {
            SendResponse sr = (SendResponse)message;
            InternalSend<?> is = outstandingSends.remove(sr.request);
            if (is != null) {
                if (sr.cause == null) {
                    is.future.postSuccess(callbackService);
                } else {
                    is.future.postFailure(callbackService, sr.cause);
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
                        String[] topicElements = crackLinkName(is.topic);
                        String errMsg = "Cannot subscribe because the client is already subscribe to topic '" + topicElements[0] + "'";
                        if (topicElements[1] != null) {
                            errMsg = errMsg + " and share '" + topicElements[1] + "'.";
                        }
                        is.future.postFailure(callbackService, new StateException(errMsg));
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
                is.future.postFailure(callbackService, new StateException("Cannot subscribe because the client is in stopped state"));
            }

        } else if (message instanceof SubscribeResponse) {
            SubscribeResponse sr = (SubscribeResponse)message;
            SubData sd = subscribedDestinations.get(sr.topic);
            if (sd != null) {
                sd.inProgressSubscribe.future.postSuccess(callbackService);
                sd.inProgressSubscribe = null;
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
            String amqpTopic = buildAmqpTopicName(iu.topicPattern, iu.share);
            SubData sd = subscribedDestinations.get(amqpTopic);
            NonBlockingClientState state = stateMachine.getState();

            if (NonBlockingClientState.acceptingWorkStates.contains(state)) {
                if (sd == null) {
                    String errMsg = "Client is not subscribed to topic '" + iu.topicPattern + "'";
                    if (iu.share != null) {
                        errMsg = " and share '" + iu.share + "'";
                    }
                    StateException se = new StateException(errMsg);
                    iu.future.postFailure(callbackService, se);
                } else if (sd.pending.isEmpty()) {
                    if (sd.state == SubData.State.ATTACHING) {
                        pendingWork.addLast(iu);
                    } else if (sd.state == SubData.State.DETATCHING) {
                        StateException se = new StateException("Client is not subscribed to " +
                                ((iu.share == null || "".equals(iu.share)) ? "private" : "shared") +
                                "destination " + iu.topicPattern);
                        iu.future.postFailure(callbackService, se);
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
                iu.future.postFailure(callbackService, new StateException("Cannot unsubscribe because the client is in stopped state"));
            }
        } else if (message instanceof UnsubscribeResponse) {
            // This needs to be tolerant of receiving an unsubscribe response before we've issued an
            // unsubscribe request (in the case that the server closes the link)
            UnsubscribeResponse ur = (UnsubscribeResponse)message;
            SubData sd = subscribedDestinations.remove(ur.topic);
            String[] parts = crackLinkName(ur.topic);
            sd.listener.onUnsubscribed(callbackService, parts[0], parts[1]);
            if (sd.inProgressUnsubscribe != null) {
                sd.inProgressUnsubscribe.future.postSuccess(callbackService);
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
            final SubData subData = subscribedDestinations.get(dr.topicPattern);
            pendingDeliveries.add(dr);
            subData.listener.onDelivery(callbackService, dr, subData.qos, subData.autoConfirm);
        } else if (message instanceof DisconnectNotification) {
            remakingInboundLinks = false;
            DisconnectNotification dn = (DisconnectNotification)message;
            if ("ServerContext_Takeover".equals(dn.condition)) {
                if (lastException == null) lastException = new ReplacedException(dn.description);
                stateMachine.fire(NonBlockingClientTrigger.REPLACED);
            } else {
                if (lastException == null) lastException = new ClientException(dn.description);
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
            logger.debug("Unexpected message received {} from {} ", message, message.getSender());
        }
    }

    @Override
    public void startTimer() {
        timerPromise = new TimerPromiseImpl(this, null);
        timer.schedule(retryDelay, timerPromise);
    }

    @Override
    public void openConnection() {
        engine.tell(new OpenRequest(currentEndpoint, clientId), this);
    }

    @Override
    public void closeConnection() {
        pendingDeliveries.clear();
        engine.tell(new CloseRequest(currentConnection), this);
    }

    @Override
    public void cancelTimer() {
        if (timerPromise != null) {
            TimerPromiseImpl tmp = timerPromise;
            timerPromise = null;
            timer.cancel(tmp);
        }
    }

    @Override
    public void requestEndpoint() {
        endpointService.lookup(new EndpointPromiseImpl(this));
    }

    @Override
    public void remakeInboundLinks() {
        if (subscribedDestinations.isEmpty()) {
            stateMachine.fire(NonBlockingClientTrigger.SUBS_REMADE);
        } else {
            remakingInboundLinks = true;
            for (Map.Entry<String, SubData>entry : subscribedDestinations.entrySet()) {
                SubData data = entry.getValue();
                data.state = SubData.State.ATTACHING;
                SubscribeRequest sr = new SubscribeRequest(currentConnection, entry.getKey(), data.qos, data.credit, data.ttl);
                engine.tell(sr, this);
            }
        }
    }


    @Override
    public void blessEndpoint() {
        serviceUri = (currentEndpoint.useSsl() ? "amqps://" : "amqp://") +
                     currentEndpoint.getHost() + ":" + currentEndpoint.getPort();
        retryDelay = 0;
        endpointService.onSuccess(currentEndpoint);
    }

    @Override
    public void cleanup() {
        pendingDeliveries.clear();

        // Fire a drain notification if required.
        undrainedSends = 0;
        if (pendingDrain) {
            pendingDrain = false;
            clientListener.onDrain(callbackService);
        }

        // Flush any pending subscribe operations into pending work queue
        for (Map.Entry<String, SubData> entry : subscribedDestinations.entrySet()) {
            SubData subData = entry.getValue();
            if (subData.inProgressSubscribe != null) {
                subData.inProgressSubscribe.future.postFailure(callbackService, new StateException("Cannot subscribe because the client is in stopped state"));
                subData.inProgressSubscribe = null;
            }
            if (subData.state == SubData.State.ESTABLISHED) {
                String parts[] = crackLinkName(entry.getKey());
                subData.listener.onUnsubscribed(callbackService, parts[0], parts[1]);
            }
            if (subData.inProgressUnsubscribe != null) {
                subData.inProgressUnsubscribe.future.postFailure(callbackService, new StateException("Cannot unsubscribe because the client is in stopped state"));
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
                send.future.postSuccess(callbackService);
            } else {
                send.future.postFailure(callbackService, new StateException("Cannot send messages because the client is in stopped state"));
            }
        }

        // Fail any pending work
        for (QueueableWork work : pendingWork) {
            if (work instanceof InternalSend<?>) {
                InternalSend<?> is = (InternalSend<?>)work;
                StateException stateException = new StateException("Cannot send messages because the client is in stopped state");
                is.future.postFailure(callbackService, stateException);
            } else if (work instanceof InternalSubscribe<?>) {
                InternalSubscribe<?> is = (InternalSubscribe<?>)work;
                StateException stateException = new StateException("Cannot subscribe because the client is in stopped state");
                is.future.postFailure(callbackService, stateException);
            } else {  // work instanceof InternalUnsubscribe
                InternalUnsubscribe<?> iu  = (InternalUnsubscribe<?>)work;
                StateException stateException = new StateException("Cannot unsubscribe because the client is in stopped state");
                iu.future.postFailure(callbackService, stateException);
            }
        }

        timerPromise = null;
        currentConnection = null;
        remakingInboundLinks = false;
        serviceUri = null;

        // Ask the callback service to notify us when it has completed any previously
        // requested callback invocations (via a FlushResponse message to the onReceive() method)
        callbackService.run(new Runnable() {
            public void run() {}
        }, this, new CallbackPromiseImpl(this, false));

    }

    @Override
    public void failPendingStops() {
        while(!pendingStops.isEmpty()) {
            InternalStop<?> stop = pendingStops.removeFirst();
            stop.future.postFailure(callbackService, new StateException("Cannot stop client because of a subsequent start request"));
        }
    }

    @Override
    public void succeedPendingStops() {
        while(!pendingStops.isEmpty()) {
            InternalStop<?> stop = pendingStops.removeFirst();
            stop.future.postSuccess(callbackService);
        }
    }

    @Override
    public void failPendingStarts() {
        while(!pendingStarts.isEmpty()) {
            InternalStart<?> start = pendingStarts.removeFirst();
            start.future.postFailure(callbackService, new StateException("Cannot start client because of a subsequent stop request"));
        }
    }

    @Override
    public void succeedPendingStarts() {
        while(!pendingStarts.isEmpty()) {
            InternalStart<?> start = pendingStarts.removeFirst();
            start.future.postSuccess(callbackService);
        }
    }

    @Override
    public void processQueuedActions() {
        while (!pendingWork.isEmpty()) {
            tell((Message)pendingWork.removeFirst(), this);
        }
    }

    @Override
    public void eventStarting() {
        stoppedByUser = false;
        lastException = null;
        externalState = ClientState.STARTING;
    }

    @Override
    public void eventUserStopping() {
        externalState = ClientState.STOPPING;
    }

    @Override
    public void eventSystemStopping() {
        // Need to be careful because sometimes the client can be stopped by the user and then
        // a system problem be detected (in which case we get a user stopping followed by a
        // system stopping event - and should discard any error associated with the
        // system stopping event)...
        externalState = ClientState.STOPPING;
        if (lastException == null) stoppedByUser = true;
    }

    @Override
    public void eventStopped() {
        externalState = ClientState.STOPPED;
        clientListener.onStopped(callbackService, stoppedByUser ? null : lastException);
        stoppedByUser = false;
        lastException = null;
    }

    @Override
    public void eventStarted() {
        externalState = ClientState.STARTED;
        clientListener.onStarted(callbackService);
    }

    @Override
    public void eventRetrying() {
        externalState = ClientState.RETRYING;
        clientListener.onRetrying(callbackService, stoppedByUser ? null : lastException);
    }

    @Override
    public void eventRestarted() {
        externalState = ClientState.STARTED;
        clientListener.onRestarted(callbackService);
    }

    @Override
    public void breakInboundLinks() {

        pendingDeliveries.clear();

        undrainedSends = 0;
        if (pendingDrain) {
            pendingDrain = false;
            clientListener.onDrain(callbackService);
        }
        for (InternalSend<?> sendRequest : outstandingSends.values()) {
            if (sendRequest.qos == QOS.AT_MOST_ONCE) {
                // We don't know if the message made it or not - but based on this QOS - we have to assume it did...
                sendRequest.future.postSuccess(callbackService);
            } else {
                // And for this QOS - we can be pessimistic and assume it didn't...
                pendingWork.addLast(sendRequest);
            }
        }
        outstandingSends.clear();

        for (Map.Entry<String, SubData>entry : subscribedDestinations.entrySet()) {
            SubData subData = entry.getValue();
            while(!subData.pending.isEmpty()) {
                pendingWork.addLast(subData.pending.getFirst());
            }
            subData.state = SubData.State.BROKEN;
        }
    }

    // Result: true == it might have worked, false == it really didn't work!
    protected boolean doDelivery(DeliveryRequest request) {
        boolean result = pendingDeliveries.remove(request);
        if (result) {
            engine.tell(new DeliveryResponse(request), this);
        }
        return result;
    }
}
