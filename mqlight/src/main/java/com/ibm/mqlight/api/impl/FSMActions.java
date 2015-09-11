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

public interface FSMActions {

    void startTimer();

    void openConnection();

    void closeConnection();

    void cancelTimer();

    void requestEndpoint();

    /**
     * This action remakes the client's subscriptions.
     * <p>
     * The action is invoked whenever a connection is recovered.
     */
    void remakeInboundLinks();

    void blessEndpoint();

    /**
     * This action performs the cleanup operations such as flushing/failing any pending sends or subscribes.
     * <p>
     * The action can be invoked when a client is stopping.
     */
    void cleanup();

    void failPendingStops();

    void succeedPendingStops();

    void failPendingStarts();

    void succeedPendingStarts();

    // All of these relate to external state machine transitions
    void eventStarting();
    void eventUserStopping();
    void eventSystemStopping();
    void eventStopped();
    void eventStarted();
    void eventRetrying();
    void eventRestarted();

    /**
     * This action breaks any pending sends and subscription requests from the client
     * <p>
     * The action is invoked whenever a network error occurs and the client is not stopping.
     */
    void breakInboundLinks();

    void processQueuedActions();

}
