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

import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.StringDelivery;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;

class StringDeliveryImpl extends DeliveryImpl implements StringDelivery {
    
    private final String data;
    
    protected StringDeliveryImpl(NonBlockingClientImpl client, QOS qos, String share, String topic, 
                                 String topicPattern, long ttl, String data, Map<String, Object> properties, DeliveryRequest deliveryRequest) {
        super(client, qos, share, topic, topicPattern, ttl, properties, deliveryRequest);
        this.data = data;
    }
    
    @Override
    public Type getType() {
        return Type.STRING;
    }

    @Override
    public String getData() {
        return data;
    }

}
