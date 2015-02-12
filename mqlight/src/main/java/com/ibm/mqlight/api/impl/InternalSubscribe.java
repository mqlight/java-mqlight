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

import com.google.gson.GsonBuilder;
import com.ibm.mqlight.api.DestinationListener;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

class InternalSubscribe<T> extends Message implements QueueableWork {
  
    private static final Logger logger = LoggerFactory.getLogger(InternalSubscribe.class);
  
    final CompletionFuture<T> future;
    final String topic;
    final QOS qos;
    final int credit;
    final boolean autoConfirm;
    final int ttl;
    final DestinationListenerWrapper<T> destListener;

    InternalSubscribe(NonBlockingClientImpl client, String topic, QOS qos, int credit, boolean autoConfirm, int ttl,
                      GsonBuilder gsonBuilder, DestinationListener<T> destListener, T context) {
        final String methodName = "<init>";
        logger.entry(this, methodName, client, topic, qos, credit, autoConfirm, ttl, gsonBuilder, destListener, context);
      
        future = new CompletionFuture<>(client);
        this.topic = topic;
        this.qos = qos;
        this.credit = credit;
        this.autoConfirm = autoConfirm;
        this.ttl = ttl;
        this.destListener = new DestinationListenerWrapper<T>(client, gsonBuilder, destListener, context);
        
        logger.exit(this, methodName);
    }
}