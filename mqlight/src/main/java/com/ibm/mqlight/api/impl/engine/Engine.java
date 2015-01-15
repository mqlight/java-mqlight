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

package com.ibm.mqlight.api.impl.engine;

import java.nio.ByteBuffer;
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
import org.apache.qpid.proton.engine.impl.TransportImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.impl.Message;
import com.ibm.mqlight.api.impl.network.ConnectResponse;
import com.ibm.mqlight.api.impl.network.ConnectionError;
import com.ibm.mqlight.api.impl.network.DataRead;
import com.ibm.mqlight.api.impl.network.DisconnectResponse;
import com.ibm.mqlight.api.impl.network.NetworkCloseFutureImpl;
import com.ibm.mqlight.api.impl.network.NetworkConnectFutureImpl;
import com.ibm.mqlight.api.impl.network.NetworkListenerImpl;
import com.ibm.mqlight.api.impl.network.NetworkWriteFutureImpl;
import com.ibm.mqlight.api.impl.network.WriteResponse;
import com.ibm.mqlight.api.impl.timer.CancelRequest;
import com.ibm.mqlight.api.impl.timer.PopResponse;
import com.ibm.mqlight.api.impl.timer.ScheduleRequest;
import com.ibm.mqlight.api.impl.timer.TimerService;
import com.ibm.mqlight.api.network.Network;
import com.ibm.mqlight.api.network.NetworkConnectFuture;

public class Engine extends Component {
    
    private final Network network;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public Engine(Network network) {
        this.network = network;
    }
    
    @Override
    protected void onReceive(Message message) {
        if (message instanceof OpenRequest) {
            OpenRequest or = (OpenRequest)message;
            //ConnectRequest connectRequest = new ConnectRequest(or.endpoint.getHost(), or.endpoint.getPort());
            //connectRequest.setContext(or);
            //nn.tell(connectRequest, this);
            NetworkListenerImpl listener = new NetworkListenerImpl(this);
            NetworkConnectFuture future = new NetworkConnectFutureImpl(this, or);
            network.connect(or.endpoint.getHost(), or.endpoint.getPort(), listener, future);
        }
        else if (message instanceof ConnectResponse) {
            // Message from network telling us that a connect request has completed...
            ConnectResponse cr = (ConnectResponse)message;
            OpenRequest or = (OpenRequest)cr.context;
            if (cr.cause != null) {
                or.getSender().tell(new OpenResponse(or, cr.cause), this);
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
                    sasl.plain(or.endpoint.getUser(), new String(or.endpoint.getPassword()));
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
            engineConnection.heartbeatInterval = 0;
            if (engineConnection.scheduleRequest != null) {
                ScheduleRequest tmp = engineConnection.scheduleRequest;
                engineConnection.scheduleRequest = null;
                TimerService.getInstance().tell(new CancelRequest(tmp), Component.NOBODY);
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
            
            Delivery d = linkSender.delivery(String.valueOf(engineConnection.deliveryTag++).getBytes());
            
            linkSender.send(sr.data, 0, sr.length);
            if (sr.qos == QOS.AT_MOST_ONCE) {
                d.settle();
            } else {
                engineConnection.inProgressOutboundDeliveries.put(d, sr);
            }
            linkSender.advance();
            engineConnection.drained = false;
            writeToNetwork(engineConnection);
            if (sr.qos == QOS.AT_MOST_ONCE) {
                // TODO: is it possible to be more accurate about this (e.g. when the message has been written?)
                sr.getSender().tell(new SendResponse(sr, null), this);
            }
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
            if (ur.zeroTtl) {
                Target t = (Target)sd.receiver.getTarget();
                t.setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);
                t.setTimeout(new UnsignedInteger(0));
                Source s = (Source)sd.receiver.getSource();
                s.setTimeout(new UnsignedInteger(0));
                s.setExpiryPolicy(TerminusExpiryPolicy.LINK_DETACH);
            }
            sd.receiver.close();
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
                ByteBuffer tail = engineConnection.transport.tail();
                int amount = Math.min(engineConnection.transport.capacity(), dr.buffer.remaining());
                tail.limit(tail.position() + amount);
                tail.put(dr.buffer);
                engineConnection.transport.process();
                process(engineConnection.collector);
            }
            
            // Write any data from Proton to the network.
            writeToNetwork(engineConnection);
        } else if (message instanceof DisconnectResponse) {
            // Message from network telling us that it has completed our disconnect request.
            DisconnectResponse dr = (DisconnectResponse)message;
            CloseRequest cr = (CloseRequest)dr.context;
            cr.connection.dead = true;
            if (cr != null) {
                cr.getSender().tell(new CloseResponse(cr), this);
            }
        } else if (message instanceof ConnectionError) {
            // Message from network telling us that a error has occurred at the TCP/IP level.
            ConnectionError ce = (ConnectionError)message;
            EngineConnection engineConnection = (EngineConnection)ce.channel.getContext();
            if (!engineConnection.dead) {
                engineConnection.heartbeatInterval = 0;
                if (engineConnection.scheduleRequest != null) {
                    ScheduleRequest tmp = engineConnection.scheduleRequest;
                    engineConnection.scheduleRequest = null;
                    TimerService.getInstance().tell(new CancelRequest(tmp), Component.NOBODY);
                }
                engineConnection.dead = true;
                engineConnection.transport.close_tail();
                engineConnection.requestor.tell(new DisconnectNotification(engineConnection, ce.cause.getClass().toString(), ce.cause.getMessage()), this);
            }
        } else if (message instanceof PopResponse) {
            PopResponse pr = (PopResponse)message;
            EngineConnection engineConnection = (EngineConnection)pr.request.getContext();
            if (engineConnection.heartbeatInterval > 0) {
                ScheduleRequest sr = new ScheduleRequest(engineConnection.heartbeatInterval);
                sr.setContext(engineConnection);
                engineConnection.scheduleRequest = sr;
                TimerService.getInstance().tell(sr, this);
                
                ((TransportImpl)engineConnection.transport).writeEmptyFrame();
                writeToNetwork(engineConnection);
            }
        }
    }

