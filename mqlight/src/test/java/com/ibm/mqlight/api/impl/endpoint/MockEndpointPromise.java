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
import static org.junit.Assert.assertFalse;

import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointPromise;

class MockEndpointPromise implements EndpointPromise {

    protected enum Method { NONE, SUCCESS, WAIT, FAILURE }
    private final MockEndpointPromise.Method expectedMethod;
    private Endpoint actualEndpoint;
    long actualDelay;
    private boolean done;
    
    protected MockEndpointPromise(MockEndpointPromise.Method expectedMethod) {
        this.expectedMethod = expectedMethod;
    }
    
    @Override public synchronized boolean isComplete() {
        return done;
    }
    
    @Override public synchronized void setSuccess(Endpoint endpoint) {
        assertEquals("didn't expect setSuccess to be called", expectedMethod, Method.SUCCESS);
        assertFalse("didn't expect setSuccess to be called on a completed future", done);
        done = true;
        actualEndpoint = endpoint;
    }

    @Override
    public synchronized void setWait(long delay) {
        assertEquals("didn't expect setWait to be called", expectedMethod, Method.WAIT);
        assertFalse("didn't expect setSuccess to be called on a completed future", done);
        done = true;
        actualDelay = delay;
    }

    @Override
    public synchronized void setFailure(Exception exception) {
        assertEquals("didn't expect setFailure to be called", expectedMethod, Method.FAILURE);
        assertFalse("didn't expect setSuccess to be called on a completed future", done);
        done = true;
    }
    
    protected Endpoint getEndoint() { return actualEndpoint; }
    protected long getDelay() { return actualDelay; }
}