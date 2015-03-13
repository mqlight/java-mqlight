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
package com.ibm.mqlight.api;

import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

/**
 * A set of options that can be used to configure the behaviour of the
 * {@link NonBlockingClient#subscribe(String, SubscribeOptions, DestinationListener, CompletionListener, Object)}
 * method.  This class is setup to be used in a fluent style.  For example:
 * <pre>
 * SubscribeOptions opts = SubscribeOptions.builder().setQos(AT_LEAST_ONCE).setAutoConfirm(false).build();
 * </pre>
 */
public class SubscribeOptions {

    private static final Logger logger = LoggerFactory.getLogger(SubscribeOptions.class);
  
    private final boolean autoConfirm;
    private final int credit;
    private final QOS qos;
    private final String shareName;
    private final long ttl;

    private SubscribeOptions(boolean autoConfirm, int credit, QOS qos, String shareName, long ttl) {
        final String methodName = "<init>";
        logger.entry(this, methodName, autoConfirm, credit, qos, shareName, ttl);
      
        this.autoConfirm = autoConfirm;
        this.credit = credit;
        this.qos = qos;
        this.shareName = shareName;
        this.ttl = ttl;
        
        logger.exit(this, methodName);
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

    public long getTtl() {
        return ttl;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" [autoConfirm=")
          .append(autoConfirm)
          .append(", credit=")
          .append(credit)
          .append(", qos=")
          .append(qos)
          .append(", share=")
          .append(shareName)
          .append(", ttl=")
          .append(ttl)
          .append("]");
        return sb.toString();
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
         * @throws IllegalArgumentException if an invalid <code>credit</code> value is specified.
         */
        public SubscribeOptionsBuilder setCredit(int credit) throws IllegalArgumentException {
            final String methodName = "setCredit";
            logger.entry(this, methodName, credit);
          
            if (credit < 0) {
              final IllegalArgumentException exception = new IllegalArgumentException("Credit value '" + credit + "' is invalid, must be >= 0");
              logger.throwing(this,  methodName, exception);
              throw exception;
            }
            this.credit = credit;
            
            logger.exit(this, methodName, this);
            
            return this;
        }

        /**
         * The quality of service to use for delivering messages to the subscription.  The
         * default, if this option is not set, is: 'at most once'.
         * @param qos The required quality of service. Cannot be null.
         * @return the instance of <code>SubscribeOptionsBuilder</code> that this method was invoked on.
         * @throws IllegalArgumentException if an invalid <code>qos</code> value is specified.
         */
        public SubscribeOptionsBuilder setQos(QOS qos) throws IllegalArgumentException {
            final String methodName = "setQos";
            logger.entry(this, methodName, qos);
          
            if (qos == null) {
              final IllegalArgumentException exception = new IllegalArgumentException("QOS value cannot be null");
              logger.throwing(this,  methodName, exception);
              throw exception;
            }
            this.qos = qos;
            
            logger.exit(this, methodName, this);
            
            return this;
        }

        /**
         * The share argument supplied used when subscribing to a destination.
         * @param shareName the share argument used to subscribe to a destination.  The
         *                  default is <code>null</code> which is interpreted as "do not subscribe
         *                  to a shared destination - the destination is private to this client".
         *                  When specified, the share name must not contain a colon (:) character. 
         * @return the instance of <code>SubscribeOptionsBuilder</code> that this method was invoked on.
         * @throws IllegalArgumentException if an invalid <code>shareName</code> value is specified.
         */
        public SubscribeOptionsBuilder setShare(String shareName) throws IllegalArgumentException {
            final String methodName = "setShare";
            logger.entry(this, methodName, shareName);
          
            if (shareName != null && shareName.contains(":")) {
              final IllegalArgumentException exception = new IllegalArgumentException("Share name cannot contain a colon (:) character");
              logger.throwing(this,  methodName, exception);
              throw exception;
            }
            this.shareName = shareName;
            
            logger.exit(this, methodName, this);
            
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
         *            This must be a positive value, and a maximum of 4294967295 (0xFFFFFFFF)
         * @return the instance of <code>SubscribeOptionsBuilder</code> that this method was invoked on.
         * @throws IllegalArgumentException if an invalid <code>ttl</code> value is specified.
         */
        public SubscribeOptionsBuilder setTtl(long ttl) throws IllegalArgumentException {
            final String methodName = "setTtl";
            logger.entry(this, methodName, ttl);
            if (ttl < 0 || ttl > 4294967295L) {
              final IllegalArgumentException exception = new IllegalArgumentException("ttl value " + ttl + " is invalid, must be an unsigned 32-bit value");
              logger.throwing(this,  methodName, exception);
              throw exception;
            }
            this.ttl = ttl;
            
            logger.exit(this, methodName, this);
            
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