    // Drains any pending data from a Proton transport object onto the network
    private void writeToNetwork(EngineConnection engineConnection) {
        if (engineConnection.transport.pending() > 0) {
            ByteBuffer head = engineConnection.transport.head();
            int amount = head.remaining();
            ByteBuffer tmp = ByteBuffer.allocate(amount);       // TODO: we could avoid allocating this if we were a bit smarter
            tmp.put(head);                                      //       about when we popped the transport...
            tmp.flip();
            //ByteBuf buf = Unpooled.wrappedBuffer(head);
            engineConnection.transport.pop(amount);
            engineConnection.channel.write(tmp, new NetworkWriteFutureImpl(this, engineConnection));
            //nn.tell(new WriteRequest(connection, buf), this);
        }
    }
    
    // TODO: Proton 0.8 provides an Event.dispatch() method that could be used to replace this code...
    private void process(Collector collector) {
  
        while (collector.peek() != null) {
            Event event = collector.peek();
            logger.debug("Processing event: {}", event.getType());
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
                throw new IllegalStateException("Unknown event type: " + event.getType());
            }
            collector.pop();
        }
    }
    
    private void processEventConnectionLocalState(Event event) {
        logger.debug("CONNECTION_LOCAL_STATE: {}", event.getConnection());
        // TODO: do we care about this event?
    }
    
    private void processEventConnectionRemoteState(Event event) {
        logger.debug("CONNECTION_REMOTE_STATE: ", event.getConnection());
        
        if (event.getConnection().getRemoteState() == EndpointState.CLOSED) {
            if (event.getConnection().getLocalState() != EndpointState.CLOSED) {
                EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
                engineConnection.heartbeatInterval = 0;
                if (engineConnection.scheduleRequest != null) {
                    ScheduleRequest tmp = engineConnection.scheduleRequest;
                    engineConnection.scheduleRequest = null;
                    TimerService.getInstance().tell(new CancelRequest(tmp), Component.NOBODY);
                }
                if (engineConnection.openRequest != null) {
                    OpenRequest req = engineConnection.openRequest;
                    engineConnection.openRequest = null;
                    if (!engineConnection.dead) {
                        engineConnection.dead = true;
                        //DisconnectRequest dr = new DisconnectRequest(engineConnection.networkConnection);
                        //NettyNetwork.getInstance().tell(dr, Component.NOBODY);
                        engineConnection.channel.close(null);
                        String errorDescription = event.getConnection().getRemoteCondition().getDescription();
                        req.getSender().tell(new OpenResponse(req, errorDescription == null ? new Exception() : new Exception(errorDescription)), this);
                    }
                } else {    // TODO: should we also special case closeRequest in progress??
                    if (!engineConnection.dead) {
                        engineConnection.dead = true;
                        engineConnection.channel.close(null);
                        //DisconnectRequest req = new DisconnectRequest(engineConnection.networkConnection);
                        //NettyNetwork.getInstance().tell(req, Component.NOBODY);
                        String condition = event.getConnection().getRemoteCondition().getCondition().toString();
                        if (condition == null) condition = "";
                        String description = event.getConnection().getRemoteCondition().getDescription();
                        if (description == null) description = "";
                        engineConnection.requestor.tell(new DisconnectNotification(engineConnection, condition, description), this);
                    }
                }
            } else {
                EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
                if (!engineConnection.dead) {
                    engineConnection.dead = true;
                    CloseRequest cr = engineConnection.closeRequest;
                    NetworkCloseFutureImpl future = new NetworkCloseFutureImpl(this, cr);
                    engineConnection.channel.close(future);
//                    DisconnectRequest req = new DisconnectRequest(engineConnection.networkConnection);
//                    req.setContext(cr);
//                    NettyNetwork.getInstance().tell(req, this);
                }
            }
        } else if (event.getConnection().getRemoteState() == EndpointState.ACTIVE) {
            UnsignedInteger ui = ((org.apache.qpid.proton.engine.impl.ConnectionImpl)event.getConnection()).getRemoteIdleTimeOut();
            if (ui != null) {
                EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
                engineConnection.heartbeatInterval = ui.longValue() / 2;
                engineConnection.scheduleRequest = new ScheduleRequest(engineConnection.heartbeatInterval);
                engineConnection.scheduleRequest.setContext(engineConnection);
                TimerService.getInstance().tell(engineConnection.scheduleRequest, this);
            }
        }
    }
    
    private void processEventDelivery(Event event) {
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
           
            EngineConnection.SubscriptionData subData = engineConnection.subscriptionData.get(event.getLink().getName());
            subData.unsettled++;
            QOS qos = delivery.remotelySettled() ? QOS.AT_MOST_ONCE : QOS.AT_LEAST_ONCE;
            subData.subscriber.tell(new DeliveryRequest(data, qos, event.getLink().getName(), delivery, event.getConnection()), this);
        }
    }
    
    private void processEventLinkFlow(Event event) {
        
    }
    
    private void processEventLinkLocalState(Event event) {
        logger.debug("LINK_LOCAL ", event.getLink());
    }
    
    private void processEventLinkRemoteState(Event event) {
        Link link = event.getLink();
        logger.debug("LINK_REMOTE ", link);
        if (link instanceof Receiver) {
            if (link.getLocalState() == EndpointState.ACTIVE &&
                link.getRemoteState() == EndpointState.ACTIVE) {
                // Receiver link open has been ack'ed by server.
                EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
                EngineConnection.SubscriptionData sd = engineConnection.subscriptionData.get(link.getName());
                sd.subscriber.tell(new SubscribeResponse(engineConnection, link.getName()), this);
            } else if (link.getRemoteState() == EndpointState.CLOSED) {
                if (link.getLocalState() != EndpointState.CLOSED) {
                    link.close();
                }
                EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
                EngineConnection.SubscriptionData sd = engineConnection.subscriptionData.remove(link.getName());
                
                // TODO: can we assume that getRemoteConnection will be null if there is no error?
                sd.subscriber.tell(new UnsubscribeResponse(engineConnection, link.getName(), link.getRemoteCondition() != null), this);
            }
        } else if (link instanceof Sender) {
            if (link.getRemoteState() == EndpointState.CLOSED) {
                if (link.getLocalState() != EndpointState.CLOSED) {
                    // TODO: trace an error - as the server has closed our sending link unexpectedly...
                    link.close();
                }
            }
        }
    }
    
    private void processEventSessionLocalState(Event event) {
        logger.debug("processEventSessionLocalState ", event.getSession());
        
        // TODO: do we care about this event?
    }
    
    private void processEventSessionRemoteState(Event event) {
        logger.debug("processEventSessionRemoteState", event.getSession());
        if (event.getSession().getLocalState() == EndpointState.ACTIVE &&
            event.getSession().getRemoteState() == EndpointState.ACTIVE) {
            // First session has opened on the connection
            EngineConnection engineConnection = (EngineConnection)event.getConnection().getContext();
            OpenRequest req = engineConnection.openRequest;
            engineConnection.openRequest = null;
            engineConnection.requestor.tell(new OpenResponse(req, engineConnection), this);
        }
        
        // TODO: should reject remote party trying to establish sessions with us
    }
    
    private void processEventTransport(Event event) {
        logger.debug("processEventTransport", event.getTransport());
    }
}
