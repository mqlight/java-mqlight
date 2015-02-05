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
 * A set of options that can be used to configure the behaviour of the <code>NonBlockingClient</code>
 * {@link NonBlockingClient#send(String, java.nio.ByteBuffer, Map, SendOptions, CompletionListener, Object)} and
 * {@link NonBlockingClient#send(String, String, Map, SendOptions, CompletionListener, Object)} methods. For example:
 * <pre>
 * SendOptions opts = SendOptions.builder().setQos(QOS.AT_LEAST_ONCE).setTtl(5000).build();
 * client.send("/tadpoles", "Hello baby frogs!", opts, listener, null);
 * </pre>
 */
public class SendOptions {

    private final QOS qos;
    private final long ttl;

    private SendOptions(QOS qos, long ttl) {
        this.qos = qos;
        this.ttl = ttl;
    }

    public final QOS getQos() {
        return qos;
    }

    public final long getTtl() {
        return ttl;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" [qos=")
          .append(qos)
          .append(", ttl=")
          .append(ttl)
          .append("]");
        return sb.toString();
    }

    /**
     * @return a new instance of <code>SendOptionsBuilder</code> that can be used to build
     *         (immutable) instances of <code>SendOptions</code>.
     */
    public static SendOptionsBuilder builder() {
        return new SendOptionsBuilder();
    }

    /**
     * A builder for <code>SendOptions</code> objects.
     */
    public static class SendOptionsBuilder {
        private QOS qos = QOS.AT_MOST_ONCE;
        private long ttl = 0;

        private SendOptionsBuilder() {}

        /**
         * Sets the quality of service that will be used to send messages to the MQ Light
         * server.
         * @param qos
         * @return the instance of <code>SendOptionsBuilder</code> that this method was
         *         called on.
         */
        public SendOptionsBuilder setQos(QOS qos) {
            if (qos == null) throw new IllegalArgumentException("qos argument cannot be null");
            this.qos = qos;
            return this;
        }

        /**
         * Sets the time to live that will be used for messages sent to the MQ Light server.
         * @param ttl time to live in milliseconds.
         * @return the instance of <code>SendOptionsBuilder</code> that this method was
         *         called on.
         * @throws IllegalArgumentException if an invalid <code>ttl</code> value is specified.
         *         Valid <code>ttl</code> values must be an unsigned non-zero integer number.
         */
        public SendOptionsBuilder setTtl(long ttl) throws IllegalArgumentException {
            if (ttl < 1 || ttl > 4294967295L) {
                throw new IllegalArgumentException("ttl value '" + ttl + "' is invalid, must be an unsigned non-zero integer number");
            }
            this.ttl = ttl;
            return this;
        }

        /**
         * @return an instance of SendOptions based on the current settings of
         *         this builder.
         */
        public SendOptions build() {
            return new SendOptions(qos, ttl);
        }
    }


}
