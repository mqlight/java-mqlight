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

public class NetworkClosePromiseImpl implements Promise<Void> {

    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final Component component;
    private final Object context;
    
    public NetworkClosePromiseImpl(Component component, Object context) {
        this.component = component;
        this.context = context;
    }

    @Override
    public boolean isComplete() {
        return complete.get();
    }

    @Override
    public void setSuccess(Void x) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        } else {
            component.tell(new DisconnectResponse(context), Component.NOBODY);
        }
    }

    @Override
    public void setFailure(Exception exception) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        }
        // TODO: should never be called!
    }
}
