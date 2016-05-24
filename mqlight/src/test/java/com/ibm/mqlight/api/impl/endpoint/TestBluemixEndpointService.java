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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.regex.Pattern;

import org.junit.Test;

import com.ibm.mqlight.api.impl.endpoint.MockEndpointPromise.Method;

public class TestBluemixEndpointService {

    private final String servicesJson = "{\"service\": [ \"amqp://ep1.example.org\", \"amqp://ep2.example.org\" ]}";

    // Example JSON for a user-provided service
    private final String expectedUserProvidedUri = "http://mqlight-lookup.stage1.ng.bluemix.net/Lookup?serviceId=74f27d98-5368-4ae6-aad4-5c23798dec92&tls=true";
    private final String userProvidedJSONUsername =
            "{" +
            "  'user-provided': [" +
            "    {" +
            "      'name': 'Example MQ Light User Provided'," +
            "      'label': 'user-provided', "+
            "      'plan': 'standard'," +
            "      'credentials': {" +
            "        'password': 'r2jk9J?!P~/:', " +
            "        'nonTLSConnectionLookupURI': 'http://mqlight-lookup.stage1.ng.bluemix.net/Lookup?serviceId=74f27d98-5368-4ae6-aad4-5c23798dec92'," +
            "        'version': '2'," +
            "        'connectionLookupURI': '" + expectedUserProvidedUri + "'," +
            "        'username': 'nvAN7VgCXSFS'" +
            "      }" +
            "    }" +
            "  ]" +
            "}".replace("'", "\"");

    private final String userProvidedJSONUser =
            "{" +
            "  'user-provided': [" +
            "    {" +
            "      'name': 'Example MQ Light User Provided'," +
            "      'label': 'user-provided', "+
            "      'plan': 'standard'," +
            "      'credentials': {" +
            "        'password': 'r2jk9J?!P~/:', " +
            "        'nonTLSConnectionLookupURI': 'http://mqlight-lookup.stage1.ng.bluemix.net/Lookup?serviceId=74f27d98-5368-4ae6-aad4-5c23798dec92'," +
            "        'version': '2'," +
            "        'connectionLookupURI': '" + expectedUserProvidedUri + "'," +
            "        'user': 'DkcWc9aSldLs'" +
            "      }" +
            "    }" +
            "  ]" +
            "}".replace("'", "\"");

    private final String userProvidedJSONUserAndUsername =
            "{" +
            "  'user-provided': [" +
            "    {" +
            "      'name': 'Example MQ Light User Provided'," +
            "      'label': 'user-provided', "+
            "      'plan': 'standard'," +
            "      'credentials': {" +
            "        'password': 'r2jk9J?!P~/:', " +
            "        'nonTLSConnectionLookupURI': 'http://mqlight-lookup.stage1.ng.bluemix.net/Lookup?serviceId=74f27d98-5368-4ae6-aad4-5c23798dec92'," +
            "        'version': '2'," +
            "        'connectionLookupURI': '" + expectedUserProvidedUri + "'," +
            "        'username': 'nvAN7VgCXSFS', " +
            "        'user': 'DkcWc9aSldLs'" +
            "      }" +
            "    }" +
            "  ]" +
            "}".replace("'", "\"");


    // Example JSON for two user-provided services, only one of which is MQ Light related
    private final String expectedUserProvidedWithNonMQLightUri = "http://mqlight-lookup.stage1.ng.bluemix.net/Lookup?serviceId=74f27d98-5368-4ae6-aad4-5c23798dec92&tls=true";
    private final String userProvidedWithNonMQLightJSON =
            "{" +
            "  'user-provided': [" +
            "    {" +
            "      'name': 'MQLightBased'," +
            "      'label': 'user-provided', "+
            "      'plan': 'standard'," +
            "      'credentials': {" +
            "        'password': 'r2jk9J?!P~/:', " +
            "        'nonTLSConnectionLookupURI': 'http://mqlight-lookup.stage1.ng.bluemix.net/Lookup?serviceId=74f27d98-5368-4ae6-aad4-5c23798dec92'," +
            "        'version': '2'," +
            "        'connectionLookupURI': '" + expectedUserProvidedWithNonMQLightUri + "'," +
            "        'username': 'nvAN7VgCXSFS'" +
            "      }" +
            "    }," +
            "    {" +
            "      'name': 'CloudantBased'," +
            "      'label': 'user-provided', "+
            "      'plan': 'standard'," +
            "      'credentials': {" +
            "        'couch': 'abcdef'," +
            "        'pass': 'efghij'," +
            "        'user': 'klmnop'" +
            "      }" +
            "    }" +
            "  ]" +
            "}".replace("'", "\"");

