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

import static org.junit.Assert.*;

import org.junit.Test;

import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointService;

public class TestSingleEndpointService {


    @Test
    public void getOneEndpoint() {
        EndpointService service = new SingleEndpointService("amqp://example.org", null, null);
        MockEndpointFuture endpointFuture = new MockEndpointFuture(MockEndpointFuture.Method.SUCCESS);
        service.lookup(endpointFuture);
        
        assertTrue("Expected future to be marked as done", endpointFuture.isDone());
        Endpoint endpoint = endpointFuture.getEndoint();
        assertNotNull("Expected a non-null Endpoint object", endpoint);
        assertEquals("Endpoint hostname does not match", "example.org", endpoint.getHost());
        assertEquals(5672, endpoint.getPort());
        assertNull(endpoint.getUser());
        assertNull(endpoint.getPassword());
        assertFalse(endpoint.useSsl());
    }
    
    @Test
    public void exhaustEndpoints() {
        EndpointService service = new SingleEndpointService("amqp://example.org", null, null);
        MockEndpointFuture[] futures = new MockEndpointFuture[6];
        for (int i = 0; i < futures.length; ++i) { 
            futures[i] = new MockEndpointFuture(i % 2 == 0 ? MockEndpointFuture.Method.SUCCESS : MockEndpointFuture.Method.WAIT);
            service.lookup(futures[i]);
            assertTrue("Expected future " + i + " to be marked as done", futures[i].isDone());
        }
        
        long t1 = futures[1].actualDelay;
        long t2 = futures[3].actualDelay;
        long t3 = futures[5].actualDelay;
        assertTrue("Retry time #1 (" + t1 + ") should be > 0", t1 > 0);
        assertTrue("Retry time #2 (" + t2 + ") should be > time #1 (" + t1 + ")", t2 > t1);
        assertTrue("Retry time #3 (" + t3 + ") should be > time #2 (" + t2 + ")", t3 > t2);
    }
    
    @Test
    public void endpointSuccess() {
        EndpointService service = new SingleEndpointService("amqp://example.org", null, null);
        MockEndpointFuture future1 = new MockEndpointFuture(MockEndpointFuture.Method.SUCCESS);
        MockEndpointFuture future2 = new MockEndpointFuture(MockEndpointFuture.Method.SUCCESS);
        
        service.lookup(future1);
        assertTrue("Expected future1 to be marked done", future1.isDone());
        service.onSuccess(future1.getEndoint());
        
        service.lookup(future2);
        assertTrue("Expected future2 to be marked done", future1.isDone());
        
        assertEquals("Expected both lookups to return the same endpoint", future1.getEndoint(), future2.getEndoint());
    }
    
    

}
