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

package com.ibm.mqlight.api;

/**
 * Represents a promise that the recipient of an instance of this object will
 * perform an operation and then <em>complete</em> the promise by invoking
 * one of the set functions.
 * 
 * @param <T> the type of object passed back when the promise is completed
 *            successfully via the {@link Promise#setSuccess(Object)} method.
 */
public interface Promise<T> {
    
    /**
     * Called to indicate that the related operation failed in some way.
     * @param exception an indication of why the operation failed.
     * @throws IllegalStateException if the promise has already been completed.
     */
    void setFailure(Exception exception) throws IllegalStateException;
    
    /**
     * Called to indicate that the related operation succeeded.
     * @param result an object that represents the result of the operation.
     *               This can be passed back to the class that issued the promise.
     * @throws IllegalStateException if the promise has already been completed.
     */
    void setSuccess(T result) throws IllegalStateException;
    
    /**
     * @return true if the promise has been completed (by either the 
     * {@link Promise#setSuccess(Object)} or the {@link Promise#setFailure(Exception)}
     * method being invoked).
     */
    boolean isComplete();
}
