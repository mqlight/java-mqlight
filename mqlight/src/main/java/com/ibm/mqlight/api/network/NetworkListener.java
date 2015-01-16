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

/**
 * A listener for events that occur on a particular network channel.
 */
public interface NetworkListener {

    /**
     * Called when data is read from the network.
     * @param channel identifies which network connection the data was
     *                read from.
     * @param buffer contains the data that has been read.  The buffer belongs
     *               to the implementation of this method - and will not be
     *               altered by the implementation of {@link NetworkService} once
     *               this method has been invoked.
     */
    void onRead(NetworkChannel channel, ByteBuffer buffer);
    
    /**
     * Called when the network connection is closed at the remote end.
     * @param channel identifies which network connections was closed.
     */
    void onClose(NetworkChannel channel);
    
    /**
     * Called when an error occurs on the network connection - for example because
     * the process at the remote end of the connection abruptly ends.
     * @param channel identifies which network connection the error relates to.
     * @param exception an exception relating to the error condition.
     */
    void onError(NetworkChannel channel, Exception exception);
}
