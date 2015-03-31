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

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

import org.junit.Test;

import static org.junit.Assert.*;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.impl.MockComponent;
import com.ibm.mqlight.api.network.NetworkChannel;

public class TestNetworkListenerImpl {

    private class StubNetworkChannel implements NetworkChannel {
        @Override public void close(Promise<Void> promise) {}
        @Override public void write(ByteBuf buffer, Promise<Boolean> promise) {}
        @Override public void setContext(Object context) {}
        @Override public Object getContext() { return null; }
    }
    
    @Test
    public void onRead() {
        MockComponent component = new MockComponent();
        NetworkListenerImpl listener = new NetworkListenerImpl(component);
        StubNetworkChannel expectedChannel = new StubNetworkChannel();
        ByteBuffer expectedBuffer = ByteBuffer.allocate(1);

        listener.onRead(expectedChannel, expectedBuffer);
        assertEquals("Expected component to receive 1 message", 1, component.getMessages().size());
        assertTrue("Expected message to be instanceof DataRead", component.getMessages().get(0) instanceof DataRead);
        DataRead read = (DataRead)component.getMessages().get(0);
        assertSame("Expected same buffer in message", expectedBuffer, read.buffer);
        assertSame("Expected same channel in message", expectedChannel, read.channel);
    }
    
    @Test
    public void onClose() {
        MockComponent component = new MockComponent();
        NetworkListenerImpl listener = new NetworkListenerImpl(component);
        StubNetworkChannel expectedChannel = new StubNetworkChannel();

        listener.onClose(expectedChannel);
        assertEquals("Expected component to receive 1 message", 1, component.getMessages().size());
        assertTrue("Expected message to be instanceof ConnectionError", component.getMessages().get(0) instanceof ConnectionError);
        ConnectionError error = (ConnectionError)component.getMessages().get(0);
        assertSame("Expected same channel in message", expectedChannel, error.channel);
        assertNotNull("Expected a cause to be filled in in the message", error.cause);
    }
    
    @Test
    public void onError() {
        MockComponent component = new MockComponent();
        NetworkListenerImpl listener = new NetworkListenerImpl(component);
        StubNetworkChannel expectedChannel = new StubNetworkChannel();
        Exception expectedException = new Exception();

        listener.onError(expectedChannel, expectedException);
        assertEquals("Expected component to receive 1 message", 1, component.getMessages().size());
        assertTrue("Expected message to be instanceof ConnectionError", component.getMessages().get(0) instanceof ConnectionError);
        ConnectionError error = (ConnectionError)component.getMessages().get(0);
        assertSame("Expected same channel in message", expectedChannel, error.channel);
        assertSame("Expected same exception in message", expectedException, error.cause);
    }
}
