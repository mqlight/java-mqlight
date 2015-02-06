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
