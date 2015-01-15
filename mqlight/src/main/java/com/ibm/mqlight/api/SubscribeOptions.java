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

package com.ibm.mqlight.api;

/**
 * A set of options that can be used to configure the behaviour of the 
 * {@link NonBlockingClient#subscribe(String, SubscribeOptions, DestinationListener, CompletionListener, Object)}
 * method.  This class is setup to be used in a fluent style.  For example:
 * <pre>
 * SubscribeOptions opts = SubscribeOptions.builder().setQos(AT_LEAST_ONCE).setAutoConfirm(false).build();
 * </pre>
 */
public class SubscribeOptions {
    
    private final boolean autoConfirm;
    private final int credit;
    private final QOS qos;
    private final String shareName;
    private final long ttl;
    
    private SubscribeOptions(boolean autoConfirm, int credit, QOS qos, String shareName, long ttl) {
        this.autoConfirm = autoConfirm;
        this.credit = credit;
        this.qos = qos;
        this.shareName = shareName;
        this.ttl = ttl;
    }
    
    public boolean getAutoConfirm() {
        return autoConfirm;
    }
    
    public int getCredit() {
        return credit;
    }
    
    public QOS getQOS() {
        return qos;
    }
    
    public String getShareName() {
        return shareName;
    }
    
    // TODO: should this be an int?
    public long getTtl() {
        return ttl;
    }
    
    /**
     * @return a builder that can be used to create an immutable instance of <code>SubscribeOptions</code>.
     */
    public static SubscribeOptionsBuilder builder()  {
        return new SubscribeOptionsBuilder();
    }
    
    /**
     * A builder that creates instances of <code>SubscribeOptions</code>.
     */
    public static class SubscribeOptionsBuilder {
        
        private boolean autoConfirm = true;
        private int credit = 1024;
        private QOS qos = QOS.AT_MOST_ONCE;
        private String shareName = null;
        private long ttl = 0;
        
        private SubscribeOptionsBuilder() {}
        
        /**
         * Determines whether the client automatically confirms the receipt of
         * 'at least once' quality of service messages.
         * @param autoConfirm  When set to <code>true</code> (the default) the client will automatically 
         *                     confirm delivery of messages when after calling the appropriate
         *                     {@code DestinationListener#onMessage(NonBlockingClient, Object, Delivery)}
         *                     or {@code DestinationListener#onMalformed(NonBlockingClient, Object, BytesDelivery, com.ibm.mqlight.api.DestinationListener.MalformedReason)}
         *                     method registered for the destination that the message was received from.
         *                     When set to <code>false</code>, the application using the client is
         *                     responsible for confirming the delivery of messages using the 
         *                     {@code Delivery#confirm()} method).  <code>autoConfirm</code> is only applicable 
         *                     to 'at least once' quality of service (see {@code #setQos(QOS)})
         * @return the instance of <code>SubscribeOptionsBuilder</code> that this method was invoked on.
         */
        public SubscribeOptionsBuilder setAutoConfirm(boolean autoConfirm) {
            this.autoConfirm = autoConfirm;
            return this;
        }
        
        /**
         * Sets the maximum number of unconfirmed messages a client can have before the server will stop
         * sending new messages to the client and require that it confirms some of the outstanding message
         * deliveries in order to receive more messages.
         * @param credit the credit value which must be >= 0.  The default if this is not specified is 1024.
         * @return the instance of <code>SubscribeOptionsBuilder</code> that this method was invoked on.
         */
        public SubscribeOptionsBuilder setCredit(int credit) {
            this.credit = credit;
            return this;
        }
        
        /**
         * The quality of service to use for delivering messages to the subscription.  The
         * default, if this option is not set, is: 'at most once'.
         * @param qos
         * @return the instance of <code>SubscribeOptionsBuilder</code> that this method was invoked on.
         */
        public SubscribeOptionsBuilder setQos(QOS qos) {
            this.qos = qos;
            return this;
        }
        
        /**
         * The share argument supplied used when subscribing to a destination.
         * @param shareName the share argument used to subscribe to a destination.  The
         *                  default is <code>null</code> which is interpreted as "do not subscribe
         *                  to a shared destination - the destination is private to this client".
         * @return the instance of <code>SubscribeOptionsBuilder</code> that this method was invoked on.
         */
        public SubscribeOptionsBuilder setShare(String shareName) {
            this.shareName = shareName;
            return this;
        }
        
        /**
         * A time-to-live value, in milliseconds, that is applied to the destination that the client
         * is subscribed to. This value will replace any previous value, if the destination already
         * exists. Time to live starts counting down when there are no instances of a client subscribed
         * to a destination. It is reset each time a new instance of the client subscribes to the
         * destination. If time to live counts down to zero then MQ Light will delete the destination
         * by discarding any messages held at the destination and not accruing any new messages.
         * @param ttl a time to live value in milliseconds, the default being 0 - meaning the destination
         *            will be deleted as soon as there are no clients subscribed to it.
         * @return the instance of <code>SubscribeOptionsBuilder</code> that this method was invoked on.
         */
        public SubscribeOptionsBuilder setTtl(long ttl) {
            this.ttl = ttl;
            return this;
        }
        
        /**
         * @return an instance of SubscribeOptions based on the current settings of
         *         this builder.
         */
        public SubscribeOptions build() {
            return new SubscribeOptions(autoConfirm, credit, qos, shareName, ttl);
        }
    }
}
