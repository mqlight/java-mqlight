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

import junit.framework.AssertionFailedError;
import static org.junit.Assert.*;

import org.junit.Test;

import com.ibm.mqlight.api.impl.MockComponent;

public class TestCallbackPromiseImpl {

    @Test
    public void success() {
        MockComponent mockComponent = new MockComponent();
        CallbackPromiseImpl promise = new CallbackPromiseImpl(mockComponent, false);
        
        assertFalse("Promise should not have been created in a completed stata", promise.isComplete());
        promise.setSuccess(null);
        
        assertTrue("Promise should have been marked complete", promise.isComplete());
        assertEquals("Expected a single message to have been delivered to the component", 1, mockComponent.getMessages().size());
        assertTrue("Message should have been of type FlushResponse", mockComponent.getMessages().get(0) instanceof FlushResponse);

        try {
            promise.setSuccess(null);
            throw new AssertionFailedError("Should not have been able to complete a promise twice (calling setSuccess)");
        } catch(IllegalStateException e) {
            // Expected code path
        }
        
        try {
            promise.setFailure(null);
            throw new AssertionFailedError("Should not have been able to complete a promise twice (calling setFailure)");
        } catch(IllegalStateException e) {
            // Expected code path
        }
    }

    public void ignoreSuccess() {
        MockComponent mockComponent = new MockComponent();
        CallbackPromiseImpl promise = new CallbackPromiseImpl(mockComponent, true);
        promise.setSuccess(null);
        assertEquals("Expected no messages to have been delivered to the component", 0, mockComponent.getMessages().size());
    }

    @Test
    public void failure() {
        MockComponent mockComponent = new MockComponent();
        CallbackPromiseImpl promise = new CallbackPromiseImpl(mockComponent, false);

        promise.setFailure(new Exception());
        assertTrue("Promise should have been marked complete", promise.isComplete());
    }
}
