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

import java.util.EnumSet;

enum NonBlockingClientState {
    Retrying1A, Retrying1B, Retrying1C,
    Retrying2A, Retrying2B, Retrying2C, Retrying2D,
    Started,
    StartingA, StartingB,
    Stopped,
    StoppingA, StoppingB, StoppingC, StoppingD,
    StoppingR1A, StoppingR1B, StoppingR1C, StoppingR1D, StoppingR1E, StoppingR1F,
    StoppingR2A, StoppingR2B, StoppingR2C, StoppingR2D, StoppingR2E, StoppingR2F, StoppingR2G, StoppingR2H,
    StoppingSA, StoppingSB, StoppingSC, StoppingSD;
    
    public static final EnumSet<NonBlockingClientState> acceptingWorkStates = EnumSet.of(Started);
    public static final EnumSet<NonBlockingClientState> queueingWorkStates = 
            EnumSet.of(Retrying1A, Retrying1B, Retrying1C, Retrying2A, Retrying2B, Retrying2C, Retrying2D, StartingA, StartingB);
    public static final EnumSet<NonBlockingClientState> rejectingWorkStates = 
            EnumSet.of(Stopped, StoppingA, StoppingB, StoppingC, StoppingD, StoppingR1A, StoppingR1B, StoppingR1C, StoppingR1D, StoppingR1E, StoppingR1F,
                    StoppingR2A, StoppingR2B, StoppingR2C, StoppingR2D, StoppingR2E, StoppingR2F, StoppingR2G, StoppingR2H, StoppingSA, StoppingSB, StoppingSC, StoppingSD);
}