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

package com.ibm.mqlight.api.impl.network;

import com.ibm.mqlight.api.impl.Message;

public class ConnectRequest extends Message {
    public final String host;
    public final int port;
    private Object context = null;
    
    public ConnectRequest(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public Object getContext() {
        return context;
    }
    
    public void setContext(Object context) {
        this.context = context;
    }
}
