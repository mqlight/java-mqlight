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
