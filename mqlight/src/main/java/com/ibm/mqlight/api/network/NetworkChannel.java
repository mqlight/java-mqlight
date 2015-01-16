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

package com.ibm.mqlight.api.network;

import java.nio.ByteBuffer;

import com.ibm.mqlight.api.Promise;

/**
 * Represents an open network connection
 */
public interface NetworkChannel {

    /**
     * Close the connection.
     * @param promise a promise which is to be completed when the connection is closed.
     */
    void close(Promise<Void> promise);
    
    /**
     * Write data to the network connection.
     * @param buffer contains the data to write.
     * @param promise a promise which is to be completed when the data is written.
     */
    void write(ByteBuffer buffer, Promise<Boolean> promise);
    
    /**
     * Allows an arbitrary object to be associated with this channel object.
     * @param context
     */
    public void setContext(Object context);
    
    /**
     * Retrieves the value set using {@link NetworkChannel#setContext(Object)}.
     * Returns <code>null</code> if no value has yet been set.
     * @return the context object (if any) set using {@link NetworkChannel#setContext(Object)}
     */
    public Object getContext();
}
