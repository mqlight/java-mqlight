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

import java.io.IOException;

import org.junit.Test;

import com.ibm.mqlight.api.impl.endpoint.MockEndpointPromise.Method;

public class TestBluemixEndpointService {

    private class MockBluemixEndpointService extends BluemixEndpointService {
        
        private final String vcapServicesJson;
        private final String expectedUri;
        private final String servicesJson;
        
        protected MockBluemixEndpointService(String vcapServicesJson, String expectedUri, String servicesJson) {
            this.vcapServicesJson = vcapServicesJson;
            this.expectedUri = expectedUri;
            this.servicesJson = servicesJson;
        }
        
        @Override
        protected String getVcapServices() {
            return vcapServicesJson;
        }

        @Override
        protected String hitUri(String httpUri) throws IOException {
            assertEquals("Didn't get a request for the expected URI", expectedUri, httpUri);
            return servicesJson;
        }
    }
    
    @Test
    public void noVcapServices() {
        BluemixEndpointService service = new MockBluemixEndpointService(null, "", "");
        MockEndpointPromise future = new MockEndpointPromise(Method.FAILURE);
        service.lookup(future);
        assertTrue("Future should have been marked done", future.isComplete());
    }
    
    @Test
    public void goldenPath() throws InterruptedException {
        String vcapJson = 
                "{ \"mqlight\": [ { \"name\": \"mqlsampleservice\", " +
                "\"label\": \"mqlight\", \"plan\": \"default\", " +
                "\"credentials\": { \"username\": \"jBruGnaTHuwq\", " +
                "\"connectionLookupURI\": \"http://mqlightp-lookup.ng.bluemix.net/Lookup?serviceId=ServiceId_0000000090\", " +
                "\"password\": \"xhUQve2gdgAN\", \"version\": \"2\" } } ] }";
        String expectedUri = "http://mqlightp-lookup.ng.bluemix.net/Lookup?serviceId=ServiceId_0000000090";
        String servicesJson = "{\"service\": [ \"amqp://ep1.example.org\", \"amqp://ep2.example.org\" ]}";
        BluemixEndpointService service = new MockBluemixEndpointService(vcapJson, expectedUri, servicesJson);
        MockEndpointPromise future = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(future);
        // Future completed on another thread - need to delay for a reasonable amount of time to allow this to happen.
        for (int i = 0; i < 20; ++i) {  
            if (future.isComplete()) break;
            Thread.sleep(50);
        }
        assertTrue("Future should have been marked done", future.isComplete());
        
        // Expect 1st endpoint to be returned.
        assertEquals("ep1.example.org", future.getEndoint().getHost());
        
        // If the test asks for another endpoint - it should receive the second.
        future = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(future);
        assertTrue("Future should have been marked done", future.isComplete());
        assertEquals("ep2.example.org", future.getEndoint().getHost());
        
        // Mark the second endpoint as successful - expect it to be returned again if we ask for another endpoint
        service.onSuccess(future.getEndoint());
        future = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(future);
        assertTrue("Future should have been marked done", future.isComplete());
        assertEquals("ep2.example.org", future.getEndoint().getHost());
        
        // Asking for another endpoint should return the 1st endpoint...
        future = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(future);
        assertTrue("Future should have been marked done", future.isComplete());
        assertEquals("ep1.example.org", future.getEndoint().getHost());
        
        // Asking for another endpoint should result in the test being told to wait...
        future = new MockEndpointPromise(Method.WAIT);
        service.lookup(future);
        assertTrue("Future should have been marked done", future.isComplete());
    }
    
 // TODO: {"service": [ "amqp://ep1.example.org", "amqp://ep2.example.org" ]}
}
