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
