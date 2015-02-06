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
package com.ibm.mqlight.api;

import junit.framework.AssertionFailedError;

import org.junit.Test;

public class TestClientOptions {

    @Test
    public void clientIdLength() {
        ClientOptions.builder().setId("012345678901234567890123456789012345678901234567").build();  // 48-char
        ClientOptions.builder().setId("0").build();
        ClientOptions.builder().setId(null).build();

        try {
            ClientOptions.builder().setId("").build();
            throw new AssertionFailedError("Expected a zero-length ID to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }

        try {
            ClientOptions.builder().setId("0123456789012345678901234567890123456789012345678").build();
            throw new AssertionFailedError("Expected a 49 character length ID to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void clientIdValidChars() {
        ClientOptions.builder().setId("abcdefghijklmnopqrstuvwxyz").build();
        ClientOptions.builder().setId("ABCDEFGHIJKLMNOPQRSTUVWXYZ").build();
        ClientOptions.builder().setId("0123456789").build();
        ClientOptions.builder().setId("%./_").build();
        try {
            ClientOptions.builder().setId("badid!").build();
            throw new AssertionFailedError("ID should have been rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }
    }
}
