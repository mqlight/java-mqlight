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
package com.ibm.mqlight.api.endpoint;

import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.Promise;

/**
 * Plug point for endpoint lookup implementations.  The implementation used for an
 * instance of the client can be specified using the
 * {@link NonBlockingClient#create(com.ibm.mqlight.api.endpoint.EndpointService, com.ibm.mqlight.api.callback.CallbackService, com.ibm.mqlight.api.network.NetworkService, com.ibm.mqlight.api.timer.TimerService, com.google.gson.GsonBuilder, com.ibm.mqlight.api.ClientOptions, com.ibm.mqlight.api.NonBlockingClientListener, Object)}
 * method.
 */
public interface EndpointService {

    /**
     * Called by the client each time it needs to make a decision about which endpoint
     * to attempt a connection to.
     * <p>
     * When the operation completes the promise (supplied as an argument) is used to
     * notify the client of the outcome.  If an endpoint is available it will be
     * supplied to the {@link Promise#setSuccess(Object)} method.  If no
     * endpoints are currently available, the client can be advised to wait for a
     * period of time before calling this method again using the {@link EndpointPromise#setWait(long)}
     * method.  Indicating failure by calling the {@link EndpointPromise#setFailure(Exception)}
     * will cause the client to transition into <code>stopped</code> state.
     *
     * @param promise a promise that is to be completed when the endpoint service has
     *                completed the lookup request.
     */
    void lookup(EndpointPromise promise);

    /**
     * Called by the client when it has successfully established a connection to one of
     * the endpoints returned by this service.  This allows the endpoint service to
     * optimise the order in which it returns endpoints (for example to implement an
     * algorithm that always returns the most recently successful endpoints first).
     *
     * @param endpoint
     */
    void onSuccess(Endpoint endpoint);
}
