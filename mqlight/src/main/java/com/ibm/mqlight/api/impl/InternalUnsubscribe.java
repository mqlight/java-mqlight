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

class InternalUnsubscribe<T> extends Message implements QueueableWork {
    final CompletionFuture<T> future;
    final String topicPattern;
    final String share;
    final boolean zeroTtl;
    InternalUnsubscribe(NonBlockingClientImpl client, String topicPattern, String share, boolean zeroTtl) {
        future = new CompletionFuture<>(client);
        this.topicPattern = topicPattern;
        this.share = share;
        this.zeroTtl = zeroTtl;
    }
}