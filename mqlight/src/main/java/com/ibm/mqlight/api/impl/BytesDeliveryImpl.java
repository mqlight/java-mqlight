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
package com.ibm.mqlight.api.impl;

import java.nio.ByteBuffer;
import java.util.Map;

import com.ibm.mqlight.api.BytesDelivery;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.impl.engine.DeliveryRequest;

class BytesDeliveryImpl extends DeliveryImpl implements BytesDelivery {

    private final ByteBuffer data;
    
    protected BytesDeliveryImpl(NonBlockingClientImpl client, QOS qos, String shareName, String topic, 
                                String topicPattern, long ttl, ByteBuffer data, Map<String, Object> properties, DeliveryRequest req) {
        super(client, qos, shareName, topic, topicPattern, ttl, properties, req);
        this.data = data;
    }
    
    @Override
    public Type getType() {
        return Type.BYTES;
    }

    @Override
    public ByteBuffer getData() {
        return data;
    }

}
