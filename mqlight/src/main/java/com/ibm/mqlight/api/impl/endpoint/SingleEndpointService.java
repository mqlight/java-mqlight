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

package com.ibm.mqlight.api.impl.endpoint;

import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointFuture;

public class SingleEndpointService extends EndpointServiceImpl {

    private final Endpoint endpoint;
    boolean exhausted = false;
    int retryCount = 0;
    
    @Override
    public void lookup(EndpointFuture future) {
        if (exhausted) {
            exhausted = false;
            future.setWait(calculateDelay(retryCount++));
        } else {
            exhausted = true;
            future.setSuccess(endpoint);
        }
    }

    @Override
    public void onSuccess(Endpoint endpoint) {
        exhausted = false;
        retryCount = 0;
    }

    public SingleEndpointService(String uri, String user, String password) {
        endpoint = new EndpointImpl(uri, user, password);
    }
}
