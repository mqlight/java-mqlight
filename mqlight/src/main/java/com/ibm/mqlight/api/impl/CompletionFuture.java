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
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.StateException;
import com.ibm.mqlight.api.callback.CallbackService;
import com.ibm.mqlight.api.impl.callback.CallbackPromiseImpl;

// TODO: re-write this class based on the Promise interface.
public class CompletionFuture<T> {

    private boolean complete = false;
    
    private final NonBlockingClient client;
    private CompletionListener<T> listener;
    private T context;
    
    private Exception cause = null;
    
    public CompletionFuture(NonBlockingClient client) {
        this.client = client;
    }
    
    public void postSuccess(CallbackService callbackService) {
        final CompletionListener<T> l;
        final T c;
        synchronized(this) {
            if (complete) return;
            complete = true;
            l = listener;
            c = context;
        }
        if (l != null) {
            callbackService.run(new Runnable() {
                public void run() {
                    l.onSuccess(client, c);
                }
            }, client, new CallbackPromiseImpl(client, true));
        }
    }
    
    public void postFailure(CallbackService callbackService, final Exception cause) {
        this.cause = cause;
        final CompletionListener<T> l;
        final T c;
        synchronized(this) {
            if (complete) return;
            complete = true;
            l = listener;
            c = context;
        }
        if (l != null) {
            callbackService.run(new Runnable() {
                public void run() {
                    l.onError(client, c, cause);
                }
            }, client, new CallbackPromiseImpl(client, true));
        }
    }
    
    public void setListener(CallbackService callbackService, final CompletionListener<T> listener, final T context) throws StateException {
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
    }
}
