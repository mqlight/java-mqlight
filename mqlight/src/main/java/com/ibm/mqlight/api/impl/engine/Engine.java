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
package com.ibm.mqlight.api.impl.engine;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.messaging.TerminusExpiryPolicy;
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Handler;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.impl.ProtocolTracer;
import org.apache.qpid.proton.engine.impl.TransportImpl;
import org.apache.qpid.proton.framing.TransportFrame;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.NetworkException;
import com.ibm.mqlight.api.NotPermittedException;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.ReplacedException;
import com.ibm.mqlight.api.StateException;
import com.ibm.mqlight.api.SubscribedException;
import com.ibm.mqlight.api.impl.ComponentImpl;
import com.ibm.mqlight.api.impl.Message;
import com.ibm.mqlight.api.impl.SubscriptionTopic;
import com.ibm.mqlight.api.impl.network.ConnectResponse;
import com.ibm.mqlight.api.impl.network.ConnectionError;
import com.ibm.mqlight.api.impl.network.DataRead;
import com.ibm.mqlight.api.impl.network.DisconnectResponse;
import com.ibm.mqlight.api.impl.network.NetworkClosePromiseImpl;
import com.ibm.mqlight.api.impl.network.NetworkConnectPromiseImpl;
import com.ibm.mqlight.api.impl.network.NetworkListenerImpl;
import com.ibm.mqlight.api.impl.network.NetworkWritePromiseImpl;
import com.ibm.mqlight.api.impl.network.WriteResponse;
import com.ibm.mqlight.api.impl.timer.PopResponse;
import com.ibm.mqlight.api.impl.timer.TimerPromiseImpl;
import com.ibm.mqlight.api.logging.FFDCProbeId;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;
import com.ibm.mqlight.api.network.NetworkChannel;
import com.ibm.mqlight.api.network.NetworkService;
import com.ibm.mqlight.api.timer.TimerService;

public class Engine extends ComponentImpl implements Handler {

    private static final Logger logger = LoggerFactory.getLogger(Engine.class);
    
    static class EngineProtocolTracer implements ProtocolTracer {
        private static final Logger logger = LoggerFactory.getLogger(EngineProtocolTracer.class);

        final String clientId;

        public EngineProtocolTracer(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public void receivedFrame(TransportFrame transportFrame) {
            logger.data("receivedFrame", (Object) clientId, transportFrame);
        }

        @Override
        public void sentFrame(TransportFrame transportFrame) {
            logger.data("sentFrame", (Object) clientId, transportFrame);
        }
    }

    private final NetworkService network;
    private final TimerService timer;

