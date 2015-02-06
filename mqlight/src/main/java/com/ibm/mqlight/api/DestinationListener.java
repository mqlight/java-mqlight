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
