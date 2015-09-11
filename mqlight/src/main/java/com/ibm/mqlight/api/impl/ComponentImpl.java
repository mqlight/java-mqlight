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

import com.ibm.mqlight.api.logging.FFDCProbeId;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

public abstract class ComponentImpl implements Component {

    private static final Logger logger = LoggerFactory.getLogger(ComponentImpl.class);

    public static final ComponentImpl NOBODY = new ComponentImpl() {
        public void tell(Message message, Component self) {}
        @Override protected void onReceive(Message message) {
          logger.data(this, "tell", message);
        }
    };

    private final LinkedList<Message> queue = new LinkedList<>();
    private boolean scheduled = false;
    protected final Object componentMonitor = new Object();

    public void tell(Message message, Component self) {
        final String methodName = "tell";
        logger.entry(this, methodName, message, self);

        message.setSender(self);
        boolean execute;
        synchronized(queue) {
            queue.addLast(message);
            execute = !scheduled;
            if (execute) scheduled = true;
        }
        if (execute) deliverMessages();

        logger.exit(this, methodName);
    }

    private void deliverMessages() {
        final String methodName = "deliverMessages";
        logger.entry(this, methodName);

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
              try {
                onReceive(message);
              } catch (Throwable e) {
                logger.ffdc(methodName, FFDCProbeId.PROBE_001, e);
              }
            }
        }

        logger.exit(this, methodName);
    }

    protected abstract void onReceive(Message message);
}
