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
 * Superclass of all MQ Light specific client unchecked exceptions
 */
public class ClientRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -7670482333474200402L;

    public ClientRuntimeException(String message) {
        super(message);
    }
    
    public ClientRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
