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

import java.util.HashMap;

import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.Transport;

import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.impl.timer.TimerPromiseImpl;
import com.ibm.mqlight.api.network.NetworkChannel;

public class EngineConnection {

    protected final Connection connection;
    protected final Session session;
    protected final Component requestor;    // Used for sending "you've been disconnected notifications
    
    //protected final EngineConnection engineConnection;
    protected final Transport transport;
    protected final Collector collector;
    protected final NetworkChannel channel;
    protected long deliveryTag = 0;
    protected final HashMap<Delivery, SendRequest> inProgressOutboundDeliveries = new HashMap<>();
    protected final HashMap<String, SubscriptionData> subscriptionData = new HashMap<>();
    protected OpenRequest openRequest = null;
    protected CloseRequest closeRequest = null;
    protected TimerPromiseImpl timerPromise = null;
    protected long heartbeatInterval = 0;
    protected boolean dead = false; // TODO: better name...
    protected boolean drained = true;
    
    protected static class SubscriptionData {
        protected final Component subscriber;
        protected final int maxLinkCredit;
        protected final Receiver receiver;
        protected int unsettled;
        protected int settled;
        protected SubscriptionData(Component subscriber, int maxLinkCredit, Receiver receiver) {
            this.subscriber = subscriber;
            this.maxLinkCredit = maxLinkCredit;
            this.receiver = receiver;
            this.unsettled = 0;
            this.settled = 0;
        }
    }
    
    protected EngineConnection(Connection connection, Session session, Component requestor, Transport transport, Collector collector, NetworkChannel channel) {
        this.connection = connection;
        this.session = session;
        this.requestor = requestor;
        this.transport = transport;
        this.collector = collector;
        this.channel = channel;
    }
}
