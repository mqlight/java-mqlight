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

import java.nio.ByteBuffer;

/**
 * A sub-type of delivery that is used to represent binary data being received
 * by the client.
 */
public interface BytesDelivery extends Delivery {
    /**
     * @return a byte buffer containing a message pay-load data.  Logically, this buffer 
     *         <em>belongs</em> to application at the point this object is supplied to the
     *         <code>DeliveryListener</code>.  That is to say that once passed to the
     *         <code>DeliveryListener</code> the client will never modify the data held
     *         in this buffer.
     */
    ByteBuffer getData();
}
