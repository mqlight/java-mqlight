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
 * A listener for events that occur to an instance of the <code>NonBlockingClient</code>
 */
public interface NonBlockingClientListener<T> {
    
    /**
     * Called as a notification when the client transitions into started state.
     * @param client a reference to the client that the listener was registered for and
     *               this notification pertains to.
     * @param context the context object that was specified when the listener was registered.
     */
    void onStarted(NonBlockingClient client, T context);
    
    /**
     * Called as a notification when the client transitions into stopped state.
     * @param client a reference to the client that the listener was registered for and
     *               this notification pertains to.
     * @param context the context object that was specified when the listener was registered.
     * @param exception indicates why the client transitioned into stopped state.  This will be
     *                  <code>null</code> when the client transitions into stopped state because
     *                  the <code>stop()</code> method is called on the client.
     *                  This exception can be one of the following client type exceptions:
     *                  <ul>
     *                  <li>{@link NetworkException} indicates a non-recoverable network type error.</li>
     *                  <li>{@link SecurityException} indicates a security related error.</li>
     *                  <li>{@link ReplacedException} indicates that the client has been replaced by another instance
     *                      of itself.</li>
     *                  <li>{@link ClientException} a general client type exception, usually caused by another
     *                      exception.</li>
     *                  </ul>
     *                  
     */
    void onStopped(NonBlockingClient client, T context, ClientException exception);

    /**
     * Called as a notification when the client transitions into restarting state.
     * @param client a reference to the client that the listener was registered for and
     *               this notification pertains to.
     * @param context the context object that was specified when the listener was registered.
     */
    void onRestarted(NonBlockingClient client, T context);
    
    /**
     * Called as a notification when the client transitions into retrying state.  Or for
     * each time the client attempts to re-connect to the MQ Light server, if this connection
     * attempt is unsuccessful.
     * @param client a reference to the client that the listener was registered for and
     *               this notification pertains to.
     * @param context the context object that was specified when the listener was registered.
     * @param exception indicates why the client transitioned (or remains) in the retrying state.
     *                  This exception can be one of the following client type exceptions:
     *                  <ul>
     *                  <li>{@link NetworkException} indicates a network type error.</li>
     *                  <li>{@link ClientException} a general client type exception, usually caused by another exception.</li>
     *                  </ul>
     */
    void onRetrying(NonBlockingClient client, T context, ClientException exception);
    
    /**
     * Called as a notification when the client has flushed any buffered messages to the network. This notification
     * can be used in conjunction with the value returned by a {@link NonBlockingClient#send} method to efficiently
     * send messages without buffering a large number of messages in memory allocated by the client.
     * 
     * @param client a reference to the client that the listener was registered for and this notification pertains to.
     * @param context the context object that was specified when the listener was registered.
     */
    void onDrain(NonBlockingClient client, T context);
}