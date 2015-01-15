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

package com.ibm.mqlight.api.impl.network;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.network.NetworkChannel;
import com.ibm.mqlight.api.network.NetworkConnectFuture;

public class NetworkConnectFutureImpl implements NetworkConnectFuture {
    
    private final AtomicBoolean done = new AtomicBoolean(false);
    private final Component component;
    private final Object context;
    
    public NetworkConnectFutureImpl(Component component, Object context) {
        this.component = component;
        this.context = context;
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new IllegalStateException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return done.get();
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        throw new IllegalStateException();
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        throw new IllegalStateException();
    }

    private NetworkChannel channel;
    
    @Override
    public synchronized void setSuccess(NetworkChannel channel) {
        this.channel = channel;
        done.set(true);
        component.tell(new ConnectResponse(channel, null, context), Component.NOBODY);
    }

    @Override
    public void setFailure(Exception exception) {
        done.set(true);
        component.tell(new ConnectResponse(channel, exception, context), Component.NOBODY);
    }
    
    public synchronized NetworkChannel getChannel() {
        return channel;
    }
    
    public Object getContext() {
        return context;
    }
}
