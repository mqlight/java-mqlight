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

package com.ibm.mqlight.api.network;

import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.endpoint.Endpoint;

/**
 * Plug point for network implementations.  The implementation used for an
 * instance of the client can be specified using the
 * {@link NonBlockingClient#create(com.ibm.mqlight.api.endpoint.EndpointService, com.ibm.mqlight.api.callback.CallbackService, com.ibm.mqlight.api.network.NetworkService, com.ibm.mqlight.api.timer.TimerService, com.google.gson.GsonBuilder, com.ibm.mqlight.api.ClientOptions, com.ibm.mqlight.api.NonBlockingClientListener, Object)}
 * method.
 */
public interface NetworkService {

    /**
     * Establish a network connection.
     * @param endpoint provides information about the host, port, etc. to establish the network
     *                 connection to.
     * @param listener a listener that will be notified of network connection related events.
     *                 The listener will only be used if the network connection is successfully
     *                 established.
     * @param promise a promise to complete when the outcome of the network connection attempt is
     *                established.  If the network connection is successfully established then the
     *                {@link Promise#setSuccess(Object)} is passed an implementation of
     *                {@link NetworkChannel} that can be used to send data over the network connection.
     */
    public void connect(Endpoint endpoint, NetworkListener listener, Promise<NetworkChannel> promise);
}
