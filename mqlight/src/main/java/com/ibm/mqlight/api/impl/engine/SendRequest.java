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

import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.impl.Message;

public class SendRequest extends Message {
    protected final EngineConnection connection;
    protected final String topic;
    protected final byte[] data;
    protected final int length;
    protected final QOS qos;
    public SendRequest(EngineConnection connection, String topic, byte[] data, int length, QOS qos) {
        this.connection = connection;
        this.topic = topic;
        this.data = data;
        this.length = length;
        this.qos = qos;
    }
    
}
