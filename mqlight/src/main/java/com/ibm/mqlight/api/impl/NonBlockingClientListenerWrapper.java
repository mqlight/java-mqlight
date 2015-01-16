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
    
    void onRetrying(CallbackService callbackService, final ClientException throwable) {
        if (listener != null) {
            callbackService.run(new Runnable() {
                public void run() {
                    listener.onRetrying(client, context, throwable);
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
    
    void onStopped(CallbackService callbackService, final ClientException throwable) {
        if (listener != null) {
            callbackService.run(new Runnable() {
                public void run() {
                    listener.onStopped(client, context, throwable);
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
