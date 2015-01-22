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
