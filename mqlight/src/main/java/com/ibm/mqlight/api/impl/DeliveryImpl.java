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
package com.ibm.mqlight.api.impl;

import java.util.Map;

import com.ibm.mqlight.api.Delivery;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.StateException;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

public abstract class DeliveryImpl implements Delivery {

    private static final Logger logger = LoggerFactory.getLogger(DeliveryImpl.class);

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
        final String methodName = "<init>";
        logger.entry(this, methodName, client, qos, share, topic, topicPattern, ttl, properties, deliveryRequest);

        this.client = client;
        this.qos = qos;
        this.share = share;
        this.topic = topic;
        this.topicPattern = topicPattern;
        this.ttl = ttl;
        this.properties = properties;
        this.deliveryRequest = deliveryRequest;

        logger.exit(this, methodName);
    }

    @Override
    public abstract Type getType();

    @Override
    public void confirm() throws StateException {
        final String methodName = "confirm";
        logger.entry(this, methodName);

        if (deliveryRequest == null) {
            if (qos == QOS.AT_MOST_ONCE) {
                throw new StateException("Confirming the receipt of delivery is applicable only when 'at least once' quality of service has been requested");
            } else {
                throw new StateException("Subscription has autoConfirm option set to true");
            }
        } else {
            if (confirmed) {
                throw new StateException("Delivery has already been confirmed");
            } else if (!client.doDelivery(deliveryRequest)) {
                throw new StateException("Cannot confirm delivery because of either an interruption to the network "
                        + "connection to the MQ Light server, or because the client is no longer subscribed to the "
                        + "destination that the message was received from");
            } else {
                confirmed = true;
            }
        }

        logger.exit(this, methodName);
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
