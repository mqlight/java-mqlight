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
package com.ibm.mqlight.api.impl.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedList;

import junit.framework.AssertionFailedError;

import org.junit.Test;

import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointPromise;
import com.ibm.mqlight.api.impl.ComponentImpl;
import com.ibm.mqlight.api.impl.Message;

public class TestEndpointPromiseImpl {

    protected class MockComponent extends ComponentImpl {
        protected LinkedList<Message> messages = new LinkedList<>();

        @Override
        protected void onReceive(Message message) {
            messages.addLast(message);
        }
    }

    protected class StubEndpoint implements Endpoint {
        @Override public String getHost() { return null; }
        @Override public int getPort() { return 0; }
        @Override public boolean useSsl() { return false; }
        @Override public File getCertChainFile() { return null; }
        @Override public boolean getVerifyName() { return false; }
        @Override public String getUser() { return null; }
        @Override public String getPassword() { return null; }
        @Override public int getIdleTimeout() { return 0; }
    }

    private void testPromiseThrowsIllegalStateException(EndpointPromise promise) {
        try {
            promise.setSuccess(null);
            throw new AssertionFailedError("setSuccess should have thrown an IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        try {
            promise.setWait(1234);
            throw new AssertionFailedError("setSuccess should have thrown an IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        try {
            promise.setFailure(null);
            throw new AssertionFailedError("setSuccess should have thrown an IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    @Test
    public void endpointNotDone() {
        MockComponent component = new MockComponent();
        EndpointPromise promise = new EndpointPromiseImpl(component);

        assertFalse("future should not have been marked done", promise.isComplete());
        assertTrue("component should not have been notified of any messages", component.messages.isEmpty());
    }

    @Test
    public void setWait() {
        MockComponent component = new MockComponent();
        EndpointPromise promise = new EndpointPromiseImpl(component);
        long waitValue = 12345;
        promise.setWait(waitValue);

        assertTrue("future should have been marked as done", promise.isComplete());
        assertEquals("one message should have been delivered to the component", 1, component.messages.size());
        assertEquals("wrong type for message delivered to component", ExhaustedResponse.class, component.messages.getFirst().getClass());
        ExhaustedResponse resp = (ExhaustedResponse)component.messages.getFirst();
        assertEquals("wrong delay value in message", waitValue, resp.delay);
        testPromiseThrowsIllegalStateException(promise);
    }

    @Test
    public void setSuccess() {
        MockComponent component = new MockComponent();
        EndpointPromise promise = new EndpointPromiseImpl(component);
        Endpoint endpoint = new StubEndpoint();
        promise.setSuccess(endpoint);

        assertTrue("future should have been marked as done", promise.isComplete());
        assertEquals("one message should have been delivered to the component", 1, component.messages.size());
        assertEquals("wrong type for message delivered to component", EndpointResponse.class, component.messages.getFirst().getClass());
        EndpointResponse resp = (EndpointResponse)component.messages.getFirst();
        assertSame("wrong value in message", endpoint, resp.endpoint);
        testPromiseThrowsIllegalStateException(promise);
    }

    @Test
    public void setFailure() {
        MockComponent component = new MockComponent();
        EndpointPromise promise = new EndpointPromiseImpl(component);
        promise.setFailure(new Exception());

        assertTrue("future should have been marked as done", promise.isComplete());
        assertEquals("one message should have been delivered to the component", 1, component.messages.size());
        assertEquals("wrong type for message delivered to component", EndpointResponse.class, component.messages.getFirst().getClass());
        EndpointResponse resp = (EndpointResponse)component.messages.getFirst();
        assertSame("wrong value in message", null, resp.endpoint);
        testPromiseThrowsIllegalStateException(promise);
    }
}
