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
