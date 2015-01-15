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

import java.nio.ByteBuffer;
import java.util.LinkedList;

import com.ibm.mqlight.api.impl.network.Event.Type;
import com.ibm.mqlight.api.network.NetworkChannel;
import com.ibm.mqlight.api.network.NetworkListener;

class MockNetworkListener implements NetworkListener {

    private final LinkedList<Event> events;
    
    protected MockNetworkListener(LinkedList<Event> events) {
        this.events = events;
    }
    
    @Override
    public void onRead(NetworkChannel channel, ByteBuffer buffer) {
        synchronized(events) {
            events.addLast(new Event(Type.CHANNEL_READ, buffer));
        }
    }

    @Override
    public void onClose(NetworkChannel channel) {
        synchronized(events) {
            events.addLast(new Event(Type.CHANNEL_CLOSE, null));
        }
    }

    @Override
    public void onError(NetworkChannel channel, Exception exception) {
        synchronized(events) {
            events.addLast(new Event(Type.CHANNEL_ERROR, exception));
        }
    }
    
}