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

import java.io.File;
import java.io.IOException;

import junit.framework.AssertionFailedError;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ibm.mqlight.api.ClientOptions.SSLOptions;

public class TestEndpointImpl {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    
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

    @Test
    public void goodSSL() throws IOException {
        final File keyStoreFile = folder.newFile();
        final File certFile = folder.newFile();
        final File clientCertFile = folder.newFile();
        final File clientKeyFile = folder.newFile();
        SSLOptions [] sslOptionsList = {
                new SSLOptions(keyStoreFile, "1", null, false, null, null, null),
                new SSLOptions(keyStoreFile, null, null, false, null, null, null),
                new SSLOptions(null, null, certFile, false, null, null, null),
                new SSLOptions(null, null, certFile, false, clientCertFile, clientKeyFile, "2"),        
                new SSLOptions(null, null, certFile, false, clientCertFile, clientKeyFile, null),
                new SSLOptions(null, null, null, false, clientCertFile, clientKeyFile, "2")
        };
        
        for (SSLOptions sslOptions : sslOptionsList) {
            try {
                new EndpointImpl("amqp://example.org", "user", "password", sslOptions);
            } catch (IllegalArgumentException e) {
                throw new AssertionFailedError("EndpointImpl constructor for SSLOptions: "+sslOptions+" should not have thrown an exception");
            }
        }
    }
    
    @Test
    public void badSSL() throws IOException {
        final File keyStoreFile = folder.newFile();
        final File certFile = folder.newFile();
        final File clientCertFile = folder.newFile();
        final File clientKeyFile = folder.newFile();
        final File dirFile = folder.newFolder();
        final File notExists = new File("noexits");
        SSLOptions [] sslOptionsList = {
                new SSLOptions(notExists, "1", null, false, null, null, null),
                new SSLOptions(dirFile, "1", null, false, null, null, null),
                new SSLOptions(keyStoreFile, "1", certFile, false, clientCertFile, clientKeyFile, "2"),
                new SSLOptions(keyStoreFile, "1", certFile, false, null, null, null),
                new SSLOptions(keyStoreFile, "1", null, false, clientCertFile, clientKeyFile, "2"),
                new SSLOptions(null, null, notExists, false, clientCertFile, clientKeyFile, "2"),
                new SSLOptions(null, null, dirFile, false, clientCertFile, clientKeyFile, "2"),
                new SSLOptions(null, null, certFile, false, notExists, clientKeyFile, "2"),
                new SSLOptions(null, null, certFile, false, dirFile, clientKeyFile, "2"),
                new SSLOptions(null, null, certFile, false, clientCertFile, notExists, "2"),
                new SSLOptions(null, null, certFile, false, clientCertFile, dirFile, "2"),
                new SSLOptions(null, null, certFile, false, clientCertFile, null, "2"),
                new SSLOptions(null, null, certFile, false, null, clientKeyFile, "2")
        };
        
        for (SSLOptions sslOptions : sslOptionsList) {
            try {
                new EndpointImpl("amqp://example.org", "user", "password", sslOptions);
                throw new AssertionFailedError("EndpointImpl constructor for SSLOptions: "+sslOptions+" should have thrown an exception");
            } catch (IllegalArgumentException e) {
                // Expected
            }
        }
    }
}
