/*
 *   <copyright 
 *   notice="oco-source" 
 *   pids="5725-P60" 
 *   years="2015" 
 *   crc="1438874957" > 
 *   IBM Confidential 
 *    
 *   OCO Source Materials 
 *    
 *   5724-H72
 *    
 *   (C) Copyright IBM Corp. 2015
 *    
 *   The source code for the program is not published 
 *   or otherwise divested of its trade secrets, 
 *   irrespective of what has been deposited with the 
 *   U.S. Copyright Office. 
 *   </copyright> 
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