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

import io.netty.buffer.ByteBuf;

/**
 * A listener for events that occur on a particular network channel.
 */
public interface NetworkListener {

    /**
     * Called when data is read from the network.
     * @param channel identifies which network connection the data was
     *                read from.
     * @param buffer contains the data that has been read.  The buffer belongs
     *               to the implementation of this method - and will not be
     *               altered by the implementation of {@link NetworkService} once
     *               this method has been invoked.
     *               <p>
     *               Once the buffer is finished with the {@link ByteBuf#release()}
     *               method must be called to return the buffer to the pool, otherwise
     *               a memory leak will eventually be reported by the
     *               {@link io.netty.util.ResourceLeakDetector}.
     */
    void onRead(NetworkChannel channel, ByteBuf buffer);
    
    /**
     * Called when the network connection is closed at the remote end.
     * @param channel identifies which network connections was closed.
     */
    void onClose(NetworkChannel channel);
    
    /**
     * Called when an error occurs on the network connection - for example because
     * the process at the remote end of the connection abruptly ends.
     * @param channel identifies which network connection the error relates to.
     * @param exception an exception relating to the error condition.
     */
    void onError(NetworkChannel channel, Exception exception);
}
