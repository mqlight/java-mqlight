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

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import org.junit.Test;

import com.ibm.mqlight.api.callback.CallbackService;
import com.ibm.mqlight.api.impl.callback.MockCallbackPromise.Method;

public class TestSameThreadCallbackService {
    
    @Test
    public void successfulCallback() {
        CallbackService cbs = new SameThreadCallbackService();
        MockCallbackPromise future = new MockCallbackPromise(Method.SUCCESS);
        final AtomicBoolean run = new AtomicBoolean(false);
        cbs.run(new Runnable() {
            public void run() {
                run.set(true);
            }
        }, new Object(), future);
        
        assertTrue("Runnable should have been run!", run.get());
    }
    
    @Test
    public void exceptionThrownInCallback() {
        CallbackService cbs = new SameThreadCallbackService();
        MockCallbackPromise future = new MockCallbackPromise(Method.FAILURE);
        final RuntimeException exception = new RuntimeException();
        cbs.run(new Runnable() {
            public void run() {
                throw exception;
            }
        }, new Object(), future);
        
        assertSame("Exception should have been thrown from run()!", exception, future.getException().getCause());
    }
}
