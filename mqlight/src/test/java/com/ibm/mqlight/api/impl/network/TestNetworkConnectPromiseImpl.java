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

import java.nio.ByteBuffer;

import junit.framework.AssertionFailedError;

import org.junit.Test;

import static org.junit.Assert.*;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.impl.MockComponent;
import com.ibm.mqlight.api.network.NetworkChannel;

public class TestNetworkConnectPromiseImpl {

    private class StubNetworkChannel implements NetworkChannel {
        @Override public void close(Promise<Void> promise) {}
        @Override public void write(ByteBuffer buffer, Promise<Boolean> promise) {}
        @Override public void setContext(Object context) {}
        @Override public Object getContext() { return null; }
    }

    @Test
    public void success() {
        MockComponent component = new MockComponent();
        Object expectedContext = new Object();
        NetworkConnectPromiseImpl promise = new NetworkConnectPromiseImpl(component, expectedContext);
        NetworkChannel expectedChannel = new StubNetworkChannel();
        
        assertFalse("Promise should not have been created in completed state", promise.isComplete());
        promise.setSuccess(expectedChannel);
        
        assertTrue("Promise should have been marked as completed", promise.isComplete());
        assertEquals("Component should have received 1 message", 1, component.getMessages().size());
        assertTrue("Message should have been of type ConnectResponse", component.getMessages().get(0) instanceof ConnectResponse);
        ConnectResponse response = (ConnectResponse)component.getMessages().get(0);
        assertNull("Message should have a null cause", response.exception);
        assertSame("Message should have correct channel", expectedChannel, response.channel);
        assertSame("Message should have correct context", expectedContext, response.context);
        assertSame("Promise should have correct channel", expectedChannel, promise.getChannel());
        assertSame("Promise should have correct context", expectedContext, promise.getContext());
        
        try {
            promise.setSuccess(null);
            throw new AssertionFailedError("Calling setSuccess on completed promise should throw exception");
        } catch(IllegalStateException e) {
            // Expected
        }
        
        try {
            promise.setFailure(null);
            throw new AssertionFailedError("Calling setFailure on completed promise should throw exception");
        } catch(IllegalStateException e) {
            // Expected
        }
    }
    
    @Test
    public void failure() {
        MockComponent component = new MockComponent();
        Object expectedContext = new Object();
        NetworkConnectPromiseImpl promise = new NetworkConnectPromiseImpl(component, expectedContext);
        ClientException expectedException = new ClientException("oops");

        promise.setFailure(expectedException);
        
        assertTrue("Promise should have been marked as completed", promise.isComplete());
        assertEquals("Component should have received 1 message", 1, component.getMessages().size());
        assertTrue("Message should have been of type ConnectResponse", component.getMessages().get(0) instanceof ConnectResponse);
        ConnectResponse response = (ConnectResponse)component.getMessages().get(0);
        assertSame("Message should have correct cause cause", expectedException, response.exception);
        assertNull("Message should have null channel", response.channel);
        assertSame("Message should have correct context", expectedContext, response.context);
    }
}
