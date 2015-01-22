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

import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.network.NetworkChannel;

public class NetworkConnectPromiseImpl implements Promise<NetworkChannel> {
    
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final Component component;
    private final Object context;
    
    public NetworkConnectPromiseImpl(Component component, Object context) {
        this.component = component;
        this.context = context;
    }

    @Override
    public boolean isComplete() {
        return complete.get();
    }

    private NetworkChannel channel;
    
    @Override
    public void setSuccess(NetworkChannel channel) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        } else {
            synchronized(this) {
                this.channel = channel;
            }
            component.tell(new ConnectResponse(channel, null, context), Component.NOBODY);
        }
    }

    @Override
    public void setFailure(Exception exception) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        } else {
            component.tell(new ConnectResponse(channel, exception, context), Component.NOBODY);
        }
    }
    
    public synchronized NetworkChannel getChannel() {
        return channel;
    }
    
    public Object getContext() {
        return context;
    }
}
