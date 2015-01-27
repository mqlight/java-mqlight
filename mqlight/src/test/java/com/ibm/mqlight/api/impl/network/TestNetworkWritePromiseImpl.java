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

public class TestNetworkWritePromiseImpl {

    @Test
    public void success() {
        MockComponent component = new MockComponent();
        long expectedAmount = 1000;
        Object expectedContext = new Object();
        NetworkWritePromiseImpl promise = new NetworkWritePromiseImpl(component, expectedAmount, expectedContext);
        boolean expectedDrainValue = true;
        
        assertFalse("Promise should not be created in complete state", promise.isComplete());
        promise.setSuccess(expectedDrainValue);
        
        assertTrue("Promise should have been marked as completed", promise.isComplete());
        assertEquals("Component should have been sent 1 message", 1, component.getMessages().size());
        assertTrue("Message should have been instanceof WriteResponse", component.getMessages().get(0) instanceof WriteResponse);
        WriteResponse response = (WriteResponse)component.getMessages().get(0);
        assertSame("Context object in WriteResponse should be same as passed into promise constructor", expectedContext, response.context);
        assertEquals("Amount in WriteResponse should be same as passed into promise constructor", expectedAmount, response.amount);
        assertSame("Drain value in WriteResponse should match that passed into promise setSuccess", expectedDrainValue, response.drained);
        
        try {
            promise.setSuccess(expectedDrainValue);
            throw new AssertionFailedError("Calling setSuccess() on a completed promise should throw an exception");
        } catch(IllegalStateException e) {
            // Expected
        }
        
        try {
            promise.setFailure(null);
            throw new AssertionFailedError("Calling setFailure() on a completed promise should throw an exception");
        } catch(IllegalStateException e) {
            // Expected
        }
    }
    
    @Test
    public void failure() {
        MockComponent component = new MockComponent();
        Object expectedContext = new Object();
        NetworkWritePromiseImpl promise = new NetworkWritePromiseImpl(component, 1000, expectedContext);
        
        promise.setFailure(new Exception());
        
        assertTrue("Promise should have been marked as completed", promise.isComplete());
        assertEquals("Component should have been sent 0 messages", 0, component.getMessages().size());
    }

}
