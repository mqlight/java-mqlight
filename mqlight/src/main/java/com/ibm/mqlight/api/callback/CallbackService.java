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
package com.ibm.mqlight.api.callback;

import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.Promise;

/**
 * Plug point for callback executing implementations.  The implementation used for an
 * instance of the client can be specified using the
 * {@link NonBlockingClient#create(com.ibm.mqlight.api.endpoint.EndpointService, com.ibm.mqlight.api.callback.CallbackService, com.ibm.mqlight.api.network.NetworkService, com.ibm.mqlight.api.timer.TimerService, com.google.gson.GsonBuilder, com.ibm.mqlight.api.ClientOptions, com.ibm.mqlight.api.NonBlockingClientListener, Object)}
 * method.
 */
public interface CallbackService {

    /**
     * Run the specified runnable.  This method will be invoked each time the client
     * needs to call back into application code.
     *
     * @param runnable the <code>Runnable</code> to run.
     * @param orderingCtx an object that is used to order the execution of runnables.
     *                    The implementor of this interface must ensure that if two
     *                    calls specify the same <code>orderingCtx</code> object they
     *                    are executed in the order the calls are made.  Two calls that
     *                    specify different values for the <code>orderingCtx</code>
     *                    parameter can have their runnables executed in any order.
     * @param promise a promise which is to be completed when the runnable has finished
     *                executing.
     */
    void run(Runnable runnable, Object orderingCtx, Promise<Void> promise);
}
