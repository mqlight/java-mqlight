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

import com.ibm.mqlight.api.ClientException;
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
            component.tell(new EndpointResponse(endpoint, null), Component.NOBODY);
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
            ClientException clientException;
            if (exception instanceof ClientException) {
                clientException = (ClientException)exception;
            } else {
                clientException = new ClientException(
                        "A problem occurred when trying to locate an instance of the MQ Light server.  See linked exception for more details",
                        exception);
            }
            component.tell(new EndpointResponse(null, clientException), Component.NOBODY);
        }
    }

}
