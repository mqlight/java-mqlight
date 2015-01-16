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

import com.ibm.mqlight.api.Promise;

/**
 * A promise that is used to indicate the outcome of an endpoint lookup
 * operation.  The inherited {@link Promise#setSuccess(Object)}
 * method is used when the lookup operation is successful and returns an
 * {@link Endpoint}.  The inherited {@link Promise#setFailure(Exception)}
 * method is used when the lookup operation fails and the client should
 * transition into stopped state.  The {@link EndpointPromise#setWait(long)}
 * method is used to indicate that the client should wait for a period of
 * time before making more endpoint lookup requests
 */
public interface EndpointPromise extends Promise<Endpoint>{
    
    /**
     * Completes the promise and indicates to the client that it should
     * wait for a period of time before querying the endpoint service again.
     * 
     * @param delay a wait time in milliseconds.
     * @throws IllegalStateException if this method is invoked when the promise
     *                               has already been completed.
     */
    public void setWait(long delay) throws IllegalStateException;
}
