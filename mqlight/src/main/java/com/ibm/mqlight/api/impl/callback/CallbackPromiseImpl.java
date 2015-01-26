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

package com.ibm.mqlight.api.impl.callback;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.impl.Component;

public class CallbackPromiseImpl implements Promise<Void> {
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final Component component;
    private final boolean ignoreSuccess;
    
    public CallbackPromiseImpl(Component component, boolean ignoreSuccess) {
        this.component = component;
        this.ignoreSuccess = ignoreSuccess;
    }

    @Override
    public boolean isComplete() {
        return complete.get();
    }

    @Override
    public void setFailure(Exception exception) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        }
        component.tell(new CallbackExceptionNotification(exception), component);
    }

    @Override
    public void setSuccess(Void result) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        } else {
            if (!ignoreSuccess) {
                component.tell(new FlushResponse(), Component.NOBODY);
            }
        }
    }
}
