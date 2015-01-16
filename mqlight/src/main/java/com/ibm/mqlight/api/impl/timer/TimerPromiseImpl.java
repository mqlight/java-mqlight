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

package com.ibm.mqlight.api.impl.timer;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.impl.Component;

public class TimerPromiseImpl implements Promise<Void> {

    private final Component component;
    private final Object context;
    private AtomicBoolean complete = new AtomicBoolean(false);
    
    public TimerPromiseImpl(Component component, Object context) {
        this.component = component;
        this.context = context;
    }

    @Override
    public void setFailure(Exception exception) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise is already complete");
        } else {
            component.tell(new CancelResponse(this), component);
        }
    }

    @Override
    public void setSuccess(Void result) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise is already complete");
        } else {
            component.tell(new PopResponse(this), component);
        }
    }

    @Override
    public boolean isComplete() {
        return complete.get();
    }

    public Object getContext() {
        return context;
    }
}
