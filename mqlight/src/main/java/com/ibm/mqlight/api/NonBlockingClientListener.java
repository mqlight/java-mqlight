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
     */
    void onRetrying(NonBlockingClient client, T context, ClientException exception);
    
    // TODO: document this!
    void onDrain(NonBlockingClient client, T context);
}