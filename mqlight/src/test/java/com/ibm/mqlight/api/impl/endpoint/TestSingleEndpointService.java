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
