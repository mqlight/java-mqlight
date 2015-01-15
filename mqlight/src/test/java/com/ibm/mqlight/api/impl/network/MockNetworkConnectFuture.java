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

import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ibm.mqlight.api.impl.network.Event.Type;
import com.ibm.mqlight.api.network.NetworkChannel;
import com.ibm.mqlight.api.network.NetworkConnectFuture;

class MockNetworkConnectFuture implements NetworkConnectFuture {
    
    private boolean done = false;
    private final LinkedList<Event> events;
    private NetworkChannel channel = null;
    
    protected MockNetworkConnectFuture(LinkedList<Event> events) {
        this.events = events;
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCancelled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDone() {
        synchronized(events) {
            return done;
        }
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException,
            TimeoutException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSuccess(NetworkChannel channel) {
        synchronized(events) {
            events.addLast(new Event(Type.CONNECT_SUCCESS, null));
            done = true;
            this.channel = channel;
        }
    }

    @Override
    public void setFailure(Exception exception) {
        synchronized(events) {
            events.addLast(new Event(Type.CONNECT_FAILURE, exception));
            done = true;
        }
    }
    
    protected NetworkChannel getChannel() {
        synchronized(events) {
            return channel;
        }
    }
}