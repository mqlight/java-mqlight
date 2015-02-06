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
import java.util.LinkedList;

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

    protected static class PendingQos0Response{
        long amount;
        SendResponse response;
        Component component;
        Engine engine;
        protected PendingQos0Response(long amount, SendResponse response, Component component, Engine engine) {
            this.amount = amount;
            this.response = response;
            this.component = component;
            this.engine = engine;
        }
    }

    // An (ordered) list of in-flight qos 0 transfers.  This is used to determine when to invoke
    // the associated callback (as supplied to the send method) based on how much data has been
    // written to the AMQP transport.
    protected final LinkedList<PendingQos0Response> inflightQos0 = new LinkedList<>();

    protected void addInflightQos0(int delta, SendResponse response, Component component, Engine engine) {
        inflightQos0.addLast(new PendingQos0Response(bytesWritten + delta, response, component, engine));
    }

    protected void notifyInflightQos0(boolean purge) {
        while(!inflightQos0.isEmpty()) {
            PendingQos0Response pendingResponse = inflightQos0.getFirst();
            if (purge || (pendingResponse.amount <= bytesWritten)) {
                inflightQos0.removeFirst();
                pendingResponse.component.tell(pendingResponse.response, pendingResponse.engine);
            } else {
                break;
            }
        }
    }

    protected final Transport transport;
    protected final Collector collector;
    protected final NetworkChannel channel;
    protected long deliveryTag = 0;
    protected final HashMap<Delivery, SendRequest> inProgressOutboundDeliveries = new HashMap<>();
    protected final HashMap<String, SubscriptionData> subscriptionData = new HashMap<>();
    protected OpenRequest openRequest = null;
    protected CloseRequest closeRequest = null;
    protected TimerPromiseImpl timerPromise = null;
    protected boolean closed = false;
    protected boolean drained = true;
    protected long bytesWritten = 0;

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

    /**
     * For unit testing.
     */
    public EngineConnection() {
        requestor = null;
        session = null;
        channel = null;
        connection = null;
        collector = null;
        transport = null;
    }
}
