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
package com.ibm.mqlight.api;

/**
 * A listener for completion of <code>NonBlockingClient</code> operations.  For example:
 * <pre>
 * NonBlockingClient client = // ... initialization code
 * 
 * client.send("/kittens", "Hello kitty!", new CompletionListener() {
 *     public void onSuccess(NonBlockingClient c,  // c == client
 *                           Object ctx) {
 *        // ... code for handling success of send operation
 *     }
 *     public void onError(NonBlockingClient c, // c == client
 *                         Object ctx,
 *                         ClientCheckedException exception) {
 *        // ... code for handing failure of send operation - for example:
 *        exception.printStackTrace();  // The reason the operation failed.
 *     }
 * }, null);    // This value is passed into the listener as the context argument...
 * </pre>
 */
public interface CompletionListener<T> {
    
    /**
     * Called to indicate that the operation completed successfully.
     * @param client the client that the listener was registered against.
     * @param context an object that was supplied at the point the listener was
     *                registered.  This allows an application to correlate the
     *                invocation of a listener with the point at which the listener
     *                was registered.
     */
    void onSuccess(NonBlockingClient client, T context);
    
    /**
     * Called to indicate that the operation failed.
     * @param client the client that the listener was registered against.
     * @param context an object that was supplied at the point the listener was
     *                registered.  This allows an application to correlate the
     *                invocation of a listener with the point at which the listener
     *                was registered.
     * @param exception an exception that indicates why the operation failed.
     */
    void onError(NonBlockingClient client, T context, Exception exception);
}
