/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
