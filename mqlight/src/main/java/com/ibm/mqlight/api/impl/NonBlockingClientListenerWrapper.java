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

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.NonBlockingClientListener;
import com.ibm.mqlight.api.callback.CallbackService;
import com.ibm.mqlight.api.impl.callback.CallbackPromiseImpl;

class NonBlockingClientListenerWrapper<T>{

    NonBlockingClient client;
    NonBlockingClientListener<T> listener;
    T context;
    
    protected NonBlockingClientListenerWrapper(NonBlockingClient client, NonBlockingClientListener<T> listener, T context) {
        this.client = client;
        this.listener = listener;
        this.context = context;
    }
    
    void onRestarted(CallbackService callbackService) {
        if (listener != null) {
            callbackService.run(new Runnable() {
                public void run() {
                    listener.onRestarted(client, context);
                }
            }, client, new CallbackPromiseImpl(client, true));
        }
    }
    
    void onRetrying(CallbackService callbackService, final ClientException exception) {
        if (listener != null) {
            callbackService.run(new Runnable() {
                public void run() {
                    listener.onRetrying(client, context, exception);
                }
            }, client, new CallbackPromiseImpl(client, true));
        }
    }
    
    void onStarted(CallbackService callbackService) {
        if (listener != null) {
            callbackService.run(new Runnable() {
                public void run() {
                    listener.onStarted(client, context);
                }
            }, client, new CallbackPromiseImpl(client, true));
        }
    }
    
    void onStopped(CallbackService callbackService, final ClientException exception) {
        if (listener != null) {
            callbackService.run(new Runnable() {
                public void run() {
                    listener.onStopped(client, context, exception);
                }
            }, client, new CallbackPromiseImpl(client, true));
        }
    }
    
    void onDrain(CallbackService callbackService) {
        if (listener != null) {
            callbackService.run(new Runnable() {
                public void run() {
                    listener.onDrain(client, context);
                }
            }, client, new CallbackPromiseImpl(client, true));
        }
    }
}
