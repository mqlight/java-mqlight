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

package com.ibm.mqlight.api.impl.endpoint;

import java.util.LinkedList;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointPromise;
import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.impl.Message;

public class TestEndpointPromiseImpl {

    protected class MockComponent extends Component {
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
        @Override public String getUser() { return null; }
        @Override public String getPassword() { return null; }
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
    }
}
