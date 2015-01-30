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
