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

package com.ibm.mqlight.api.impl;

import java.util.Map;

import com.ibm.mqlight.api.Delivery;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.StateException;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;

public abstract class DeliveryImpl implements Delivery {

    private final NonBlockingClientImpl client;
    private final QOS qos;
    private final String share;
    private final String topic;
    private final String topicPattern;
    private final long ttl;
    private final Map<String, Object> properties;
    private final DeliveryRequest deliveryRequest;
    private boolean confirmed = false;

    protected DeliveryImpl(NonBlockingClientImpl client, QOS qos, String share, String topic, String topicPattern, long ttl, Map<String, Object> properties, DeliveryRequest deliveryRequest) {
        this.client = client;
        this.qos = qos;
        this.share = share;
        this.topic = topic;
        this.topicPattern = topicPattern;
        this.ttl = ttl;
        this.properties = properties;
        this.deliveryRequest = deliveryRequest;
    }

    @Override
    public abstract Type getType();

    @Override
    public void confirm() {
        if (deliveryRequest == null) {
            throw new StateException("Subscription has autoConfirm option set to true");
        } else {
            if (confirmed) {
                throw new StateException("Delivery has already been confirmed");
            } else if (!client.doDelivery(deliveryRequest)) {
                throw new StateException("Cannot confirm delivery because of an interruption to the network connection to the MQ Light server");
            } else {
                confirmed = true;
            }
        }
    }

    @Override
    public QOS getQOS() {
        return qos;
    }

    @Override
    public String getShare() {
        return share;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public String getTopicPattern() {
        return topicPattern;
    }

    @Override
    public long getTtl() {
        return ttl;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }
}
