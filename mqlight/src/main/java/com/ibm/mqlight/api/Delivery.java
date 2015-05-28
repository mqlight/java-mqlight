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

import java.util.Map;

/**
 * This interface is used to represent the delivery of a messages to the MQ Light client.
 * It is sub-classed to represent the delivery of specific types of message data (e.g.
 * binary and textual data).
 */
public interface Delivery {

    /**
     * Possible types of data that can be associated with this delivery.
     */
    enum Type {
        BYTES, STRING, MALFORMED, JSON
    };

    /**
     * @return the type of the delivery.  This is provided to simplify casting this
     *         interface to the more specific <code>BytesDelivery</code> and
     *         <code>StringDelivery</code> types.
     */
    Type getType();

    /**
     * Confirms receipt of this delivery.
     *
     * @throws StateException if confirmation is not applicable or required. This is the case if any of the following
     *                        conditions are true:
     *                        <ul>
     *                        <li>the quality of service is {@link QOS#AT_MOST_ONCE}.</li>
     *                        <li>auto confirmation of delivery is enabled.</li>
     *                        <li>delivery has already been confirmed.</li>
     *                        <li>the network state is such that delivery cannot be confirmed.</li>
     *                        </ul>
     */
    void confirm() throws StateException;

    /**
     * @return the quality of service used to receive the messaging being delivered.
     */
    QOS getQOS();

    /**
     * @return the share name associated with the destination from which the message
     *         was received.  A value of <code>null</code> is returned if the destination
     *         was not subscribed to with a share value.
     */
    String getShare();

    /**
     * @return the topic to which the message, being delivered, was originally published.
     */
    String getTopic();

    /**
     * @return the topic pattern that was used to subscribe to the destination from
     *         which this message was delivered.
     */
    String getTopicPattern();

    /**
     * @return the remaining time-to-live time, for the message being delivered, in
     *         milliseconds.
     */
    long getTtl();

    /**
     * @return a {@link Map} of properties associated with the message being delivered. Keys will be non-null and values
     *         will be one of the following types: <code>null</code>, <code>Boolean</code>, <code>Byte</code>,
     *         <code>Short</code>, <code>Integer</code>, <code>Long</code>, <code>Float</code>, <code>Double</code>,
     *         <code>byte[]</code>, and <code>String</code>.
     */
    Map<String, Object> getProperties();
}
