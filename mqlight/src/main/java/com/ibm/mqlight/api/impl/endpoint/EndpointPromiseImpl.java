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
package com.ibm.mqlight.api.impl.endpoint;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointPromise;
import com.ibm.mqlight.api.impl.Component;

public class EndpointPromiseImpl implements EndpointPromise {

    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final Component component;
    
    public EndpointPromiseImpl(Component component) {
        this.component = component;
    }
    
    @Override
    public boolean isComplete() {
        return complete.get();
    }

    @Override
    public void setSuccess(Endpoint endpoint) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        } else {
            component.tell(new EndpointResponse(endpoint), Component.NOBODY);
        }
    }

    @Override
    public void setWait(long delay) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        } else {
            component.tell(new ExhaustedResponse(delay), Component.NOBODY);
        }
    }

    @Override
    public void setFailure(Exception exception) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        } else {
            // TODO: is this a reasonable way to represent a failure of the endpoint service?
            // TODO: shouldn't we propagate the exception somewhere?
            component.tell(new EndpointResponse(null), Component.NOBODY);
        }
    }

}
