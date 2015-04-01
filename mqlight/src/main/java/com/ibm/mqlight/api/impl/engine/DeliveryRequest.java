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

import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;

import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.impl.Message;

// Note: this is slightly unusual in that it is sent from the Engine to the
//       component that issued the subscribe (rather than most messages in
//       this package which are sent from some Component to the Engine component)
public class DeliveryRequest extends Message {

    public final ByteBuf buf;
    public final QOS qos;
    public final String topicPattern;
    protected final Delivery delivery;
    protected final Connection protonConnection;

    public DeliveryRequest(ByteBuf buf, QOS qos, String topicPattern, Delivery delivery, Connection protonConnection) {
        this.buf = buf;
        this.qos = qos;
        this.topicPattern = topicPattern;
        this.delivery = delivery;
        this.protonConnection = protonConnection;
    }
}
