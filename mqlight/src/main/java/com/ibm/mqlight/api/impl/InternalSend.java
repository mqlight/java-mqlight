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

import com.ibm.mqlight.api.QOS;

class InternalSend<T> extends Message implements QueueableWork {
    final String topic;
    final QOS qos;
    final byte[] data;
    final int length;
    final CompletionFuture<T> future;
    InternalSend(NonBlockingClientImpl client, String topic, QOS qos, byte[] data, int length) {
        this.future = new CompletionFuture<T>(client);
        this.topic = topic;
        this.qos = qos;
        this.data = data;
        this.length = length;
    }
}