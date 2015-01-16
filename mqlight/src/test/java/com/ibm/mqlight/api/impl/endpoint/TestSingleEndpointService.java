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
        MockEndpointPromise endpointPromise = new MockEndpointPromise(MockEndpointPromise.Method.SUCCESS);
        service.lookup(endpointPromise);
        
        assertTrue("Expected future to be marked as done", endpointPromise.isComplete());
        Endpoint endpoint = endpointPromise.getEndoint();
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
        MockEndpointPromise[] promises = new MockEndpointPromise[6];
        for (int i = 0; i < promises.length; ++i) { 
            promises[i] = new MockEndpointPromise(i % 2 == 0 ? MockEndpointPromise.Method.SUCCESS : MockEndpointPromise.Method.WAIT);
            service.lookup(promises[i]);
            assertTrue("Expected future " + i + " to be marked as done", promises[i].isComplete());
        }
        
        long t1 = promises[1].actualDelay;
        long t2 = promises[3].actualDelay;
        long t3 = promises[5].actualDelay;
        assertTrue("Retry time #1 (" + t1 + ") should be > 0", t1 > 0);
        assertTrue("Retry time #2 (" + t2 + ") should be > time #1 (" + t1 + ")", t2 > t1);
        assertTrue("Retry time #3 (" + t3 + ") should be > time #2 (" + t2 + ")", t3 > t2);
    }
    
    @Test
    public void endpointSuccess() {
        EndpointService service = new SingleEndpointService("amqp://example.org", null, null);
        MockEndpointPromise promise1 = new MockEndpointPromise(MockEndpointPromise.Method.SUCCESS);
        MockEndpointPromise promise2 = new MockEndpointPromise(MockEndpointPromise.Method.SUCCESS);
        
        service.lookup(promise1);
        assertTrue("Expected future1 to be marked done", promise1.isComplete());
        service.onSuccess(promise1.getEndoint());
        
        service.lookup(promise2);
        assertTrue("Expected future2 to be marked done", promise2.isComplete());
        
        assertEquals("Expected both lookups to return the same endpoint", promise1.getEndoint(), promise2.getEndoint());
    }
    
    

}
