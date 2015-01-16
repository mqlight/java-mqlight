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

package com.ibm.mqlight.api.impl.network;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.AssertionFailedError;

import com.ibm.mqlight.api.Promise;

class MockNetworkWritePromise implements Promise<Boolean> {
    private AtomicBoolean done = new AtomicBoolean(false);

    @Override
    public boolean isComplete() {
        // TODO Auto-generated method stub
        return done.get();
    }

    @Override
    public void setSuccess(Boolean drained) {
        done.set(true);
    }

    @Override
    public void setFailure(Exception exception) {
        done.set(true);
        throw new AssertionFailedError("setFailure should never be called!");
    }
}