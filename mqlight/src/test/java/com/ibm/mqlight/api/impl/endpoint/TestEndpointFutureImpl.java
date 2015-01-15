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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointFuture;
import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.impl.Message;

public class TestEndpointFutureImpl {

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
        EndpointFuture future = new EndpointFutureImpl(component);
        
        assertFalse("future should not have been marked done", future.isDone());
        assertFalse("future should not have been marked cancelled", future.isCancelled());
        assertTrue("component should not have been notified of any messages", component.messages.isEmpty());
    }
    

    @Test(expected=IllegalStateException.class)
    public void cancelThrowsAnException() {
        MockComponent component = new MockComponent();
        EndpointFuture future = new EndpointFutureImpl(component);
        future.cancel(false);
    }
    
    @Test(expected=IllegalStateException.class)
    public void getThrowsAnException1() throws ExecutionException, InterruptedException {
        MockComponent component = new MockComponent();
        EndpointFuture future = new EndpointFutureImpl(component);
        future.get();
    }
    
    @Test(expected=IllegalStateException.class)
    public void getThrowsAnException2() throws ExecutionException, InterruptedException, TimeoutException {
        MockComponent component = new MockComponent();
        EndpointFuture future = new EndpointFutureImpl(component);
        future.get(1000, TimeUnit.SECONDS);
    }
    
    @Test
    public void setWait() {
        MockComponent component = new MockComponent();
        EndpointFuture future = new EndpointFutureImpl(component);
        long waitValue = 12345;
        future.setWait(waitValue);
        
        assertTrue("future should have been marked as done", future.isDone());
        assertFalse("future should not have been marked as cancelled", future.isCancelled());
        assertEquals("one message should have been delivered to the component", 1, component.messages.size());
        assertEquals("wrong type for message delivered to component", ExhaustedResponse.class, component.messages.getFirst().getClass());
        ExhaustedResponse resp = (ExhaustedResponse)component.messages.getFirst();
        assertEquals("wrong delay value in message", waitValue, resp.delay);
    }
    
    @Test
    public void setSuccess() {
        MockComponent component = new MockComponent();
        EndpointFuture future = new EndpointFutureImpl(component);
        Endpoint endpoint = new StubEndpoint();
        future.setSuccess(endpoint);

        assertTrue("future should have been marked as done", future.isDone());
        assertFalse("future should not have been marked as cancelled", future.isCancelled());
        assertEquals("one message should have been delivered to the component", 1, component.messages.size());
        assertEquals("wrong type for message delivered to component", EndpointResponse.class, component.messages.getFirst().getClass());
        EndpointResponse resp = (EndpointResponse)component.messages.getFirst();
        assertSame("wrong value in message", endpoint, resp.endpoint);
    }
    
    @Test
    public void setFailure() {
        MockComponent component = new MockComponent();
        EndpointFuture future = new EndpointFutureImpl(component);
        future.setFailure();

        assertTrue("future should have been marked as done", future.isDone());
        assertFalse("future should not have been marked as cancelled", future.isCancelled());
        assertEquals("one message should have been delivered to the component", 1, component.messages.size());
        assertEquals("wrong type for message delivered to component", EndpointResponse.class, component.messages.getFirst().getClass());
        EndpointResponse resp = (EndpointResponse)component.messages.getFirst();
        assertSame("wrong value in message", null, resp.endpoint);
    }
}
