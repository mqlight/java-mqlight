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
