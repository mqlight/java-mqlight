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
 * A listener for destination specific events, such as the delivery of messages to the
 * client from the destination.
 */
public interface DestinationListener<T> {

    /**
     * Invoked to deliver a message to the client.
     * @param client the client that this <code>DestinationListener</code> was registered with.
     * @param context the context object that was supplied when this instance of the <code>DestinationListener</code>
     *                was registered with the <code>NonBlockingClient</code>.
     * @param delivery an object that contains both information about the message delivery and the
     *                 payload of the message itself.
     */
    void onMessage(NonBlockingClient client, T context, Delivery delivery);
    
    /**
     * Invoked to deliver a malformed message to the client.  Malformed messages are messages that
     * the MQ Light server cannot convert into an appropriate representation for the client.
     * @param client the client that this <code>DestinationListener</code> was registered with.
     * @param context the context object that was supplied when this instance of the <code>DestinationListener</code>
     *                was registered with the <code>NonBlockingClient</code>.
     * @param delivery an object that contains both information about the message delivery and the
     *                 payload of the message itself.
     */
    void onMalformed(NonBlockingClient client, T context, MalformedDelivery delivery);
    
    /**
     * Invoked to provide a notification that the client is no longer subscribed to a destination
     * that this <code>DestinationListneer</code> has previously being associated with.
     * @param client the client that this <code>DestinationListener</code> was registered with.
     * @param context the context object that was supplied when this instance of the <code>DestinationListener</code>
     *                was registered with the <code>NonBlockingClient</code>.
     * @param topicPattern the topic pattern which identifies the destination that the client is no
     *                     longer subscribed to.
     * @param share the share which identifies the destination that the client is no longer subscribed
     *              to.  This will be <code>null</code> if the destination was private.
     *
     */
    void onUnsubscribed(NonBlockingClient client, T context, String topicPattern, String share);
}
