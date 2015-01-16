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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointPromise;

class MockEndpointPromise implements EndpointPromise {

    protected enum Method { NONE, SUCCESS, WAIT, FAILURE }
    private final MockEndpointPromise.Method expectedMethod;
    private Endpoint actualEndpoint;
    long actualDelay;
    private boolean done;
    
    protected MockEndpointPromise(MockEndpointPromise.Method expectedMethod) {
        this.expectedMethod = expectedMethod;
    }
    
    @Override public synchronized boolean isComplete() {
        return done;
    }
    
    @Override public synchronized void setSuccess(Endpoint endpoint) {
        assertEquals("didn't expect setSuccess to be called", expectedMethod, Method.SUCCESS);
        assertFalse("didn't expect setSuccess to be called on a completed future", done);
        done = true;
        actualEndpoint = endpoint;
    }

    @Override
    public synchronized void setWait(long delay) {
        assertEquals("didn't expect setWait to be called", expectedMethod, Method.WAIT);
        assertFalse("didn't expect setSuccess to be called on a completed future", done);
        done = true;
        actualDelay = delay;
    }

    @Override
    public synchronized void setFailure(Exception exception) {
        assertEquals("didn't expect setFailure to be called", expectedMethod, Method.FAILURE);
        assertFalse("didn't expect setSuccess to be called on a completed future", done);
        done = true;
    }
    
    protected Endpoint getEndoint() { return actualEndpoint; }
    protected long getDelay() { return actualDelay; }
}