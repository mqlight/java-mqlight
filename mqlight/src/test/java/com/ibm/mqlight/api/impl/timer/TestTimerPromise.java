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

import static org.junit.Assert.*;
import junit.framework.AssertionFailedError;

import org.junit.Test;

import com.ibm.mqlight.api.impl.MockComponent;

public class TestTimerPromise {

    @Test public void success() {
        MockComponent mockComponent = new MockComponent();
        Object expectedContext = new Object();
        TimerPromiseImpl promise = new TimerPromiseImpl(mockComponent, expectedContext);
        
        assertFalse("Promise should not have been created in a completed state",  promise.isComplete());
        assertSame("Same context object should have been returned", expectedContext, promise.getContext());
        
        promise.setSuccess(null);
        
        assertTrue("Promise should have been marked as completed",  promise.isComplete());
        assertEquals("Expected 1 message to have been passed to the component", 1, mockComponent.getMessages().size());
        assertTrue("Expected message to have been of type PopResponse", mockComponent.getMessages().get(0) instanceof PopResponse);
        
        try {
            promise.setSuccess(null);
            throw new AssertionFailedError("Should have received an exception when trying to complete a proimse twice (setSuccess)");
        } catch(IllegalStateException e) {
            // Expected
        }
        
        try {
            promise.setFailure(null);
            throw new AssertionFailedError("Should have received an exception when trying to complete a proimse twice (setFailure)");
        } catch(IllegalStateException e) {
            // Expected
        }
    }
    
    @Test public void failure() {
        MockComponent mockComponent = new MockComponent();
        Object expectedContext = new Object();
        TimerPromiseImpl promise = new TimerPromiseImpl(mockComponent, expectedContext);
        
        promise.setFailure(new Exception());
        assertEquals("Expected 1 message to have been passed to the component", 1, mockComponent.getMessages().size());
        assertTrue("Expected message to have been of type CancelResponse", mockComponent.getMessages().get(0) instanceof CancelResponse);

        CancelResponse cancelResponse = (CancelResponse)mockComponent.getMessages().get(0);
        assertSame("Expected cancel response object to reference timer promise", promise, cancelResponse.promise);
    }
}
