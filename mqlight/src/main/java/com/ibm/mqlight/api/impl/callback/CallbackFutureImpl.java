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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.mqlight.api.callback.CallbackFuture;
import com.ibm.mqlight.api.impl.Component;

public class CallbackFutureImpl implements CallbackFuture {
    private final AtomicBoolean done = new AtomicBoolean(false);
    private final Component component;
    private final boolean ignoreSuccess;
    
    public CallbackFutureImpl(Component component, boolean ignoreSuccess) {
        this.component = component;
        this.ignoreSuccess = ignoreSuccess;
    }
    
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new IllegalStateException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return done.get();
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        throw new IllegalStateException();
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
        throw new IllegalStateException();
    }

    @Override
    public void setSuccess() {
        done.set(true);
        if (!ignoreSuccess) {
            component.tell(new FlushResponse(), Component.NOBODY);
        }
    }

    @Override
    public void setFailure(Exception exception) {
        done.set(true);
        // TODO: need to write code for propagating failure back to the client!
    }
}
