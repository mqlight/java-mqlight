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
