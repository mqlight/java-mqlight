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

/**
 * Bundles information that the client uses when establishing connections
 * to the system hosting the MQ Light server.
 */
public interface Endpoint {

    /**
     * @return the host name of the system to connect to.
     */
    String getHost();
    
    /**
     * @return the port number to connect to.
     */
    int getPort();
    
    /**
     * @return indicates whether an SSL/TLS protected connection should
     *         be used.
     */
    boolean useSsl();
    
    /**
     * @return the user name to use as part of a SASL PLAIN flow used to
     *         authenticate the client.  If this value is <code>null</code>
     *         then the SASL ANONYMOUS mechanism will be used instead.
     */
    String getUser();
    
    /**
     * @return the password to use as part of a SASL PLAIN flow used to
     *         authenticate the client.
     */
    String getPassword();
}
