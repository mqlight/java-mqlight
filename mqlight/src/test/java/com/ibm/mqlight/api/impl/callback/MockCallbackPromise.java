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
package com.ibm.mqlight.api.impl.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.ibm.mqlight.api.Promise;

public class MockCallbackPromise implements Promise<Void> {

    protected enum Method { NONE, SUCCESS, FAILURE }
    private final Method expectedMethod;
    private final boolean checkMethod;
    private boolean done;
    private boolean success;
    private Exception exception;
    
    protected MockCallbackPromise(Method expectedMethod) {
        this(expectedMethod, true);
    }
    
    protected MockCallbackPromise(Method expectedMethod, boolean checkMethod) {
        this.expectedMethod = expectedMethod;
        this.checkMethod = checkMethod;
    }
    
    @Override public synchronized boolean isComplete() {
        return done;
    }
    
    public synchronized boolean waitForComplete(int timeout) throws InterruptedException {
        if (done) return true;
        else {
            wait(timeout);
            return done;
        }
    }
    
    public synchronized boolean isSuccessful() {
        return success;
    }
    
    @Override public synchronized void setSuccess(Void x) {
        if (checkMethod) {
            assertEquals("didn't expect setSuccess to be called", expectedMethod, Method.SUCCESS);
            assertFalse("didn't expect setSuccess to be called on a completed future", done);
        }
        done = true;
        success = true;
        notifyAll();
    }

    @Override
    public synchronized void setFailure(Exception exception) {
        if (checkMethod) {
            assertEquals("didn't expect setFailure to be called", expectedMethod, Method.FAILURE);
            assertFalse("didn't expect setSuccess to be called on a completed future", done);
        }
        this.exception = exception;
        done = true;
        notifyAll();
    }
    
    protected synchronized Exception getException() {
        return exception;
    }
    
    protected Method getExpectedMethod() {
        return expectedMethod;
    }
}
