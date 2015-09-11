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
package com.ibm.mqlight.api.impl.timer;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.AssertionFailedError;
import static org.junit.Assert.*;

import org.junit.Test;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.timer.TimerService;

public class TestTimerService {

    private class MockPromise implements Promise<Void> {
        private AtomicBoolean complete = new AtomicBoolean(false);
        private AtomicBoolean setFailureCalled = new AtomicBoolean(false);
        private AtomicBoolean setSuccessCalled = new AtomicBoolean(false);

        @Override
        public void setFailure(Exception exception) throws IllegalStateException {
            if (complete.getAndSet(true)) throw new IllegalStateException();
            setFailureCalled.set(true);
        }

        @Override
        public void setSuccess(Void result) throws IllegalStateException {
            if (complete.getAndSet(true)) throw new IllegalStateException();
            setSuccessCalled.set(true);
        }

        @Override
        public boolean isComplete() {
            return complete.get();
        }

    }

    @Test
    public void goldenPath() throws InterruptedException {
        TimerService timer = new TimerServiceImpl();
        MockPromise promise = new MockPromise();
        timer.schedule(250, promise);

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 10; ++i) {
            if (promise.isComplete()) break;
            Thread.sleep(50);
        }
        long t2 = System.currentTimeMillis();

        assertTrue("Promise should have completed by now!", promise.isComplete());
        long elapsed = t2 - t1;
        if (elapsed < 150) throw new AssertionFailedError("Promise completed too quickly in " + elapsed +"ms (expected 250ms)");
        assertFalse("Promise should not have been marked as failed", promise.setFailureCalled.get());
    }

    @Test
    public void cancel() throws InterruptedException {
        TimerService timer = new TimerServiceImpl();
        MockPromise promise = new MockPromise();
        timer.schedule(250, promise);
        timer.cancel(promise);

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 10; ++i) {
            if (promise.isComplete()) break;
            Thread.sleep(50);
        }
        long t2 = System.currentTimeMillis();
        assertTrue("Promise should have completed by now!", promise.isComplete());
        long elapsed = t2 - t1;
        if (elapsed > 150) throw new AssertionFailedError("Promise completed too slowly in " + elapsed +"ms (expected < 150ms)");
        assertFalse("Promise should not have been marked as successful", promise.setSuccessCalled.get());
    }

    @Test
    public void cancelCompleted() throws InterruptedException {
        TimerService timer = new TimerServiceImpl();
        MockPromise promise = new MockPromise();
        timer.schedule(50, promise);

        for (int i = 0; i < 5; ++i) {
            if (promise.isComplete()) break;
            Thread.sleep(50);
        }
        assertTrue("Promise should have completed by now!", promise.isComplete());

        timer.cancel(promise);   // Should have no ill effects...
    }
}
