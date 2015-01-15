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

package com.ibm.mqlight.api;

/**
 * An abstract adapter class for receiving destination events. The methods in this class are empty.  
 * This class exists as convenience for creating destination listener objects.  Extend this class to
 * create a DestinationListener object and override the methods for the events of interest. 
 * (If you implement the DestinationListener interface, you have to define all of the methods in it.
 * This abstract class defines null methods for them all, so you can only have to define methods for
 * events you care about.)
 */
public abstract class DestinationAdapter<T> implements DestinationListener<T> {

    @Override
    public void onMessage(NonBlockingClient client, T context, Delivery delivery) {}

    @Override
    public void onMalformed(NonBlockingClient client, T context, MalformedDelivery delivery) {}

    @Override
    public void onUnsubscribed(NonBlockingClient client, T context, String topicPattern, String share) {}
}
