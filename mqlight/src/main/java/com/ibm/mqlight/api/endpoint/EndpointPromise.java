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

import com.ibm.mqlight.api.Promise;

/**
 * A promise that is used to indicate the outcome of an endpoint lookup
 * operation.  The inherited {@link Promise#setSuccess(Object)}
 * method is used when the lookup operation is successful and returns an
 * {@link Endpoint}.  The inherited {@link Promise#setFailure(Exception)}
 * method is used when the lookup operation fails and the client should
 * transition into stopped state.  The {@link EndpointPromise#setWait(long)}
 * method is used to indicate that the client should wait for a period of
 * time before making more endpoint lookup requests
 */
public interface EndpointPromise extends Promise<Endpoint>{
    
    /**
     * Completes the promise and indicates to the client that it should
     * wait for a period of time before querying the endpoint service again.
     * 
     * @param delay a wait time in milliseconds.
     * @throws IllegalStateException if this method is invoked when the promise
     *                               has already been completed.
     */
    public void setWait(long delay) throws IllegalStateException;
}
