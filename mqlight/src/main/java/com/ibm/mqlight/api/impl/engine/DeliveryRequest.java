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

package com.ibm.mqlight.api.impl.engine;

import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;

import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.impl.Message;

// TODO: this is slightly unusual in that it is sent from the Engine to the
//       component that issued the subscribe (rather than most messages in
//       this package which are sent from some Component to the Engine component)
public class DeliveryRequest extends Message {
    
    public final byte[] data;
    public final QOS qos;
    public final String topicPattern;
    protected final Delivery delivery;
    protected final Connection protonConnection;
    
    protected DeliveryRequest(byte[] data, QOS qos, String topicPattern, Delivery delivery, Connection protonConnection) {
        this.data = data;
        this.qos = qos;
        this.topicPattern = topicPattern;
        this.delivery = delivery;
        this.protonConnection = protonConnection;
    }
}