    // Example JSON for one application bound to two different instances of the
    // MQ Light service.
    private final String expectedTwoMQLightServicesUri1 = "http://expectedTwoMQLightServicesUri1";
    private final String expectedTwoMQLightServicesUri2 = "http://expectedTwoMQLightServicesUri2";
    private final String twoMQLightServicesJSON =
            "{" +
            "  'mqlight': [" +
            "    {" +
            "      'name': 'MQ Light-9n'," +
            "      'label': 'mqlight'," +
            "      'plan': 'standard'," +
            "      'credentials': {" +
            "        'password': 'r2jk9J?!P~/:', " +
            "        'nonTLSConnectionLookupURI': 'http://mqlight-lookup.stage1.ng.bluemix.net/Lookup?serviceId=74f27d98-5368-4ae6-aad4-5c23798dec92'," +
            "        'version': '2'," +
            "        'connectionLookupURI': '"+ expectedTwoMQLightServicesUri1 + "'," +
            "        'username': 'nvAN7VgCXSFS'" +
            "      }" +
            "    }," +
            "    {" +
            "      'name': 'MQ Light-gs'," +
            "      'label': 'mqlight'," +
            "      'plan': 'standard'," +
            "      'credentials': {" +
            "        'password': 'e2Xrn:^uH[xa'," +
            "        'nonTLSConnectionLookupURI': 'http://mqlight-lookup.stage1.ng.bluemix.net/Lookup?serviceId=021c9e9d-9d77-49bb-b0a6-9d64e04eb444'," +
            "        'version': '2'," +
            "        'connectionLookupURI': '"+ expectedTwoMQLightServicesUri2 + "'," +
            "        'username': 'CmjaVaPqYBa6'" +
            "      }" +
            "    }" +
            "  ]" +
            "}".replace("'", "\"");

    // Example JSON for a service bound against multiple different providers of
    // the MQ Light API.
    private final String expectedMultipleMatchingUri1 = "http://multipleMatchingLabelsJSON/expected1";
    private final String expectedMultipleMatchingUri2 = "http://multipleMatchingLabelsJSON/expected2";
    private final String expectedMultipleMatchingUri3 = "http://multipleMatchingLabelsJSON/expected3";
    private final String multipleMatchingLabelsJSON =
            "{" +
            "  'mqlight': [" +
            "    {" +
            "      'name': 'MQ Light-9n'," +
            "      'label': 'mqlight'," +
            "      'plan': 'standard'," +
            "      'credentials': {" +
            "        'password': 'r2jk9J?!P~/:', " +
            "        'nonTLSConnectionLookupURI': 'http://mqlight-lookup.stage1.ng.bluemix.net/Lookup?serviceId=74f27d98-5368-4ae6-aad4-5c23798dec92'," +
            "        'version': '2'," +
            "        'connectionLookupURI': '" + expectedMultipleMatchingUri1 + "'," +
            "        'username': 'nvAN7VgCXSFS'" +
            "      }" +
            "    }" +
            "  ]," +
            "  'messagehubincubator': [" +
            "    {" +
            "      'name': 'MQ Light-gs'," +
            "      'label': 'messagehubincubator'," +
            "      'plan': 'standard'," +
            "      'credentials': {" +
            "        'password': 'e2rn:^uH[xa'," +
            "        'nonTLSConnectionLookupURI': 'http://mqlight-lookup.stage1.ng.bluemix.net/Lookup?serviceId=021c9e9d-9d77-49bb-b0a6-9d64e04eb444'," +
            "        'version': '2'," +
            "        'connectionLookupURI': '" + expectedMultipleMatchingUri2 + "'," +
            "        'username': 'CmjaVaPqYBa6'" +
            "      }" +
            "    }" +
            "  ]," +
            "  'user-provided': [" +
            "    {" +
            "      'name': 'MQLightBased'," +
            "      'label': 'user-provided', "+
            "      'plan': 'standard'," +
            "      'credentials': {" +
            "        'password': 'r2jk9J?!P~/:', " +
            "        'nonTLSConnectionLookupURI': 'http://mqlight-lookup.stage1.ng.bluemix.net/Lookup?serviceId=74f27d98-5368-4ae6-aad4-5c23798dec92'," +
            "        'version': '2'," +
            "        'connectionLookupURI': '" + expectedMultipleMatchingUri3 + "'," +
            "        'username': 'nvAN7VgCXSFS'" +
            "      }" +
            "    }" +
            "  ]" +
            "}".replace("'", "\"");

