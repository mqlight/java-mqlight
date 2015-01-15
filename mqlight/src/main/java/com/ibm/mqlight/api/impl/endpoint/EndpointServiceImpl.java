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

import com.ibm.mqlight.api.endpoint.EndpointService;

abstract class EndpointServiceImpl implements EndpointService {

    protected long calculateDelay(int retryCount) {
        double upperBound = 1 << (retryCount < 9 ? retryCount : 8);
        double lowerBound = 0.75 * upperBound;
        double jitter = Math.random() * (0.25 * upperBound);
        double interval = Math.min(60000, (lowerBound + jitter) * 1000);
        return Math.round(interval);
    }
}
