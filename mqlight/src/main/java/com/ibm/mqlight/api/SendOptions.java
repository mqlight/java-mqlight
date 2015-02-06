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
