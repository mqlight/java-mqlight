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
    private final String description;
    private final String format;
    private final int ccsid;
    
    protected MalformedDeliveryImpl(NonBlockingClientImpl client, QOS qos, String shareName, String topic, String topicPattern, long ttl,
                                    ByteBuffer data, Map<String, Object> properties, DeliveryRequest req, MalformedReason reason,
                                    String malformedDescription, String malformedMQMDFormat, int malformedMQMDCCSID) {
        super(client, qos, shareName, topic, topicPattern, ttl, data, properties, req);
        this.reason = reason;
        this.description = malformedDescription;
        this.format = malformedMQMDFormat;
        this.ccsid = malformedMQMDCCSID;
    }

    @Override
    public Type getType() {
        return Type.MALFORMED;
    }
    
    public MalformedReason getReason() {
        return reason;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getMQMDFormat() {
        return format;
    }
    
    public int getMQMDCodedCharSetId() {
        return ccsid;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getCanonicalName());
        sb.append("@").append(Integer.toHexString(hashCode()))
          .append(" [data=").append(getData())
          .append(", description=").append(getDescription())
          .append(", mqmd ccsid=").append(getMQMDCodedCharSetId())
          .append(", properties=").append(getProperties())
          .append(", qos=").append(getQOS())
          .append(", reason=").append(getReason())
          .append(", share=").append(getShare())
          .append(", topic=").append(getTopic())
          .append(", topic pattern=").append(getTopicPattern())
          .append(", ttl=").append(getTtl())
          .append(", type=").append(getType())
          .append("]");
        return sb.toString();
    }
}
