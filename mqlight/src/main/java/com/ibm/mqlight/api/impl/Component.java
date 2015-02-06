/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
