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

package com.ibm.mqlight.api.impl.logging.logback;

import org.slf4j.MDC;

import com.ibm.mqlight.api.logging.Logger;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * A logback converter to support a client id customer conversion specifier.
 * <p>
 * Note that the client id is obtained from the MDC (Mapped Diagnostic Context), which can be set from the {@link Logger#setClientId(String)} method.
 */
public class ClientIdConverter extends ClassicConverter {

  @Override
  public String convert(ILoggingEvent event) {
    final String clientId = MDC.get(Logger.CLIENTID_KEY);
    return clientId == null ? "*" : clientId;
  }

}
