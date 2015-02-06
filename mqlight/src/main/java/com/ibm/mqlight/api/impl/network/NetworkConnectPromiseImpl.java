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

import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.network.NetworkChannel;

public class NetworkConnectPromiseImpl implements Promise<NetworkChannel> {
    
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final Component component;
    private final Object context;
    
    public NetworkConnectPromiseImpl(Component component, Object context) {
        this.component = component;
        this.context = context;
    }

    @Override
    public boolean isComplete() {
        return complete.get();
    }

    private NetworkChannel channel;
    
    @Override
    public void setSuccess(NetworkChannel channel) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        } else {
            synchronized(this) {
                this.channel = channel;
            }
            component.tell(new ConnectResponse(channel, null, context), Component.NOBODY);
        }
    }

    @Override
    public void setFailure(Exception exception) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise already completed");
        } else {
            ClientException clientException;
            if (exception instanceof ClientException) {
                clientException = (ClientException)exception;
            } else {
                clientException = new ClientException("The network operation failed.  See linked exception for more information", exception);
            }
            component.tell(new ConnectResponse(getChannel(), clientException, context), Component.NOBODY);
        }
    }
    
    public synchronized NetworkChannel getChannel() {
        return channel;
    }
    
    public Object getContext() {
        return context;
    }
}
