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
 * An enumeration that describes the <em>quality of service</em> used to transfer message
 * data between the client and the MQ Light server.  For more details about qualities of
 * service - please see <a href="https://developer.ibm.com/messaging/mq-light/docs/qos/">https://developer.ibm.com/messaging/mq-light/docs/qos/</a>
 */
public enum QOS {
    /**
     * Attempt to deliver the message at most once.  With this quality of service messages
     * may not be delivered, but they will never be delivered more than once.
     */
    AT_MOST_ONCE, 
    
    /**
     * Attempt to deliver the message at least once.  With this quality of service messages
     * may be delivered more than once, but they will never not be delivered.
     */
    AT_LEAST_ONCE
}
