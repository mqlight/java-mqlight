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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Component {
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    // TODO: should probably log something in NOBODY
    public static final Component NOBODY = new Component() {
        public void tell(Message message, Component self) {}
        @Override protected void onReceive(Message message) {}
    };
    
    private final LinkedList<Message> queue = new LinkedList<>();
    private boolean scheduled = false;
    protected final Object componentMonitor = new Object();
    
    public void tell(Message message, Component self) {
        logger.debug("Telling {}: {}", this, message);
        message.setSender(self);
        boolean execute = false;
        synchronized(queue) {
            queue.addLast(message);
            execute = !scheduled;
            if (execute) scheduled = true;
        }
        if (execute) deliverMessages();
    }
    
    private void deliverMessages() {
        while(true) {
            Message message = null;
            synchronized(queue) {
                if (queue.isEmpty()) {
                    scheduled = false;
                    break;
                }
                else message = queue.removeFirst();
            }
            synchronized(componentMonitor) {
                onReceive(message);
            }
        }
    }
    
    protected abstract void onReceive(Message message);
}
