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

package com.ibm.mqlight.api.impl.callback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ibm.mqlight.api.callback.CallbackFuture;

public class MockCallbackFuture implements CallbackFuture {

    @Override public boolean cancel(boolean mayInterruptIfRunning) { throw new AssertionError(); }
    @Override public boolean isCancelled() { throw new AssertionError(); }
    @Override public Void get() throws InterruptedException, ExecutionException { throw new AssertionError(); }
    @Override public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException { throw new AssertionError(); }

    protected enum Method { NONE, SUCCESS, FAILURE }
    private final Method expectedMethod;
    private boolean done;
    private Exception exception;
    
    protected MockCallbackFuture(Method expectedMethod) {
        this.expectedMethod = expectedMethod;
    }
    
    @Override public synchronized boolean isDone() {
        return done;
    }
    
    @Override public synchronized void setSuccess() {
        assertEquals("didn't expect setSuccess to be called", expectedMethod, Method.SUCCESS);
        assertFalse("didn't expect setSuccess to be called on a completed future", done);
        done = true;
    }

    @Override
    public synchronized void setFailure(Exception exception) {
        assertEquals("didn't expect setFailure to be called", expectedMethod, Method.FAILURE);
        assertFalse("didn't expect setSuccess to be called on a completed future", done);
        done = true;
        this.exception = exception;
    }
    
    protected Exception getException() {
        return exception;
    }
}
