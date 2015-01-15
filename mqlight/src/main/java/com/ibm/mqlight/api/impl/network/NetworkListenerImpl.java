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

import java.io.IOException;
import java.nio.ByteBuffer;

import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.network.NetworkChannel;
import com.ibm.mqlight.api.network.NetworkListener;

public class NetworkListenerImpl implements NetworkListener {

    private final Component component;
    
    public NetworkListenerImpl(Component component) {
        this.component = component;
    }

    @Override
    public void onRead(NetworkChannel channel, ByteBuffer buffer) {
        component.tell(new DataRead(channel, buffer), Component.NOBODY);
    }

    @Override
    public void onClose(NetworkChannel channel) {
        component.tell(new ConnectionError(channel, new IOException("Channel closed")), Component.NOBODY);
    }

    @Override
    public void onError(NetworkChannel channel, Exception exception) {
        component.tell(new ConnectionError(channel, exception), Component.NOBODY);
    }

}