    private class MockBluemixEndpointService extends BluemixEndpointService {

        private final String vcapServicesJson;
        private final String expectedUri;
        private final String servicesJson;

        protected MockBluemixEndpointService(String vcapServicesJson, String expectedUri, String servicesJson) {
            super(null, null);
            this.vcapServicesJson = vcapServicesJson;
            this.expectedUri = expectedUri;
            this.servicesJson = servicesJson;
        }

        protected MockBluemixEndpointService(Pattern labelPattern, Pattern namePattern,
                String vcapServicesJson, String expectedUri, String servicesJson) {
            super(labelPattern, namePattern);
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
        MockEndpointPromise promise = new MockEndpointPromise(Method.FAILURE);
        service.lookup(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());
    }

    private void waitForComplete(MockEndpointPromise promise) throws InterruptedException {
        // Promise completed on another thread - need to delay for a reasonable amount of time to allow this to happen.
        for (int i = 0; i < 20; ++i) {
            if (promise.isComplete()) break;
            Thread.sleep(50);
        }
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
        BluemixEndpointService service = new MockBluemixEndpointService(vcapJson, expectedUri, servicesJson);
        MockEndpointPromise promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        waitForComplete(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());

        // Expect 1st endpoint to be returned.
        assertEquals("ep1.example.org", promise.getEndoint().getHost());
        assertEquals("jBruGnaTHuwq", promise.getEndoint().getUser());

        // If the test asks for another endpoint - it should receive the second.
        promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());
        assertEquals("ep2.example.org", promise.getEndoint().getHost());

        // Mark the second endpoint as successful - expect it to be returned again if we ask for another endpoint
        service.onSuccess(promise.getEndoint());
        promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());
        assertEquals("ep2.example.org", promise.getEndoint().getHost());

