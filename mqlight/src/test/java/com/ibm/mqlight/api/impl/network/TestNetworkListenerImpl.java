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

import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.impl.MockComponent;
import com.ibm.mqlight.api.network.NetworkChannel;

public class TestNetworkListenerImpl {

    private class StubNetworkChannel implements NetworkChannel {
        @Override public void close(Promise<Void> promise) {}
        @Override public void write(ByteBuffer buffer, Promise<Boolean> promise) {}
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
