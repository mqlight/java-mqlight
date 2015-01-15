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
 * An enumeration that describes the <em>quality of service</em> used to transfer message
 * data between the client and the MQ Light server.  For more details about qualities of
 * service - please see <a href="https://developer.ibm.com/messaging/mq-light/docs/qos/">https://developer.ibm.com/messaging/mq-light/docs/qos/</a>
 */
public enum QOS {
    /**
     * Attempt to deliver the message at most once.  With this quality of service messages
     * may not be delivered, but they will never be delivered more than once.
     */
    AT_MOST_ONCE, 
    
    /**
     * Attempt to deliver the message at least once.  With this quality of service messages
     * may be delivered more than once, but they will never not be delivered.
     */
    AT_LEAST_ONCE
}
