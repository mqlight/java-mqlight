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

import java.net.URI;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.codec.DecodeException;

import com.google.gson.GsonBuilder;
import com.ibm.mqlight.api.Delivery;
import com.ibm.mqlight.api.DestinationListener;
import com.ibm.mqlight.api.MalformedDelivery;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.callback.CallbackService;
import com.ibm.mqlight.api.impl.callback.CallbackPromiseImpl;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;
import io.netty.buffer.ByteBuf;

class DestinationListenerWrapper<T> {

    private static final Logger logger = LoggerFactory.getLogger(DestinationListenerWrapper.class);

    private final NonBlockingClientImpl client;
    private final GsonBuilder gsonBuilder;
    private final DestinationListener<T> listener;
    private final T context;

    private static final Symbol malformedConditionSymbol = Symbol.getSymbol("x-opt-message-malformed-condition");
    private static final Symbol malformedDescriptionSymbol = Symbol.getSymbol("x-opt-message-malformed-description");
    private static final Symbol malformedMQMDFormatSymbol = Symbol.getSymbol("x-opt-message-malformed-MQMD.Format");
    private static final Symbol malformedMQMDCCSIDSymbol = Symbol.getSymbol("x-opt-message-malformed-MQMD.CodedCharSetId");

    protected DestinationListenerWrapper(NonBlockingClientImpl client, GsonBuilder gsonBuilder, DestinationListener<T> listener, T context) {
        final String methodName = "<init>";
        logger.entry(this, methodName, client, gsonBuilder, listener, context);

        this.client = client;
        this.gsonBuilder = gsonBuilder;
        this.listener = listener;
        this.context = context;

        logger.exit(this, methodName);
    }

    protected void onUnsubscribed(final CallbackService callbackService, final String topicPattern, final String share, final Exception error) {
        final String methodName = "onUnsubscribed";
        logger.entry(this, methodName, callbackService, topicPattern, share, error);

        if (listener != null) {
            callbackService.run(new Runnable() {
                @Override
                public void run() {
                    listener.onUnsubscribed(client, context, topicPattern, share, error);
                }
            }, client, new CallbackPromiseImpl(client, true));
        }

        logger.exit(this, methodName);
    }

