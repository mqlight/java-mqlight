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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.ibm.mqlight.api.callback.CallbackService;
import com.ibm.mqlight.api.impl.callback.MockCallbackPromise.Method;

public class TestThreadPoolCallbackService {

    @Test
    public void successfulCallback() throws InterruptedException {
        CallbackService cbs = new ThreadPoolCallbackService(5);
        MockCallbackPromise promise = new MockCallbackPromise(Method.SUCCESS, false);
        final AtomicBoolean run = new AtomicBoolean(false);
        cbs.run(new Runnable() {
            public void run() {
                run.set(true);
            }
        }, new Object(), promise);
        
        assertTrue("Promise should have been completed", promise.waitForComplete(2500));
        assertTrue("Promise should have been completed successfully", promise.isSuccessful());
        assertTrue("Runnable should have been run!", run.get());
    }
    
    @Test
    public void exceptionThrownInCallback() throws InterruptedException {
        CallbackService cbs = new ThreadPoolCallbackService(5);
        MockCallbackPromise promise = new MockCallbackPromise(Method.FAILURE, false);
        final RuntimeException exception = new RuntimeException();
        cbs.run(new Runnable() {
            public void run() {
                throw exception;
            }
        }, new Object(), promise);

        assertTrue("Promise should have been completed", promise.waitForComplete(2500));
        assertTrue("Promise should not have been completed successfully", !promise.isSuccessful());
        assertSame("Exception should have been thrown from run()!", exception, promise.getException());
    }

    @Test
    public void errorThrownInCallback() throws InterruptedException {
        CallbackService cbs = new ThreadPoolCallbackService(5);
        MockCallbackPromise promise = new MockCallbackPromise(Method.FAILURE, false);
        final Error error = new AssertionError();
        cbs.run(new Runnable() {
            public void run() {
                throw error;
            }
        }, new Object(), promise);

        assertTrue("Promise should have been completed", promise.waitForComplete(2500));
        assertTrue("Promise should not have been completed successfully", !promise.isSuccessful());
        assertSame("Error should have been thrown from run()!", error, promise.getException().getCause());
    }

    static class StressCallbackPromise extends MockCallbackPromise {
        private static final AtomicInteger atomicCount = new AtomicInteger(0);
        private int sequence;
        private final RuntimeException runtimeException = new RuntimeException();
        protected StressCallbackPromise(Method expectedMethod) {
            super(expectedMethod, false);
        }
        protected Runnable getRunnable() {
            if (getExpectedMethod() == Method.SUCCESS) {
                return new Runnable() {
                    @Override
                    public void run() {
                        sequence = atomicCount.getAndIncrement();
                    }
                };
            } else {
                return new Runnable() {
                    @Override
                    public void run() {
                        sequence = atomicCount.getAndIncrement();
                        throw runtimeException;
                    }
                };
            }
        }
        private int getSequence() {
            return sequence;
        }
    }

    @Test
    public void stress() throws InterruptedException {
        final int callbacks = 10000;
        final int orderingContexts = 50;
        final int poolSize = 5;
        final ThreadPoolCallbackService callbackService = new ThreadPoolCallbackService(poolSize);
        
        class PromiseList extends LinkedList<StressCallbackPromise> {
            private static final long serialVersionUID = 2857979802052992226L;
        }
        
        PromiseList[] promiseList = new PromiseList[orderingContexts];
        Object[] contexts = new Object[orderingContexts];
        for (int i = 0; i < orderingContexts; ++i) {
            promiseList[i] = new PromiseList();
            contexts[i] = new Object();
        }

        ArrayList<MockCallbackPromise> allPromises = new ArrayList<>(callbacks);
        Random random = new Random();
        for (int i = 0; i < callbacks; ++i) {
            Method expectedOutcome = Math.abs(random.nextInt()) % 100 > 5 ? Method.SUCCESS : Method.FAILURE;
            StressCallbackPromise promise = new StressCallbackPromise(expectedOutcome);
            int x = Math.abs(random.nextInt()) % orderingContexts;
            PromiseList list = promiseList[x];
            allPromises.add(promise);
            list.addLast(promise);
            callbackService.run(promise.getRunnable(), contexts[x], promise);
        }
        
        int count = 0;
        while(!allPromises.isEmpty()) {
            MockCallbackPromise p = allPromises.remove(0);
            assertTrue("Expected promise #" + count + " to have been completed", p.waitForComplete(1000));
            if (p.getExpectedMethod() == Method.SUCCESS) {
                assertTrue("Promise #" + count + " should have been completed successfully", p.isSuccessful());
            } else {
                assertTrue("Promise #" + count + " should have been completed unsuccessfully", !p.isSuccessful());
            }
            ++count;
        }
        
        // Check within a context promises were completed in order
        for (int i = 0; i < orderingContexts; ++i) {
            if (promiseList[i].size() > 1) {
                int n = promiseList[i].removeFirst().getSequence();
                count = 0;
                while(!promiseList[i].isEmpty()) {
                    int m = promiseList[i].removeFirst().getSequence();
                    assertTrue("Out of order promise completion: i=" + i + " count=" + count + " n=" + n + " m=" + m, n < m);
                    n = m;
                    ++count;
                }
            }
        }
    }
}
