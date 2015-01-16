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
        private AtomicBoolean setFailureCalled = new AtomicBoolean(false);;
        private AtomicBoolean setSuccessCalled = new AtomicBoolean(false);;
        
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
