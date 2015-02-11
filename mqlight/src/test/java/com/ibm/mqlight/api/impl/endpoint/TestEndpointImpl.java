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

import org.junit.Test;

public class TestEndpointImpl {

    @Test(expected=IllegalArgumentException.class)
    public void badUriProtocol() {
        new EndpointImpl("mailto://example.org", null, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriExtraProtocol() {
        new EndpointImpl("amqps://amqp://example.org", null, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriHost() {
        new EndpointImpl("amqp://", null, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriMissingPassword1() {
        new EndpointImpl("amqp://user@example.org", null, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriMissingPassword2() {
        new EndpointImpl("amqp://example.org", null, "password");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriInvalidCredentials() {
        new EndpointImpl("amqp://user:password:whats_this@example.org", null, "password");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriMissingUser() {
        new EndpointImpl("amqp://:password@example.org", null, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriDuplicateUser() {
        new EndpointImpl("amqp://user:password:example.org", "user", null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriDuplicatePassword() {
        new EndpointImpl("amqp://user:password:example.org", null, "password");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriDuplicateCredientials() {
        new EndpointImpl("amqp://user:password:example.org", "user", "password");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void badUriItsNotAUri() {
        new EndpointImpl("Hello, My name is Inigo Montoya", null, null);
    }
}