    public Engine(NetworkService network, TimerService timer) {
        final String methodName = "<init>";
        logger.entry(this, methodName, network, timer);

        if (network == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("NetworkService argument cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        if (timer == null) {
          final IllegalArgumentException exception = new IllegalArgumentException("TimerService argument cannot be null");
          logger.throwing(this, methodName, exception);
          throw exception;
        }
        this.network = network;
        this.timer = timer;

        logger.exit(this, methodName);
    }

    @Override
    protected void onReceive(Message message) {
        final String methodName = "onReceive";
        logger.entry(this, methodName, message);

        if (message instanceof OpenRequest) {
            OpenRequest or = (OpenRequest)message;
            NetworkListenerImpl listener = new NetworkListenerImpl(this);
            Promise<NetworkChannel> promise = new NetworkConnectPromiseImpl(this, or);
            network.connect(or.endpoint, listener, promise);
        }
        else if (message instanceof ConnectResponse) {
            // Message from network telling us that a connect request has completed...
            ConnectResponse cr = (ConnectResponse)message;
            OpenRequest or = (OpenRequest)cr.context;
            if (cr.exception != null) {
                or.getSender().tell(new OpenResponse(or, cr.exception), this);
            } else {
                Connection protonConnection = Proton.connection();
                Transport transport = Proton.transport();
                ProtocolTracer protocolTracer = new EngineProtocolTracer(or.clientId);
                ((TransportImpl) transport).setProtocolTracer(protocolTracer);
                transport.setIdleTimeout(or.endpoint.getIdleTimeout());
                transport.bind(protonConnection);
                Collector collector = Proton.collector();
                protonConnection.setContainer(or.clientId);
                protonConnection.setHostname(or.endpoint.getHost());
                protonConnection.open();
                Sasl sasl = transport.sasl();
                sasl.client();
                if (or.endpoint.getUser() == null) {
                    sasl.setMechanisms(new String[]{"ANONYMOUS"});
                } else {
                    sasl.plain(or.endpoint.getUser(), or.endpoint.getPassword());
                }
                Session session = protonConnection.session();
                session.open();
                protonConnection.collect(collector);

                EngineConnection engineConnection = new EngineConnection(protonConnection, session, or.getSender(), transport, collector, cr.channel);
                engineConnection.openRequest = or;
                protonConnection.setContext(engineConnection);
                cr.channel.setContext(engineConnection);

                // Write any data from Proton to the network.
                writeToNetwork(engineConnection);
            }
        } else if (message instanceof CloseRequest) {
            CloseRequest cr = (CloseRequest)message;
            Connection protonConnection = cr.connection.connection;
            EngineConnection engineConnection = (EngineConnection)protonConnection.getContext();
            if (engineConnection.timerPromise != null) {
                TimerPromiseImpl tmp = engineConnection.timerPromise;
                engineConnection.timerPromise = null;
                timer.cancel(tmp);
            }
            protonConnection.close();
            engineConnection.closeRequest = cr;
            writeToNetwork(engineConnection);
        } else if (message instanceof SendRequest) {
            SendRequest sr = (SendRequest)message;
       
            EngineConnection engineConnection = sr.connection;

            // Look to see if there is already a suitable sending link, and open one if there is not...
            Link link = sr.connection.connection.linkHead(EnumSet.of(EndpointState.ACTIVE),
                EnumSet.of(EndpointState.ACTIVE, EndpointState.UNINITIALIZED));
            Sender linkSender;
            boolean linkOpened = false;
            while(true) {
              if (link == null) {
                linkSender = sr.connection.session.sender(sr.topic);
                Source source = new Source();
                Target target = new Target();
                source.setAddress(sr.topic);
                target.setAddress(sr.topic);
                linkSender.setSource(source);
                linkSender.setTarget(target);
                linkSender.open();
                linkOpened = true;
                break;
              }
              if ((link instanceof Sender) && sr.topic.equals(link.getName())) {
                linkSender = (Sender)link;
                break;
              }
              link = link.next(EnumSet.of(EndpointState.ACTIVE),
                  EnumSet.of(EndpointState.ACTIVE, EndpointState.UNINITIALIZED));
            }
            Delivery d = linkSender.delivery(String.valueOf(engineConnection.deliveryTag++).getBytes(Charset.forName("UTF-8")));

            linkSender.send(sr.buf.array(), 0, sr.length);

            if (sr.qos == QOS.AT_MOST_ONCE) {
              d.settle();
            } else {
              engineConnection.inProgressOutboundDeliveries.put(d, sr);
            }
            linkSender.advance();
            engineConnection.drained = false;
            int delta = engineConnection.transport.head().remaining();
            // If the link was also opened as part of processing this request then increase the
            // amount of data expected (as the linkSender.send() won't count against the amount of
            // data in transport.head() unless there is link credit - which there won't be until
            // the server responds to the link open).
            if (linkOpened) {
              delta += sr.length;
            }
            if (sr.qos == QOS.AT_MOST_ONCE) {
              engineConnection.addInflightQos0(delta, new SendResponse(sr, null), sr.getSender(), this);
            }
            writeToNetwork(engineConnection);

        } else if (message instanceof SubscribeRequest) {
            SubscribeRequest sr = (SubscribeRequest) message;
            EngineConnection engineConnection = sr.connection;
            if (engineConnection.subscriptionData.containsKey(sr.topic.toString())) {
                // The client is already subscribed - should not really occur
                final SubscribedException exception = new SubscribedException("Cannot subscribe because the client is already subscribed to topic "+sr.topic.toString());
                sr.getSender().tell(new SubscribeResponse(engineConnection, sr.topic, exception), this);
            } else {
                Receiver linkReceiver = sr.connection.session.receiver(sr.topic.getTopic());
                engineConnection.subscriptionData.put(sr.topic.toString(), new EngineConnection.SubscriptionData(sr.getSender(), sr.initialCredit, linkReceiver));
                Source source = new Source();
                source.setAddress(sr.topic.getTopic());
                Target target = new Target();
                target.setAddress(sr.topic.getTopic());

                if (sr.ttl > 0) {
                    source.setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);
                    source.setTimeout(UnsignedInteger.valueOf(sr.ttl));
                    target.setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);
                    target.setTimeout(UnsignedInteger.valueOf(sr.ttl));
                }

                linkReceiver.setSource(source);
                linkReceiver.setTarget(target);
                if (sr.qos == QOS.AT_LEAST_ONCE) {
                    linkReceiver.setSenderSettleMode(SenderSettleMode.UNSETTLED);
                    linkReceiver.setReceiverSettleMode(ReceiverSettleMode.FIRST);
                } else {
                    linkReceiver.setSenderSettleMode(SenderSettleMode.SETTLED);
                    linkReceiver.setReceiverSettleMode(ReceiverSettleMode.FIRST);
                }
                
                if (sr.topic.isShared()) {
                  source.setCapabilities(Symbol.valueOf("shared"));
                }

                linkReceiver.open();
                linkReceiver.flow(sr.initialCredit);

                writeToNetwork(engineConnection);
            }
        } else if (message instanceof UnsubscribeRequest) {
            UnsubscribeRequest ur = (UnsubscribeRequest) message;
            EngineConnection engineConnection = ur.connection;
            EngineConnection.SubscriptionData sd = engineConnection.subscriptionData.get(ur.topic.toString());
            Target t = (Target)sd.receiver.getTarget();
            Source s = (Source)sd.receiver.getSource();
            if (ur.zeroTtl) {
                t.setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);
                t.setTimeout(new UnsignedInteger(0));
                s.setTimeout(new UnsignedInteger(0));
                s.setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);
            }

