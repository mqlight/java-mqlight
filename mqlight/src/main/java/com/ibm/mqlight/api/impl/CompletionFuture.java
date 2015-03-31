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

import com.ibm.mqlight.api.CompletionListener;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.StateException;
import com.ibm.mqlight.api.callback.CallbackService;
import com.ibm.mqlight.api.impl.callback.CallbackPromiseImpl;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

public class CompletionFuture<T> implements Promise<T> {
  
    private static final Logger logger = LoggerFactory.getLogger(CompletionFuture.class);
  
    private boolean complete = false;
    
    private final NonBlockingClientImpl client;
    private CompletionListener<T> listener;
    private T context;
    
    private Exception cause = null;
    
    public CompletionFuture(NonBlockingClientImpl client) {
        final String methodName = "<init>";
        logger.entry(this, methodName, client);

        this.client = client;
        
        logger.exit(this, methodName);
    }
    
    public void setSuccess(T result) throws IllegalStateException {
        final String methodName = "postSuccess";
        logger.entry(this, methodName, result);
      
        final CompletionListener<T> l;
        final T c;
        synchronized(this) {
            if (complete) {
                final IllegalStateException ex = new IllegalStateException("Promise already completed");
                logger.throwing(this,  methodName, ex);
                throw ex;
            }
            complete = true;
            l = listener;
            c = context;
        }
        if (l != null) {
          client.run(new Runnable() {
                public void run() {
                    l.onSuccess(client, c);
                }
            }, client, new CallbackPromiseImpl(client, true));
        }
        
        logger.exit(this, methodName);
    }
    
    public void setFailure(final Exception exception) throws IllegalStateException {
        final String methodName = "postFailure";
        logger.entry(this, methodName, exception);
      
        this.cause = exception;
        final CompletionListener<T> l;
        final T c;
        synchronized(this) {
            if (complete) {
                final IllegalStateException ex = new IllegalStateException("Promise already completed");
                logger.throwing(this,  methodName, ex);
                throw ex;
            }
            complete = true;
            l = listener;
            c = context;
        }
        if (l != null) {
            client.run(new Runnable() {
                public void run() {
                    l.onError(client, c, cause);
                }
            }, client, new CallbackPromiseImpl(client, true));
        }
        
        logger.exit(this, methodName);
    }
    
    public void setListener(CallbackService callbackService, final CompletionListener<T> listener, final T context) throws StateException {
        final String methodName = "setListener";
        logger.entry(this, methodName, callbackService, listener, context);
      
        boolean callCallback = false;
        synchronized(this) {
            if (!complete) {
                this.listener = listener;
                this.context = context;
            } else {
                callCallback = true;
            }
        }
        
        if (callCallback) { 
            if ( cause != null) {
                if (cause instanceof StateException) throw (StateException)cause;
                else if (listener != null) {
                    callbackService.run(new Runnable() {
                        public void run() {
                            listener.onError(client, context, cause);
                        }
                    }, client, new CallbackPromiseImpl(client, true));
                }
            } else if (listener != null) {
                callbackService.run(new Runnable() {
                    public void run() {
                        listener.onSuccess(client, context);
                    }
                }, client, new CallbackPromiseImpl(client, true));
            }
        }
        
        logger.exit(this, methodName);
    }

    @Override
    public synchronized boolean isComplete() {
      return complete;
    }
}
