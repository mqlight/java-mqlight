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
 * This exception is thrown to indicate that the client is not in the correct state
 * to perform the requested operation.  Examples include:
 * <ul>
 *   <li>Attempting to send, subscribe or unsubscribe while the client is in stopped or
 *       stopping state.</li>
 *   <li>Attempting to subscribe to a destination to which the client is already subscribed.</li>
 *   <li>Attempting to unsubscribe from a destination to which the client is not already
 *       subscribed.</li>
 */
public class StateException extends ClientRuntimeException {

    private static final long serialVersionUID = -8951433512053398231L;

    public StateException(String message) {
        super(message);
    }

    public StateException(String message, Throwable cause) {
        super(message, cause);
    }

}
