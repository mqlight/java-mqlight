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
 * This exception is thrown to indicate that the client is not in the correct state
 * to perform the requested operation.  Examples include:
 * <ul>
 *   <li>Attempting to send, subscribe or unsubscribe while the client is in stopped or
 *       stopping state.</li>
 *   <li>Attempting to subscribe to a destination to which the client is already subscribed.</li>
 *   <li>Attempting to unsubscribe from a destination to which the client is not already
 *       subscribed.</li>
 */
public class StateException extends ClientRuntimeException {

    private static final long serialVersionUID = -8951433512053398231L;

}