            // detach link if expiry is still in effect, else close
            if (t.getExpiryPolicy() == TerminusExpiryPolicy.NEVER ||
                    t.getTimeout().longValue() > 0) {
                sd.receiver.detach();
            } else {
                sd.receiver.close();
            }
            writeToNetwork(engineConnection);

        } else if (message instanceof DeliveryResponse) {
            DeliveryResponse dr = (DeliveryResponse)message;
            Delivery delivery = dr.request.delivery;
            delivery.settle();
            
            EngineConnection engineConnection = (EngineConnection)dr.request.protonConnection.getContext();
            EngineConnection.SubscriptionData subData = engineConnection.subscriptionData.get(dr.request.topicPattern);
            if (subData == null) {
              if (dr.request.qos != QOS.AT_MOST_ONCE) {
                throw new StateException("Client had unsubscribed from '" + dr.request.topicPattern + "' before delivery was confirmed");
              }
            } else {
              subData.settled++;
              subData.unsettled--;

              double available = subData.maxLinkCredit - subData.unsettled;
              if ((available / subData.settled) <= 1.25 ||
                  (subData.unsettled == 0 && subData.settled > 0)) {
                subData.receiver.flow(subData.settled);
                subData.settled = 0;
              }
            }

            writeToNetwork(engineConnection);
            
            // send the DeliveryResponse back to indicate settlement has been actioned
            engineConnection.requestor.tell(message, this);

        } else if (message instanceof WriteResponse) {
            // Message from network telling us that a write operation has completed...
            // Try to flush any pending data to the network...
            WriteResponse wr = (WriteResponse)message;
            EngineConnection engineConnection = (EngineConnection)wr.context;
            if (engineConnection != null) {
                engineConnection.bytesWritten += wr.amount;
                engineConnection.notifyInflightQos0(false);
                
                // If all buffered network data has been sent and the last send request could not be sent immediately
                // then send a drain event to inform the client that it is ok to send more messages
                if (wr.drained && !engineConnection.drained) {
                    engineConnection.drained = true;
                    engineConnection.requestor.tell(new DrainNotification(), this); 
                }
                if (engineConnection.transport.pending() > 0) {
                    writeToNetwork(engineConnection);
                }
            }
        } else if (message instanceof DataRead) {
            // Message from the network telling us that data has been read...
            DataRead dr = (DataRead) message;
            try {
                EngineConnection engineConnection = (EngineConnection) dr.channel.getContext();
                if (!engineConnection.closed) {
                    final ByteBuffer buffer = dr.buffer.nioBuffer();
                    while (buffer.remaining() > 0) {
                        int origLimit = buffer.limit();
                        ByteBuffer tail = engineConnection.transport.tail();
                        int amount = Math.min(tail.remaining(), buffer.remaining());
                        buffer.limit(buffer.position() + amount);
                        tail.put(buffer);
                        buffer.limit(origLimit);
                        engineConnection.transport.process();
                        process(engineConnection.collector);
                    }

                    // Write any data from Proton to the network.
                    writeToNetwork(engineConnection);
                }
            } finally {
                dr.buffer.release();
            }
        } else if (message instanceof DisconnectResponse) {
            // Message from network telling us that it has completed our disconnect request.
            DisconnectResponse dr = (DisconnectResponse)message;
            CloseRequest cr = (CloseRequest)dr.context;
            if (cr != null) {
                cr.connection.closed = true;
                cr.connection.notifyInflightQos0(true);
                cr.getSender().tell(new CloseResponse(cr), this);
            }
        } else if (message instanceof ConnectionError) {
            // Message from network telling us that a error has occurred at the TCP/IP level.
            ConnectionError ce = (ConnectionError)message;
            EngineConnection engineConnection = (EngineConnection)ce.channel.getContext();
            if (!engineConnection.closed) {
                if (engineConnection.timerPromise != null) {
                    TimerPromiseImpl tmp = engineConnection.timerPromise;
                    engineConnection.timerPromise = null;
                    timer.cancel(tmp);
                }
                engineConnection.notifyInflightQos0(true);
                engineConnection.closed = true;
                engineConnection.transport.close_tail();
                engineConnection.requestor.tell(new DisconnectNotification(
                        engineConnection, ce.cause), this);
            }
        } else if (message instanceof PopResponse) {
            PopResponse pr = (PopResponse)message;
            EngineConnection engineConnection = (EngineConnection)pr.promise.getContext();
            long now = System.currentTimeMillis();
            long timeout = engineConnection.transport.tick(now);
            logger.data(this, methodName, "Timeout: {}", timeout);
            if (timeout > 0) {
                TimerPromiseImpl promise = new TimerPromiseImpl(this, engineConnection);
                engineConnection.timerPromise = promise;
                logger.data(this, methodName, "Scheduling at: {}", timeout - now);
                timer.schedule(timeout - now, promise);
                writeToNetwork(engineConnection);
            }
        }

        logger.exit(this, methodName);
    }

    // Drains any pending data from a Proton transport object onto the network
    private void writeToNetwork(EngineConnection engineConnection) {
      final String methodName = "writeToNetwork";
      logger.entry(this, methodName, engineConnection);

        if (engineConnection.transport.pending() > 0) {
            ByteBuffer head = engineConnection.transport.head();
            int amount = head.remaining();
            engineConnection.channel.write(head, new NetworkWritePromiseImpl(this, amount, engineConnection));
            engineConnection.transport.pop(amount);
            engineConnection.transport.tick(System.currentTimeMillis());
        }

        logger.exit(this, methodName);
    }

    /** Runs scheduled asynchronous Tasks. */
    private final ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
    /** A scheduled task that runs if we receive no data from the client in the scheduled time. */
    private ScheduledFuture<?> receiveScheduledFuture;

    /**
     * Reset the local idle timers, now that we have received some data.
     *
     * If we have set an idle timeout the client must send some data at least that often,
     * we double the timeout before checking.
     */
    private void resetReceiveIdleTimer(Event event) {
        final String methodName = "resetReceiveIdleTimer";
        logger.entry(this, methodName, event);

        if (receiveScheduledFuture != null) {
            receiveScheduledFuture.cancel(false);
        }

        final Transport transport = event.getTransport();
        if (transport != null) {
            final int localIdleTimeOut = transport.getIdleTimeout();
            if (localIdleTimeOut > 0) {
                Runnable receiveTimeout = new Runnable() {
                    @Override
                    public void run() {
                        final String methodName = "run";
                        logger.entry(this, methodName);
                        transport.process();
                        transport.tick(System.currentTimeMillis());
                        logger.exit(methodName);
                    }
                };
                receiveScheduledFuture = scheduler.schedule(receiveTimeout,
                        localIdleTimeOut, TimeUnit.MILLISECONDS);
            }
        }
        logger.exit(this, methodName);
    }

    private void process(Collector collector) {
        final String methodName = "process";
        logger.entry(this, methodName, collector);

        while (collector.peek() != null) {
            Event event = collector.peek();
            logger.data(this, methodName, "Processing event: {}", event.getType());
            event.dispatch(this);
            resetReceiveIdleTimer(event);

            collector.pop();
        }

        logger.exit(this, methodName);
    }

    private void processEventConnectionRemoteState(Event event) {
        final String methodName = "processEventConnectionRemoteState";
        logger.entry(this, methodName, event);

        if (event.getConnection().getRemoteState() == EndpointState.CLOSED) {
            final ErrorCondition remoteCondition = event.getConnection().getRemoteCondition();
            final EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
            if (engineConnection.timerPromise != null) {
                TimerPromiseImpl tmp = engineConnection.timerPromise;
                engineConnection.timerPromise = null;
                timer.cancel(tmp);
            }

            if (event.getConnection().getLocalState() == EndpointState.CLOSED || engineConnection.openRequest == null) {
                if (!engineConnection.closed) {
                    engineConnection.notifyInflightQos0(true);
                    engineConnection.closed = true;
                    CloseRequest cr = engineConnection.closeRequest;
                    engineConnection.closeRequest = null;
                    NetworkClosePromiseImpl future = new NetworkClosePromiseImpl(this, cr);
                    engineConnection.channel.close(future);
                    if (cr == null) {
                        Throwable error = getClientException(remoteCondition);
                        engineConnection.requestor.tell(new DisconnectNotification(engineConnection, error), this);
                    }
                }
            } else {
                OpenRequest req = engineConnection.openRequest;
                engineConnection.openRequest = null;
                if (!engineConnection.closed) {
                    engineConnection.notifyInflightQos0(true);
                    engineConnection.closed = true;
                    engineConnection.channel.close(null);

                    final ClientException clientException;
                    // check for SASL failures
                    final Sasl sasl = engineConnection.transport.sasl();
                    if (sasl.getOutcome() == Sasl.SaslOutcome.PN_SASL_AUTH) {
                        clientException = new com.ibm.mqlight.api.SecurityException(
                                "Failed to authenticate with server - invalid username or password",
                                getClientException(remoteCondition));
                    } else {
                        // else just report error condition on event
                        if (remoteCondition == null
                                || remoteCondition.getDescription() == null) {
                            clientException = new NetworkException(
                                    "The server closed the connection without providing any error information.");
                        } else {
                            clientException = getClientException(remoteCondition);
                        }
                    }
                    req.getSender().tell(new OpenResponse(req, clientException), this);
                }
            }
        } else if (event.getConnection().getRemoteState() == EndpointState.ACTIVE) {
            EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
            long now = System.currentTimeMillis();
            long timeout = engineConnection.transport.tick(now);
            if (timeout > 0) {
                engineConnection.timerPromise = new TimerPromiseImpl(this, engineConnection);
                timer.schedule(timeout - now, engineConnection.timerPromise);
            }
        }

        logger.exit(this, methodName);
    }

    private ClientException getClientException(ErrorCondition errorCondition) {
        final String methodName = "getClientException";
        logger.entry(this, methodName, errorCondition);

        ClientException result = null;

        if (errorCondition != null && errorCondition.getCondition() != null) {
            if (errorCondition.getDescription().toString().contains("_Takeover")) {
                result = new ReplacedException(errorCondition.getDescription());
            } else if (errorCondition.getCondition().equals(AmqpError.PRECONDITION_FAILED)
                    || errorCondition.getCondition().equals(AmqpError.NOT_ALLOWED)
                    || errorCondition.getCondition().equals(AmqpError.NOT_IMPLEMENTED)
                    || errorCondition.getDescription().toString().contains("_InvalidSourceTimeout")) {
                result = new NotPermittedException(errorCondition.getDescription());
            }

            if (result == null && errorCondition.getDescription() != null) {
                if (errorCondition.getDescription().contains("sasl ") || errorCondition.getDescription().contains("SSL ")) {
                    result = new com.ibm.mqlight.api.SecurityException(errorCondition.getDescription());
                }
            }

            if (result == null) {
                String msg = errorCondition.getCondition().toString();
                if (errorCondition.getDescription() != null) {
                    msg += ": " + errorCondition.getDescription();
                }
                result = new NetworkException(msg);
            }
        }

        logger.exit(this, methodName, result);
        return result;
    }

    private void processEventLinkLocalState(Event event) {
        final String methodName = "processEventLinkLocalState";
        logger.entry(this, methodName, event);

        Link link = event.getLink();
        logger.data(this, methodName, "LINK_LOCAL {} {} {}", link, link.getLocalState(), link.getRemoteState());
        logger.exit(this, methodName);
    }

    private void processEventLinkRemoteState(Event event) {
        final String methodName = "processEventLinkRemoteState";
        logger.entry(this, methodName, event);

        Link link = event.getLink();
        logger.data(this, methodName, "LINK_REMOTE {} {} {}", link, link.getLocalState(), link.getRemoteState());

        final Event.Type eventType = event.getType();

        if (link instanceof Receiver) {
            if (eventType == Event.Type.LINK_REMOTE_OPEN) {
                // Receiver link open has been ack'ed by server.
                if (link.getLocalState() == EndpointState.ACTIVE) {
                    if (link.getRemoteState() == EndpointState.ACTIVE) {
                        EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
                        EngineConnection.SubscriptionData sd = engineConnection.subscriptionData.get(link.getName());
                        sd.subscriber.tell(new SubscribeResponse(engineConnection, new SubscriptionTopic(link.getName())), this);
                    } else if (link.getRemoteState() == EndpointState.CLOSED) {
                        // link was immediately closed remotely after being ack'ed?
                        ClientException clientException = getClientException(link.getRemoteCondition());
                        logger.data(this, methodName, event, clientException, this);
                    }
                }
            } else if (eventType == Event.Type.LINK_REMOTE_CLOSE
                    || eventType == Event.Type.LINK_REMOTE_DETACH) {
                // Receiver link has been closed by the server.
                if (link.getRemoteState() == EndpointState.CLOSED) {
                    ClientException clientException = getClientException(link.getRemoteCondition());
                    if (link.getLocalState() != EndpointState.CLOSED && !link.detached()) {
                        if (clientException == null) {
                            clientException = new ClientException(
                                "The server indicated that the destination was unsubscribed due to an error condition, " +
                                "without providing any further error information.");
                        }
                        link.close();
                    }
                    link.free();

                    EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
                    EngineConnection.SubscriptionData sd = engineConnection.subscriptionData.remove(link.getName());

                    if (sd == null) {
                      logger.ffdc(this, methodName, FFDCProbeId.PROBE_001, null, this, event);
                    } else {
                        sd.subscriber.tell(new UnsubscribeResponse(engineConnection, new SubscriptionTopic(link.getName()), clientException), this);
                    }
                }

            }
        } else if (link instanceof Sender) {
            if (eventType == Event.Type.LINK_REMOTE_CLOSE &&
                    link.getRemoteState() == EndpointState.CLOSED) {
                if (link.getLocalState() != EndpointState.CLOSED) {
                    String msg = "The server indicated that our sending link was closed due to an error condition, ";
                    ErrorCondition remoteCondition = link.getRemoteCondition();
                    if (remoteCondition == null || remoteCondition.getCondition() == null) {
                        msg += "without providing any further error information.";
                    } else {
                        msg += remoteCondition.getCondition().toString();
                        if (remoteCondition.getDescription() != null) {
                            msg += " - " + remoteCondition.getDescription();
                        }
                    }
                    logger.data(this, methodName, msg, link.getTarget().getAddress(), this);
                    EngineConnection engineConnection = (EngineConnection) event.getConnection().getContext();
                    for (Delivery delivery = link.head(); delivery != null; delivery = delivery.next()) {
                        SendRequest sr = engineConnection.inProgressOutboundDeliveries.remove(delivery);
                        if (sr != null && sr.getSender() != null) {
                            sr.getSender().tell(new SendResponse(sr, new ClientException(msg)), this);
                        }
                    }
                    link.close();
                }
                link.free();
            }
        }

        logger.exit(this, methodName);

    }

    private void processEventSessionRemoteState(Event event) {
        final String methodName = "processEventSessionRemoteState";
        logger.entry(this, methodName, event);

        if (event.getSession().getRemoteState() == EndpointState.ACTIVE) {
            if (event.getSession().getLocalState() == EndpointState.ACTIVE) {
                final EngineConnection engineConnection =
                        (EngineConnection) event.getConnection().getContext();
                if (!engineConnection.closed) {
                    // First session has opened on the connection
                    OpenRequest req = engineConnection.openRequest;
                    engineConnection.openRequest = null;
                    engineConnection.requestor.tell(new OpenResponse(req, engineConnection), this);
                }
            } else {
                // The remote end is trying to establish a new session with us, which is not allowed. I don't think this is a usual case,
                // but could occur with a badly written remote end (i.e. sends an initial AMQP open immediately followed by a begin)
                final Connection protonConnection = event.getConnection();
                protonConnection.setCondition(new ErrorCondition(Symbol.getSymbol("mqlight:session-remote-open-rejected"),
                                                                 "MQ Light client is unable to accept an open session request"));
                protonConnection.close();
            }
        }

        logger.exit(this, methodName);
    }

    @Override
    public void onConnectionInit(Event e) {
      // No action required
    }

    @Override
    public void onConnectionLocalOpen(Event e) {
      // No action required
    }

    @Override
    public void onConnectionRemoteOpen(Event e) {
      processEventConnectionRemoteState(e);
    }

    @Override
    public void onConnectionLocalClose(Event e) {
      // No action required
    }

    @Override
    public void onConnectionRemoteClose(Event e) {
      processEventConnectionRemoteState(e);
    }

    @Override
    public void onConnectionBound(Event e) {
      // No action required
    }

    @Override
    public void onConnectionUnbound(Event e) {
      // No action required
    }

    @Override
    public void onConnectionFinal(Event e) {
      // No action required
    }

    @Override
    public void onSessionInit(Event e) {
      // No action required
    }

    @Override
    public void onSessionLocalOpen(Event e) {
      // No action required
    }

    @Override
    public void onSessionRemoteOpen(Event e) {
      processEventSessionRemoteState(e);
    }

    @Override
    public void onSessionLocalClose(Event e) {
      // No action required
    }

    @Override
    public void onSessionRemoteClose(Event e) {
      processEventSessionRemoteState(e);
    }

    @Override
    public void onSessionFinal(Event e) {
      // No action required
    }

    @Override
    public void onLinkInit(Event e) {
      // No action required
    }

    @Override
    public void onLinkLocalOpen(Event e) {
      processEventLinkLocalState(e);
    }

    @Override
    public void onLinkRemoteOpen(Event e) {
      processEventLinkRemoteState(e);
    }

    @Override
    public void onLinkLocalDetach(Event e) {
      processEventLinkLocalState(e);
    }

    @Override
    public void onLinkRemoteDetach(Event e) {
      processEventLinkRemoteState(e);
    }

    @Override
    public void onLinkLocalClose(Event e) {
      processEventLinkLocalState(e);
    }

    @Override
    public void onLinkRemoteClose(Event e) {
      processEventLinkRemoteState(e);
    }

    @Override
    public void onLinkFlow(Event e) {
      // No action required
    }

    @Override
    public void onLinkFinal(Event e) {
      // No action required
    }

    @Override
    public void onDelivery(Event event) {
      final String methodName = "onDelivery";
      logger.entry(this, methodName, event);

      EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
      Delivery delivery = event.getDelivery();
      if (event.getLink() instanceof Sender) {
          SendRequest sr = engineConnection.inProgressOutboundDeliveries.remove(delivery);
          Exception exception = null;
          if (delivery.getRemoteState() instanceof Rejected) {
              final Rejected rejected = (Rejected) delivery.getRemoteState();
              final ErrorCondition error = rejected.getError();
              if (error == null || error.getDescription() == null) {
                  exception = new Exception("Message was rejected");
              } else {
                  exception = new Exception(error.getDescription());
              }
          } else if (delivery.getRemoteState() instanceof Released) {;
              exception = new Exception("Message was released");
          } else if (delivery.getRemoteState() instanceof Modified) {
              exception = new Exception("Message was modified");
          }
          sr.getSender().tell(new SendResponse(sr, exception), this);
      } else if (delivery.isReadable() && !delivery.isPartial()) {    // Assuming link instanceof Receiver...
          Receiver receiver = (Receiver)event.getLink();
          int amount = delivery.pending();
          byte[] data = new byte[amount];
          receiver.recv(data, 0, amount);
          receiver.advance();

          EngineConnection.SubscriptionData subData = engineConnection.subscriptionData.get(event.getLink().getName());
          subData.unsettled++;
          QOS qos = delivery.remotelySettled() ? QOS.AT_MOST_ONCE : QOS.AT_LEAST_ONCE;
          subData.subscriber.tell(new DeliveryRequest(data, qos, event.getLink().getName(), delivery, event.getConnection()), this);
      }

      logger.exit(this, methodName);
    }

    @Override
    public void onTransport(Event e) {
      // No action required
    }

    @Override
    public void onTransportError(Event e) {
      // No action required
    }

    @Override
    public void onTransportHeadClosed(Event e) {
      // No action required
    }

    @Override
    public void onTransportTailClosed(Event e) {
      // No action required
    }

    @Override
    public void onTransportClosed(Event e) {
      // No action required
    }

    @Override
    public void onUnhandled(Event e) {
      final IllegalStateException exception = new IllegalStateException("Unknown event type: " + e.getType());
      logger.throwing(this, "onUnhandled", exception);
      throw exception;
    }
}
