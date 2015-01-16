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

import java.net.URI;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;

import com.ibm.mqlight.api.DestinationListener;
import com.ibm.mqlight.api.MalformedDelivery;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.callback.CallbackService;
import com.ibm.mqlight.api.impl.callback.CallbackPromiseImpl;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;

class DestinationListenerWrapper<T> {

    private final NonBlockingClientImpl client;
    private final DestinationListener<T> listener;
    private final T context;
    
    protected DestinationListenerWrapper(NonBlockingClientImpl client, DestinationListener<T> listener, T context) {
        this.client = client;
        this.listener = listener;
        this.context = context;
    }
    
    protected void onUnsubscribed(final CallbackService callbackService, final String topicPattern, final String share) {
        if (listener != null) {
            callbackService.run(new Runnable() {
                public void run() {
                    listener.onUnsubscribed(client, context, topicPattern, share);
                }
            }, client, new CallbackPromiseImpl(client, true));
        }
    }
    
    protected void onDelivery(final CallbackService callbackService, final DeliveryRequest deliveryRequest, final QOS qos, final boolean autoConfirm) {
        callbackService.run(new Runnable() {
            public void run() {
                byte[] data = deliveryRequest.data;
                
                MalformedDelivery.MalformedReason malformedReason = null;
                byte[] payloadBytes = null;
                String payloadString = null;
                
                org.apache.qpid.proton.message.Message msg = Proton.message();
                try {
                    msg.decode(data, 0, data.length);
                } catch(BufferOverflowException | BufferUnderflowException e) { // TODO: are these the only exceptions thrown by bad AMQP data?
                    malformedReason = MalformedDelivery.MalformedReason.PAYLOADNOTAMQP;
                    payloadBytes = data;
                }

                Map<String, Object> properties = new HashMap<String, Object>();
                if (malformedReason == null) {
                    Object value = ((AmqpValue)msg.getBody()).getValue();
                    if (value instanceof Binary) {
                        Binary binaryValue = (Binary)value;
                        if ((binaryValue.getArrayOffset() == 0) && (binaryValue.getArray().length == binaryValue.getLength())) {
                            payloadBytes = binaryValue.getArray();
                        } else {
                            payloadBytes = new byte[binaryValue.getLength()];
                            System.arraycopy(binaryValue.getArray(), binaryValue.getArrayOffset(), payloadBytes, 0, binaryValue.getLength());
                        }
                    } else if (value instanceof String) {
                        payloadString = (String)value;
                    } else {
                        malformedReason = MalformedDelivery.MalformedReason.FORMATNOMAPPING;  // TODO: is this the right reason code?
                        payloadBytes = data;
                    }
                    
                    if ((msg.getApplicationProperties() != null) && (msg.getApplicationProperties().getValue() != null)) {
                        Map<?, ?> msgMap = msg.getApplicationProperties().getValue();
                        for (Map.Entry<?, ?> entry : msgMap.entrySet()) {
                            // TODO: what do we do with property values that we don't know how to interpret?
                            if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
                                properties.put((String)entry.getKey(), (String)entry.getValue());
                            }
                        }
                    }
                }
                
                // TODO: IIRC there are some message annotations used to describe other reasons a message is malformed - we should
                //       use these too!

                String crackedLinkName[] = NonBlockingClientImpl.crackLinkName(deliveryRequest.topicPattern);
                String shareName = crackedLinkName[1];
                String topicPattern = crackedLinkName[0];
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
                }
                
                if (payloadBytes != null) {
                    
                    if (malformedReason == null) {
                        BytesDeliveryImpl delivery = new BytesDeliveryImpl(client, qos, shareName, topic, topicPattern, ttl, ByteBuffer.wrap(payloadBytes), properties, autoConfirm ? null : deliveryRequest);
                        listener.onMessage(client, context, delivery);
                    } else {
                        // TODO
                        MalformedDeliveryImpl delivery = new MalformedDeliveryImpl(client, qos, shareName, topic, topicPattern, ttl, ByteBuffer.wrap(payloadBytes), properties, autoConfirm ? null : deliveryRequest, malformedReason);
                        listener.onMalformed(client, context, delivery);
                    }
                } else {
                    StringDeliveryImpl delivery = new StringDeliveryImpl(client, qos, shareName, topic, topicPattern, ttl, payloadString, properties, autoConfirm ? null : deliveryRequest);
                    listener.onMessage(client, context, delivery);
                }
                
                if (autoConfirm) {
                    client.doDelivery(deliveryRequest);
                }
            }
        }, client, new CallbackPromiseImpl(client, true));
    }
}