        // Asking for another endpoint should return the 1st endpoint...
        promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());
        assertEquals("ep1.example.org", promise.getEndoint().getHost());

        // Asking for another endpoint should result in the test being told to wait...
        promise = new MockEndpointPromise(Method.WAIT);
        service.lookup(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());
    }

    // Using the default values (e.g. match all service names) check that the
    // lookup fails when there is ambiguity about which service name to use.
    @Test
    public void ambigiousServiceNameFails() {
        BluemixEndpointService service = new MockBluemixEndpointService(twoMQLightServicesJSON, "", "");
        MockEndpointPromise promise = new MockEndpointPromise(Method.FAILURE);
        service.lookup(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());
    }

    // The service name pattern can be used to disambiguate situations where
    // VCAP_SERVICES contains multiple instances of the MQ Light service
    @Test
    public void specificServiceCanBePickedByServiceName() throws InterruptedException {
        BluemixEndpointService service =
                new MockBluemixEndpointService(null, Pattern.compile(Pattern.quote("MQ Light-9n")),
                        twoMQLightServicesJSON, expectedTwoMQLightServicesUri1, servicesJson);
        MockEndpointPromise promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        waitForComplete(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());

        service =
                new MockBluemixEndpointService(null, Pattern.compile("MQ Light\\-gs"), twoMQLightServicesJSON, expectedTwoMQLightServicesUri2, servicesJson);
        promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        waitForComplete(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());
    }

    // Using the default values (e.g. match service labels containing 'mqlight'
    // and containing 'messagehub') check that the lookup fails when there is
    // ambiguity about which service label to use.
    @Test
    public void ambigiousServiceLabelFails() {
        BluemixEndpointService service = new MockBluemixEndpointService(multipleMatchingLabelsJSON, "", "");
        MockEndpointPromise promise = new MockEndpointPromise(Method.FAILURE);
        service.lookup(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());
    }

    // The service label pattern can be used to disambiguate situations where
    // VCAP_SERVICES contains multiple different services that can match
    @Test
    public void specificServiceCanBePickedByLabelName() throws InterruptedException {
        BluemixEndpointService service =
                new MockBluemixEndpointService(Pattern.compile("mqlight"), null, multipleMatchingLabelsJSON, expectedMultipleMatchingUri1, servicesJson);
        MockEndpointPromise promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        waitForComplete(promise);
        assertTrue("Promise (1) should have been marked done", promise.isComplete());

        service =
                new MockBluemixEndpointService(Pattern.compile("messagehubincubator"), null, multipleMatchingLabelsJSON, expectedMultipleMatchingUri2, servicesJson);
        promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        waitForComplete(promise);
        assertTrue("Promise (2) should have been marked done", promise.isComplete());

        service =
                new MockBluemixEndpointService(Pattern.compile("user-provided"), null, multipleMatchingLabelsJSON, expectedMultipleMatchingUri3, servicesJson);
        promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        waitForComplete(promise);
        assertTrue("Promise (3) should have been marked done", promise.isComplete());
    }

    // Test golden path for 'user-provided' services.
    @Test
    public void userProvidedWorks() throws InterruptedException {
        BluemixEndpointService service = new MockBluemixEndpointService(userProvidedJSONUsername, expectedUserProvidedUri, servicesJson);
        MockEndpointPromise promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        waitForComplete(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());
    }

    // Test that when there are multiple user-provided services (but only one is
    // based on the MQ Light service) that the non-MQ Light related services are
    // ignored.
    @Test
    public void userProvidedIgnoresNonMQLight() throws InterruptedException {
        BluemixEndpointService service = new MockBluemixEndpointService(userProvidedWithNonMQLightJSON, expectedUserProvidedWithNonMQLightUri, servicesJson);
        MockEndpointPromise promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        waitForComplete(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());

        assertEquals("User name", "nvAN7VgCXSFS", promise.getEndoint().getUser());
    }

    // Test that Message Hub services (which use 'user' rather than 'username' in
    // the VCAP_SERVICES) are correctly parsed.
    @Test
    public void goldenPathUserNotUsername() throws InterruptedException {
        String vcapJson =
                "{ \"mqlight\": [ { \"name\": \"mqlsampleservice\", " +
                "\"label\": \"mqlight\", \"plan\": \"default\", " +
                "\"credentials\": { \"user\": \"jBruGnaTHuwq\", " +
                "\"connectionLookupURI\": \"http://mqlightp-lookup.ng.bluemix.net/Lookup?serviceId=ServiceId_0000000090\", " +
                "\"password\": \"xhUQve2gdgAN\", \"version\": \"2\" } } ] }";
        String expectedUri = "http://mqlightp-lookup.ng.bluemix.net/Lookup?serviceId=ServiceId_0000000090";
        BluemixEndpointService service = new MockBluemixEndpointService(vcapJson, expectedUri, servicesJson);
        MockEndpointPromise promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        waitForComplete(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());

        // Expect 1st endpoint to be returned.
        assertEquals("ep1.example.org", promise.getEndoint().getHost());
        assertEquals("jBruGnaTHuwq", promise.getEndoint().getUser());
    }

    // Test that user provided entries can use 'user' as well as 'username'
    @Test
    public void userProvidedWorksWithUserNotUsername() throws InterruptedException {
        BluemixEndpointService service = new MockBluemixEndpointService(userProvidedJSONUser, expectedUserProvidedUri, servicesJson);
        MockEndpointPromise promise = new MockEndpointPromise(Method.SUCCESS);
        service.lookup(promise);
        waitForComplete(promise);
        assertTrue("Promise should have been marked done", promise.isComplete());

        assertEquals("User name", "DkcWc9aSldLs", promise.getEndoint().getUser());
    }

    // Test that user provided endpoints are only valid if they contain _either_
    // a 'user' property, or a 'username' property, not both. As with any user provided
    // endpoints that are not valid, they will be skipped over and ignored.
    @Test
    public void userProvidedIgnoredIfBothUserAndUsernameSpecified() throws InterruptedException {
        BluemixEndpointService service = new MockBluemixEndpointService(userProvidedJSONUserAndUsername, expectedUserProvidedUri, servicesJson);
        MockEndpointPromise promise = new MockEndpointPromise(Method.FAILURE);
        service.lookup(promise);
        waitForComplete(promise);

        assertTrue("Promise should have been marked done", promise.isComplete());
    }
}
