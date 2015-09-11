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
package com.ibm.mqlight.api.network;

import java.nio.ByteBuffer;

import com.ibm.mqlight.api.Promise;

/**
 * Represents an open network connection
 */
public interface NetworkChannel {

    /**
     * Close the connection.
     * @param promise a promise which is to be completed when the connection is closed.
     */
    void close(Promise<Void> promise);

    /**
     * Write data to the network connection.
     * @param buffer contains the data to write.
     * @param promise a promise which is to be completed when the data is written.
     */
    void write(ByteBuffer buffer, Promise<Boolean> promise);

    /**
     * Allows an arbitrary object to be associated with this channel object.
     * @param context
     */
    void setContext(Object context);

    /**
     * Retrieves the value set using {@link NetworkChannel#setContext(Object)}.
     * Returns <code>null</code> if no value has yet been set.
     * @return the context object (if any) set using {@link NetworkChannel#setContext(Object)}
     */
    Object getContext();
}
