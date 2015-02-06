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
package com.ibm.mqlight.api.network;

import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.endpoint.Endpoint;

/**
 * Plug point for network implementations.  The implementation used for an
 * instance of the client can be specified using the
 * {@link NonBlockingClient#create(com.ibm.mqlight.api.endpoint.EndpointService, com.ibm.mqlight.api.callback.CallbackService, com.ibm.mqlight.api.network.NetworkService, com.ibm.mqlight.api.timer.TimerService, com.google.gson.GsonBuilder, com.ibm.mqlight.api.ClientOptions, com.ibm.mqlight.api.NonBlockingClientListener, Object)}
 * method.
 */
public interface NetworkService {

    /**
     * Establish a network connection.
     * @param endpoint provides information about the host, port, etc. to establish the network
     *                 connection to.
     * @param listener a listener that will be notified of network connection related events.
     *                 The listener will only be used if the network connection is successfully
     *                 established.
     * @param promise a promise to complete when the outcome of the network connection attempt is
     *                established.  If the network connection is successfully established then the
     *                {@link Promise#setSuccess(Object)} is passed an implementation of
     *                {@link NetworkChannel} that can be used to send data over the network connection.
     */
    public void connect(Endpoint endpoint, NetworkListener listener, Promise<NetworkChannel> promise);
}
