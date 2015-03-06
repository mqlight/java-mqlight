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
package com.ibm.mqlight.api.impl.engine;

import io.netty.buffer.ByteBuf;

import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.impl.Message;

public class SendRequest extends Message {
    protected final EngineConnection connection;
    protected final String topic;
    protected final ByteBuf buf;
    protected final int length;
    protected final QOS qos;
    public SendRequest(EngineConnection connection, String topic, ByteBuf buf, int length, QOS qos) {
        this.connection = connection;
        this.topic = topic;
        this.buf = buf;
        this.length = length;
        this.qos = qos;
    }

}
