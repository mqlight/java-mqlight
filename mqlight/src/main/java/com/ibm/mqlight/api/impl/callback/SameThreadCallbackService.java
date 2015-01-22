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

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.callback.CallbackService;

public class SameThreadCallbackService implements CallbackService {
    
    @Override
    public void run(Runnable runnable, Object orderingCtx, Promise<Void> promise) {
        try {
            runnable.run();
            promise.setSuccess(null);
        } catch(Exception e) {
            promise.setFailure(new ClientException("Application code throw an exception from within a callback.  See linked exception for more details.", e));
        }
    }

}
