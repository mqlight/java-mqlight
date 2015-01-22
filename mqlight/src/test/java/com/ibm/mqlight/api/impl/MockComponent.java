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

package com.ibm.mqlight.api.impl;

import java.util.LinkedList;

import junit.framework.AssertionFailedError;

import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.impl.Message;

public class MockComponent extends Component {
    private LinkedList<Message> messages = new LinkedList<>();
    
    @Override protected void onReceive(Message message) {
        throw new AssertionFailedError("onReceive should not have been called");
    }
    
    @Override public synchronized void tell(Message message, Component self) {
        messages.add(message);
    }
    
    public synchronized LinkedList<Message> getMessages() {
        return messages;
    }
}