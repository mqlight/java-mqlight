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

    public void startTimer();
    
    public void openConnection();
    
    public void closeConnection();
    
    public void cancelTimer();
    
    public void requestEndpoint();
    
    public void remakeInboundLinks();  // TODO: AKA "remake subscriptions"
    
    public void blessEndpoint();
    
    public void cleanup();  // TODO: AKA "fail outbound, wait for inbound"
    
    public void failPendingStops();
    
    public void succeedPendingStops();
    
    public void failPendingStarts();
    
    public void succeedPendingStarts();
    
    // TODO: all of these relate to external state machine transitions...
    public void eventStarting();
    public void eventUserStopping();
    public void eventSystemStopping();
    public void eventStopped();
    public void eventStarted();
    public void eventRetrying();
    public void eventRestarted();

    // TODO: AKA "break subs"
    public void breakInboundLinks();
    
    public void processQueuedActions();

}