    protected void onDelivery(final CallbackService callbackService, final DeliveryRequest deliveryRequest, final QOS qos, final boolean autoConfirm) {

        final String methodName = "onDelivery";
        logger.entry(this, methodName, callbackService, deliveryRequest, qos, autoConfirm);

        callbackService.run(new Runnable() {
            @Override
            public void run() {
                final String methodName = "run";
                logger.entry(this, methodName);

                MalformedDelivery.MalformedReason malformedReason = null;
                String malformedDescription = null;
                String malformedMQMDFormat = null;
                int malformedMQMDCCSID = 0;

                byte[] payloadBytes = null;
                String payloadString = null;
                boolean payloadIsJson = false;

                org.apache.qpid.proton.message.Message msg = Proton.message();
                try {
                    msg.decode(deliveryRequest.buf.nioBuffer());
                } catch(BufferOverflowException | BufferUnderflowException | DecodeException e) {
                    malformedReason = MalformedDelivery.MalformedReason.PAYLOADNOTAMQP;
                    malformedDescription = "The message could not be decoded because the message data is not a valid AMQP message";

                    payloadBytes = copyWrappyByteBuf(deliveryRequest.buf);
                }

                Map<String, Object> properties = new HashMap<String, Object>();
                if (malformedReason == null) {
                    Object msgBodyValue = ((AmqpValue)msg.getBody()).getValue();
                    if (msgBodyValue instanceof Binary) {
                        Binary binaryValue = (Binary)msgBodyValue;
                        if ((binaryValue.getArrayOffset() == 0) && (binaryValue.getArray().length == binaryValue.getLength())) {
                            payloadBytes = binaryValue.getArray();
                        } else {
                            payloadBytes = new byte[binaryValue.getLength()];
                            System.arraycopy(binaryValue.getArray(), binaryValue.getArrayOffset(), payloadBytes, 0, binaryValue.getLength());
                        }
                    } else if (msgBodyValue instanceof String) {
                        payloadString = (String)msgBodyValue;
                        payloadIsJson = "application/json".equalsIgnoreCase(msg.getContentType());
                    } else {
                        malformedReason = MalformedDelivery.MalformedReason.FORMATNOMAPPING;
                        malformedDescription = "The message payload uses an AMQP format that the MQ Light client cannot process";

                        payloadBytes = copyWrappyByteBuf(deliveryRequest.buf);
                    }

                    if ((msg.getApplicationProperties() != null) && (msg.getApplicationProperties().getValue() != null)) {
                        Map<?, ?> msgMap = msg.getApplicationProperties().getValue();
                        for (Map.Entry<?, ?> entry : msgMap.entrySet()) {
                            if (entry.getKey() instanceof String) {
                                Object value = entry.getValue();
                                if (value == null) {
                                    properties.put((String)entry.getKey(), null);
                                } else if (value instanceof Binary) {
                                    properties.put((String)entry.getKey(), ((Binary)value).getArray());
                                } else {
                                    for (int i = 0; i < NonBlockingClientImpl.validPropertyValueTypes.length; ++i) {
                                        if (NonBlockingClientImpl.validPropertyValueTypes[i].isAssignableFrom(value.getClass())) {
                                            properties.put((String)entry.getKey(), value);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // all done parsing the delivery request data, release the buffer
                deliveryRequest.buf.release();

                String parts[] = new SubscriptionTopic(deliveryRequest.topicPattern).split();
                String shareName = parts[1];
                String topicPattern = parts[0];
                long ttl = 0;
                String topic = "";
                if (malformedReason == null) {
                    try {
                        topic = URI.create(msg.getAddress()).getPath();
                    } catch(IllegalArgumentException e) {
                    }
                    if (topic == null) topic = "";
                    else if (topic.startsWith("/")) topic = topic.substring(1);
                    ttl = msg.getTtl();

                    if (msg.getDeliveryAnnotations() != null) {
                        Map<Symbol, Object> annotations = msg.getDeliveryAnnotations().getValue();
                        String condition = null;
                        if (annotations.containsKey(malformedConditionSymbol) &&
                            annotations.get(malformedConditionSymbol) instanceof Symbol) {
                            condition = ((Symbol)annotations.get(malformedConditionSymbol)).toString();
                            if (condition.equals("FORMATNOMAPPING")) {
                                malformedReason = MalformedDelivery.MalformedReason.FORMATNOMAPPING;
                            } else if (condition.equals("JMSNOMAPPING")) {
                                malformedReason = MalformedDelivery.MalformedReason.JMSNOMAPPING;
                            } else if (condition.equals("PAYLOADENCODING")) {
                                malformedReason = MalformedDelivery.MalformedReason.PAYLOADENCODING;
                            } else if (condition.equals("PAYLOADNOTAMQP")) {
                                malformedReason = MalformedDelivery.MalformedReason.PAYLOADNOTAMQP;
                            }

                            if (malformedReason != null &&
                                annotations.containsKey(malformedDescriptionSymbol) &&
                                annotations.get(malformedDescriptionSymbol) instanceof String) {
                                malformedDescription = (String)annotations.get(malformedDescriptionSymbol);

                                if (annotations.containsKey(malformedMQMDFormatSymbol) &&
                                    annotations.get(malformedMQMDFormatSymbol) instanceof String) {
                                    malformedMQMDFormat = (String)annotations.get(malformedMQMDFormatSymbol);
                                }

                                if (annotations.containsKey(malformedMQMDCCSIDSymbol) &&
                                    annotations.get(malformedMQMDCCSIDSymbol) instanceof Integer) {
                                    malformedMQMDCCSID = (Integer)annotations.get(malformedMQMDCCSIDSymbol);
                                }
                            }
                        }
                    }
                }

                if (payloadBytes != null) {
                    if (malformedReason == null) {
                        BytesDeliveryImpl delivery = new BytesDeliveryImpl(client, qos, shareName, topic, topicPattern, ttl, ByteBuffer.wrap(payloadBytes), properties, autoConfirm ? null : deliveryRequest);
                        listener.onMessage(client, context, delivery);
                    } else {
                        MalformedDeliveryImpl delivery = new MalformedDeliveryImpl(client, qos, shareName, topic, topicPattern, ttl, ByteBuffer.wrap(payloadBytes),
                                properties, autoConfirm ? null : deliveryRequest, malformedReason, malformedDescription, malformedMQMDFormat, malformedMQMDCCSID);
                        listener.onMalformed(client, context, delivery);
                    }
                } else {
                    if (malformedReason == null) {
                        Delivery delivery;
                        if (payloadIsJson) {
                            delivery = new JsonDeliveryImpl(client, qos, shareName, topic, topicPattern, ttl, payloadString, gsonBuilder, properties, autoConfirm ? null : deliveryRequest);
                        } else {
                            delivery = new StringDeliveryImpl(client, qos, shareName, topic, topicPattern, ttl, payloadString, properties, autoConfirm ? null : deliveryRequest);
                        }
                        listener.onMessage(client, context, delivery);
                    } else {
                        MalformedDeliveryImpl delivery = new MalformedDeliveryImpl(client, qos, shareName, topic, topicPattern, ttl, ByteBuffer.wrap(payloadString.getBytes(Charset.forName("UTF-8"))),
                                properties, autoConfirm ? null : deliveryRequest, malformedReason, malformedDescription, malformedMQMDFormat, malformedMQMDCCSID);
                        listener.onMalformed(client, context, delivery);
                    }
                }

                if (autoConfirm) {
                    client.doDelivery(deliveryRequest);
                }

                logger.exit(this, methodName);
            }

            private byte[] copyWrappyByteBuf(ByteBuf buf) {
                byte[] data = new byte[deliveryRequest.buf.array().length];
                System.arraycopy(deliveryRequest.buf.array(), 0, data, 0, deliveryRequest.buf.array().length);
                return data;
            }
        }, client, new CallbackPromiseImpl(client, true));

        logger.exit(this, methodName);
    }
}
