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

package com.ibm.mqlight.api.callback;

public interface CallbackService {

    /**
     * Run the specified runnable.
     * 
     * @param runnable the <code>Runnable</code> to run.
     * @param orderingCtx an object that is used to order the execution of runnables.
     *                    The implementor of this interface must ensure that if two
     *                    calls specify the same <code>orderingCtx</code> object they
     *                    are executed in the order the calls are made.  Two calls that
     *                    specify different values for the <code>orderingCtx</code>
     *                    parameter can have their runnables executed in any order.
     * 
     * @param future a future which is to be completed when the runnable has finished
     *               executing.
     */
    void run(Runnable runnable, Object orderingCtx, CallbackFuture future);
}
