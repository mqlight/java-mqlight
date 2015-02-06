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
