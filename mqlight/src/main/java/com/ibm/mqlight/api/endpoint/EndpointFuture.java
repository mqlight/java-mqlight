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

package com.ibm.mqlight.api.endpoint;

import java.util.concurrent.Future;

public interface EndpointFuture extends Future<Void>{

    public void setSuccess(Endpoint endpoint);
    
    public void setWait(long delay);
    
    public void setFailure();   // TODO: should this accept an exception as a message?
}
