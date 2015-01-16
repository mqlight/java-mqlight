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

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.impl.network.Event.Type;
import com.ibm.mqlight.api.network.NetworkChannel;

class MockNetworkConnectPromise implements Promise<NetworkChannel> {
    
    private boolean done = false;
    private final LinkedList<Event> events;
    private NetworkChannel channel = null;
    
    protected MockNetworkConnectPromise(LinkedList<Event> events) {
        this.events = events;
    }

    @Override
    public boolean isComplete() {
        synchronized(events) {
            return done;
        }
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