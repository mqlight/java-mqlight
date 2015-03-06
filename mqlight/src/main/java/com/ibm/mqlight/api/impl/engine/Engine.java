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

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.EnumSet;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.messaging.TerminusExpiryPolicy;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.Transport;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.NetworkException;
import com.ibm.mqlight.api.NotPermittedException;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.ReplacedException;
import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.impl.Message;
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

public class Engine extends Component {

    private static final Logger logger = LoggerFactory.getLogger(Engine.class);

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
            //ConnectRequest connectRequest = new ConnectRequest(or.endpoint.getHost(), or.endpoint.getPort());
            //connectRequest.setContext(or);
            //nn.tell(connectRequest, this);
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
            //engineConnection.transport.close_head();
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
                    linkSender = sr.connection.session.sender(sr.topic);    // TODO: the Node.js client uses sender-xxx as a link name...
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
                    sr.topic.equals(link.getName());
                    linkSender = (Sender)link;
                    break;
                }
                link = link.next(EnumSet.of(EndpointState.ACTIVE),
                                            EnumSet.of(EndpointState.ACTIVE, EndpointState.UNINITIALIZED));
            }
            Delivery d = linkSender.delivery(String.valueOf(engineConnection.deliveryTag++).getBytes(Charset.forName("UTF-8")));

            linkSender.send(sr.buf.array(), 0, sr.length);
            sr.buf.release();

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
            // TODO: track credit in this class so that we can detect this case and more accurately
            //       calculate when the first message sent will have been flushed to the network.
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
            if (engineConnection.subscriptionData.containsKey(sr.topic)) {
                // The client is already subscribed...
                // TODO: should this be an error condition?
                sr.getSender().tell(new SubscribeResponse(engineConnection, sr.topic), this);
            } else {
                Receiver linkReceiver = sr.connection.session.receiver(sr.topic);
                engineConnection.subscriptionData.put(sr.topic, new EngineConnection.SubscriptionData(sr.getSender(), sr.initialCredit, linkReceiver));
                Source source = new Source();
                source.setAddress(sr.topic);
                Target target = new Target();
                target.setAddress(sr.topic);

                if (sr.ttl > 0) {
                    source.setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);
                    source.setTimeout(new UnsignedInteger(sr.ttl));
                    target.setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);
                    target.setTimeout(new UnsignedInteger(sr.ttl));
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

                linkReceiver.open();
                linkReceiver.flow(sr.initialCredit);

                writeToNetwork(engineConnection);
            }
        } else if (message instanceof UnsubscribeRequest) {
            UnsubscribeRequest ur = (UnsubscribeRequest) message;
            EngineConnection engineConnection = ur.connection;
            EngineConnection.SubscriptionData sd = engineConnection.subscriptionData.get(ur.topic);
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
            subData.settled++;
            subData.unsettled--;

            double available = subData.maxLinkCredit - subData.unsettled;
            if ((available / subData.settled) <= 1.25 ||
                (subData.unsettled == 0 && subData.settled > 0)) {
                subData.receiver.flow(subData.settled);
                subData.settled = 0;
            }

            writeToNetwork(engineConnection);

        } else if (message instanceof WriteResponse) {
            // Message from network telling us that a write operation has completed...
            // Try to flush any pending data to the network...
            WriteResponse wr = (WriteResponse)message;
            EngineConnection engineConnection = (EngineConnection)wr.context;
            if (engineConnection != null) {
                engineConnection.bytesWritten += wr.amount;
                engineConnection.notifyInflightQos0(false);
                if (engineConnection.transport.pending() > 0) {
                    writeToNetwork(engineConnection);
                } else if (!engineConnection.drained){
                    engineConnection.drained = true;
                    engineConnection.requestor.tell(new DrainNotification(), this);
                }
            }
        } else if (message instanceof DataRead) {
            // Message from the network telling us that data has been read...
            DataRead dr = (DataRead)message;
            EngineConnection engineConnection = (EngineConnection)dr.channel.getContext();
            while (dr.buffer.remaining() > 0) {
                int origLimit = dr.buffer.limit();
                ByteBuffer tail = engineConnection.transport.tail();
                int amount = Math.min(tail.remaining(), dr.buffer.remaining());
                dr.buffer.limit(dr.buffer.position() + amount);
                tail.put(dr.buffer);
                dr.buffer.limit(origLimit);
                engineConnection.transport.process();
                engineConnection.transport.tick(System.currentTimeMillis());
                process(engineConnection.collector);
            }

            // Write any data from Proton to the network.
            writeToNetwork(engineConnection);
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
            ByteBuffer tmp = ByteBuffer.allocate(amount);       // TODO: we could avoid allocating this if we were a bit smarter
            tmp.put(head);                                      //       about when we popped the transport...
            tmp.flip();
            //ByteBuf buf = Unpooled.wrappedBuffer(head);
            engineConnection.transport.pop(amount);
            engineConnection.transport.tick(System.currentTimeMillis());
            engineConnection.channel.write(tmp, new NetworkWritePromiseImpl(this, amount, engineConnection));
            //nn.tell(new WriteRequest(connection, buf), this);
        }

        logger.exit(this, methodName);
    }

    // TODO: Proton 0.8 provides an Event.dispatch() method that could be used to replace this code...
    private void process(Collector collector) {
        final String methodName = "process";
        logger.entry(this, methodName, collector);

        while (collector.peek() != null) {
            Event event = collector.peek();
            logger.data(this, methodName, "Processing event: {}", event.getType());
            switch(event.getType()) {   // TODO: could some of these be common'ed up? E.g. have one processEventConnection - which deals with both local and remote state changes
            case CONNECTION_BOUND:
            case CONNECTION_FINAL:
            case CONNECTION_INIT:
            case CONNECTION_UNBOUND:
                break;
            case CONNECTION_LOCAL_CLOSE:
            case CONNECTION_LOCAL_OPEN:
                processEventConnectionLocalState(event);
                break;
            case CONNECTION_REMOTE_CLOSE:
            case CONNECTION_REMOTE_OPEN:
                processEventConnectionRemoteState(event);
                break;
            case DELIVERY:
                processEventDelivery(event);
                break;
            case LINK_FINAL:
            case LINK_INIT:
                break;
            case LINK_FLOW:
                processEventLinkFlow(event);
                break;
            case LINK_LOCAL_CLOSE:
            case LINK_LOCAL_DETACH:
            case LINK_LOCAL_OPEN:
                processEventLinkLocalState(event);
                break;
            case LINK_REMOTE_CLOSE:
            case LINK_REMOTE_DETACH:
            case LINK_REMOTE_OPEN:
                processEventLinkRemoteState(event);
                break;
            case SESSION_FINAL:
            case SESSION_INIT:
                break;
            case SESSION_LOCAL_CLOSE:
            case SESSION_LOCAL_OPEN:
                processEventSessionLocalState(event);
                break;
            case SESSION_REMOTE_CLOSE:
            case SESSION_REMOTE_OPEN:
                processEventSessionRemoteState(event);
                break;
            case TRANSPORT:
            case TRANSPORT_CLOSED:
            case TRANSPORT_ERROR:
            case TRANSPORT_HEAD_CLOSED:
            case TRANSPORT_TAIL_CLOSED:
                processEventTransport(event);
                break;
            default:
                final IllegalStateException exception = new IllegalStateException("Unknown event type: " + event.getType());
                logger.throwing(this, methodName, exception);
                throw exception;
            }
            collector.pop();
        }

        logger.exit(this, methodName);
    }

    private void processEventConnectionLocalState(Event event) {
        final String methodName = "processEventConnectionLocalState";
        logger.entry(this, methodName, event);
        logger.exit(this, methodName);
    }

    private void processEventConnectionRemoteState(Event event) {
        final String methodName = "processEventConnectionRemoteState";
        logger.entry(this, methodName, event);

        if (event.getConnection().getRemoteState() == EndpointState.CLOSED) {
            if (event.getConnection().getLocalState() != EndpointState.CLOSED) {
                EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
                if (engineConnection.timerPromise != null) {
                    TimerPromiseImpl tmp = engineConnection.timerPromise;
                    engineConnection.timerPromise = null;
                    timer.cancel(tmp);
                }
                if (engineConnection.openRequest != null) {
                    OpenRequest req = engineConnection.openRequest;
                    engineConnection.openRequest = null;
                    if (!engineConnection.closed) {
                        engineConnection.notifyInflightQos0(true);
                        engineConnection.closed = true;
                        engineConnection.channel.close(null);
                        final String errMsg;
                        if (event.getConnection().getRemoteCondition() == null
                                || event.getConnection().getRemoteCondition().getDescription() == null) {
                            errMsg = "The server closed the connection without providing any error information.";
                        } else {
                            errMsg = event.getConnection().getRemoteCondition().getDescription();
                        }

                        final ClientException clientException;
                        // check for SASL failures
                        final Sasl sasl = engineConnection.transport.sasl();
                        if (sasl.getOutcome() == Sasl.SaslOutcome.PN_SASL_AUTH) {
                            clientException = new com.ibm.mqlight.api.SecurityException(
                                    "Failed to authenticate with server - invalid username or password",
                                    (event.getConnection().getRemoteCondition() == null) ? null : getClientException(errMsg));
                        } else {
                            // else just report error condition on event
                            clientException = getClientException(errMsg);
                        }
                        req.getSender().tell(new OpenResponse(req, clientException), this);
                    }
                } else {    // TODO: should we also special case closeRequest in progress??
                    if (!engineConnection.closed) {
                        engineConnection.notifyInflightQos0(true);
                        engineConnection.closed = true;
                        engineConnection.channel.close(null);
                        String condition = "";
                        if ((event.getConnection().getRemoteCondition() != null) && (event.getConnection().getRemoteCondition().getCondition() != null)) {
                            condition = event.getConnection().getRemoteCondition().getCondition().toString();
                        }
                        String description = "";
                        if ((event.getConnection().getRemoteCondition() != null) && (event.getConnection().getRemoteCondition().getDescription() != null)) {
                            description = event.getConnection().getRemoteCondition().getDescription();
                        }
                        Throwable error = null;
                        if ("ServerContext_Takeover".equals(condition)) {
                            error = new ReplacedException(description);
                        } else if (description.length() > 0) {
                            error = getClientException(description);
                        }
                        engineConnection.requestor.tell(new DisconnectNotification(engineConnection, error), this);
                    }
                }
            } else {
                EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
                if (!engineConnection.closed) {
                    engineConnection.notifyInflightQos0(true);
                    engineConnection.closed = true;
                    CloseRequest cr = engineConnection.closeRequest;
                    NetworkClosePromiseImpl future = new NetworkClosePromiseImpl(this, cr);
                    engineConnection.channel.close(future);
//                    DisconnectRequest req = new DisconnectRequest(engineConnection.networkConnection);
//                    req.setContext(cr);
//                    NettyNetwork.getInstance().tell(req, this);
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

    private ClientException getClientException(String errMsg) {
      if (errMsg.contains("sasl ") || errMsg.contains("SSL ")) {
        return new com.ibm.mqlight.api.SecurityException(errMsg);
      }

      if (errMsg.contains("_Takeover")) {
          return new ReplacedException(errMsg);
      }

      if (errMsg.contains("_InvalidSourceTimeout")) {
          return new NotPermittedException(errMsg);
      }

      return new NetworkException(errMsg);
    }

    private void processEventDelivery(Event event) {
        final String methodName = "processEventDelivery";
        logger.entry(this, methodName, event);

        EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
        Delivery delivery = event.getDelivery();
        if (event.getLink() instanceof Sender) {
            SendRequest sr = engineConnection.inProgressOutboundDeliveries.remove(delivery);
            Exception exception = null;
            if (delivery.getRemoteState() instanceof Rejected) {
                Rejected rejected = (Rejected)delivery.getRemoteState();
                // If we ever need to check the symbolic error code returned by the server -
                // this is accessible via the getCondition() method - e.g.
                //     rejected.getError().getCondition() => 'MAX_TTL_EXCEEDED'
                String description = rejected.getError().getDescription();
                if (description == null) {
                    exception = new Exception("Message was rejected");
                } else {
                    exception = new Exception(description);
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
            ByteBuf buf = io.netty.buffer.Unpooled.wrappedBuffer(data);

            EngineConnection.SubscriptionData subData = engineConnection.subscriptionData.get(event.getLink().getName());
            subData.unsettled++;
            QOS qos = delivery.remotelySettled() ? QOS.AT_MOST_ONCE : QOS.AT_LEAST_ONCE;
            subData.subscriber.tell(new DeliveryRequest(buf, qos, event.getLink().getName(), delivery, event.getConnection()), this);
        }

        logger.exit(this, methodName);
    }

    private void processEventLinkFlow(Event event) {
        final String methodName = "processEventLinkFlow";
        logger.entry(this, methodName, event);
        logger.exit(this, methodName);
    }

    private void processEventLinkLocalState(Event event) {
        final String methodName = "processEventLinkFlow";
        logger.entry(this, methodName, event);

        Link link = event.getLink();
        logger.data(this, methodName, "LINK_LOCAL {} {} {}", link, link.getLocalState(), link.getRemoteState());

        logger.exit(this, methodName);
    }

    private void processEventLinkRemoteState(Event event) {
        final String methodName = "processEventLinkFlow";
        logger.entry(this, methodName, event);

        Link link = event.getLink();
        logger.data(this, methodName, "LINK_REMOTE {} {} {}", link, link.getLocalState(), link.getRemoteState());

        final Event.Type eventType = event.getType();

        if (link instanceof Receiver) {
            if (eventType == Event.Type.LINK_REMOTE_OPEN) {
                // Receiver link open has been ack'ed by server.
                if (link.getLocalState() == EndpointState.ACTIVE &&
                        link.getRemoteState() == EndpointState.ACTIVE) {
                    EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
                    EngineConnection.SubscriptionData sd = engineConnection.subscriptionData.get(link.getName());
                    sd.subscriber.tell(new SubscribeResponse(engineConnection, link.getName()), this);
                }
            } else if (eventType == Event.Type.LINK_REMOTE_CLOSE
                    || eventType == Event.Type.LINK_REMOTE_DETACH) {
                // Receiver link has been closed by the server.
                if (link.getRemoteState() == EndpointState.CLOSED) {
                    if (link.getLocalState() != EndpointState.CLOSED) {
                        link.close();
                    }
                    link.free();

                    EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
                    EngineConnection.SubscriptionData sd = engineConnection.subscriptionData.remove(link.getName());

                    if (sd == null) {
                      logger.ffdc(this, methodName, FFDCProbeId.PROBE_001, null, this, event);
                        // TODO: throw IllegalStateException?
                    } else {
                        // we assume that getRemoteConnection will be null or empty if there is no error
                        ClientException clientException = null;
                        if (link.getRemoteCondition() != null && link.getRemoteCondition().getCondition() != null) {
                            String errorDescription = link.getRemoteCondition().getDescription();
                            String errMsg = null;
                            if (errorDescription == null) {
                                errMsg = "The server indicated that the destination was unsubscribed due to an error condition, " +
                                         "without providing any further error information.";
                            } else {
                                errMsg = errorDescription;
                            }
                            clientException = getClientException(errMsg);
                        }
                        sd.subscriber.tell(new UnsubscribeResponse(engineConnection, link.getName(), clientException), this);
                    }
                }
            }
        } else if (link instanceof Sender) {
            if (eventType == Event.Type.LINK_REMOTE_CLOSE &&
                    link.getRemoteState() == EndpointState.CLOSED) {
                if (link.getLocalState() != EndpointState.CLOSED) {
                    // TODO: trace an error - as the server has closed our sending link unexpectedly...
                    link.close();
                }
                link.free();
            }
        }

        logger.exit(this, methodName);

    }

    private void processEventSessionLocalState(Event event) {
        final String methodName = "processEventSessionLocalState";
        logger.entry(this, methodName, event);

        // TODO: do we care about this event?

        logger.exit(this, methodName);
    }

    private void processEventSessionRemoteState(Event event) {
        final String methodName = "processEventSessionLocalState";
        logger.entry(this, methodName, event);

        if (event.getSession().getLocalState() == EndpointState.ACTIVE &&
            event.getSession().getRemoteState() == EndpointState.ACTIVE) {
            // First session has opened on the connection
            EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
            OpenRequest req = engineConnection.openRequest;
            engineConnection.openRequest = null;
            engineConnection.requestor.tell(new OpenResponse(req, engineConnection), this);
        }

        // TODO: should reject remote party trying to establish sessions with us

        logger.exit(this, methodName);
    }

    private void processEventTransport(Event event) {
        final String methodName = "processEventTransport";
        logger.entry(this, methodName, event);
        logger.exit(this, methodName);
    }
}
