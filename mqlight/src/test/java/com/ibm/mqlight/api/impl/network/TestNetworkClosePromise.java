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

import static org.junit.Assert.*;
import junit.framework.AssertionFailedError;

import org.junit.Test;

import com.ibm.mqlight.api.impl.MockComponent;

public class TestNetworkClosePromise {

    @Test
    public void success() {
        MockComponent component = new MockComponent();
        Object expectedContext = new Object();
        NetworkClosePromiseImpl promise = new NetworkClosePromiseImpl(component, expectedContext);
        
        assertFalse("Promise should not have been created completed", promise.isComplete());
        promise.setSuccess(null);
        
        assertTrue("Promise should have been marked complete", promise.isComplete());
        assertEquals("Expected one message to have been sent to the component", 1, component.getMessages().size());
        assertTrue("Expected type of message to be DisconnectResponse", component.getMessages().get(0) instanceof DisconnectResponse);
        DisconnectResponse response = (DisconnectResponse)component.getMessages().get(0);
        assertSame("Expected context in response to match that passed into promises' constructor", expectedContext, response.context);
        
        try {
            promise.setSuccess(null);
            throw new AssertionFailedError("Expected that calling setSuccess() on a completed process would throw an exception");
        } catch(IllegalStateException e) {
            // Expected
        }
        
        try {
            promise.setFailure(null);
            throw new AssertionFailedError("Expected that calling setFailure() on a completed process would throw an exception");
        } catch(IllegalStateException e) {
            // Expected
        }
    }
    
    @Test
    public void failure() {
        MockComponent component = new MockComponent();
        Object expectedContext = new Object();
        NetworkClosePromiseImpl promise = new NetworkClosePromiseImpl(component, expectedContext);
        
        assertFalse("Promise should not have been created completed", promise.isComplete());
        promise.setFailure(new Exception());
        assertTrue("Promise should have been marked complete", promise.isComplete());
        assertEquals("Expected zero messages to have been sent to the component", 0, component.getMessages().size());
    }
}
