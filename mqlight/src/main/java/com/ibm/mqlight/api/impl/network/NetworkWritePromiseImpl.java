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

public class NetworkWritePromiseImpl implements Promise<Boolean> {
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final Component component;
    private final long amount;
    private final Object context;
    
    public NetworkWritePromiseImpl(Component component, long amount, Object context) {
        this.component = component;
        this.amount = amount;
        this.context = context;
    }

    @Override
    public boolean isComplete() {
        return complete.get();
    }

    @Override
    public void setSuccess(Boolean drained) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        } else {
            component.tell(new WriteResponse(context, amount, drained), Component.NOBODY);
        }
    }

    @Override
    public void setFailure(Exception exception) throws IllegalStateException {
        // TODO: can never fail?
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        }
    }

}
