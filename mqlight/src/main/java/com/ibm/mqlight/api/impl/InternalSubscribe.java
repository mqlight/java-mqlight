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

import com.google.gson.GsonBuilder;
import com.ibm.mqlight.api.DestinationListener;
import com.ibm.mqlight.api.QOS;

class InternalSubscribe<T> extends Message implements QueueableWork {
    final CompletionFuture<T> future;
    final String topic;
    final QOS qos;
    final int credit;
    final boolean autoConfirm;
    final int ttl;
    final DestinationListenerWrapper<T> destListener;

    InternalSubscribe(NonBlockingClientImpl client, String topic, QOS qos, int credit, boolean autoConfirm, int ttl,
                      GsonBuilder gsonBuilder, DestinationListener<T> destListener, T context) {
        future = new CompletionFuture<>(client);
        this.topic = topic;
        this.qos = qos;
        this.credit = credit;
        this.autoConfirm = autoConfirm;
        this.ttl = ttl;
        this.destListener = new DestinationListenerWrapper<T>(client, gsonBuilder, destListener, context);
    }
}