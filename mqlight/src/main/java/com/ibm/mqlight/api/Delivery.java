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

import java.util.Map;

/**
 * This interface is used to represent the delivery of a messages to the MQ Light client.
 * It is sub-classed to represent the delivery of specific types of message data (e.g.
 * binary and textual data).
 */
public interface Delivery {

    /**
     * Possible types of data that can be associated with this delivery.
     * <p>
     * TODO: should these be BINARY and TEXT?
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
     * <p>
     * TODO: this needs to throw an exception if the delivery is not QoS 1
     */
    void confirm();

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

    Map<String, Object> getProperties();
}
