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
 * Used to indicate that the client has been replaced by another instance of itself.  This behaviour
 * occurs when a client connects using the same client ID as an already connected client.  In this case
 * the already connected client is disconnected from the MQ Light server and notified using an
 * instance of this exception.
 */
public class ReplacedException extends ClientException {
    private static final long serialVersionUID = -6033481693545964064L;

    public ReplacedException(String message) {
        super(message);
    }
    
    public ReplacedException(String message, Throwable cause) {
        super(message, cause);
    }
}
