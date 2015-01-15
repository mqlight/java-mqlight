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

import java.nio.ByteBuffer;
import java.util.Map;

import com.ibm.mqlight.api.MalformedDelivery;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;

public class MalformedDeliveryImpl extends BytesDeliveryImpl implements MalformedDelivery {

    private final MalformedReason reason;
    
    protected MalformedDeliveryImpl(NonBlockingClientImpl client, QOS qos, String shareName, String topic, String topicPattern, long ttl,
            ByteBuffer data, Map<String, Object> properties, DeliveryRequest req, MalformedReason reason) {
        super(client, qos, shareName, topic, topicPattern, ttl, data, properties, req);
        this.reason = reason;
    }

    public MalformedReason getReason() {
        return reason;
    }
}
