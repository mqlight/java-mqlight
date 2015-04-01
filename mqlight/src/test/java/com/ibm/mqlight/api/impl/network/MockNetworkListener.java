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
package com.ibm.mqlight.api.impl.network;

import io.netty.buffer.ByteBuf;

import java.util.LinkedList;

import com.ibm.mqlight.api.impl.network.Event.Type;
import com.ibm.mqlight.api.network.NetworkChannel;
import com.ibm.mqlight.api.network.NetworkListener;

class MockNetworkListener implements NetworkListener {

    private final LinkedList<Event> events;

    protected MockNetworkListener(LinkedList<Event> events) {
        this.events = events;
    }

    @Override
    public void onRead(NetworkChannel channel, ByteBuf buffer) {
        synchronized(events) {
            events.addLast(new Event(Type.CHANNEL_READ, buffer));
        }
    }

    @Override
    public void onClose(NetworkChannel channel) {
        synchronized(events) {
            events.addLast(new Event(Type.CHANNEL_CLOSE, null));
        }
    }

    @Override
    public void onError(NetworkChannel channel, Exception exception) {
        synchronized(events) {
            events.addLast(new Event(Type.CHANNEL_ERROR, exception));
        }
    }

}