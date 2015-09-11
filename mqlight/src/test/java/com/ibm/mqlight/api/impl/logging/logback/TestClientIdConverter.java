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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.MDC;

import com.ibm.mqlight.api.impl.logging.LogMarker;
import com.ibm.mqlight.api.logging.Logger;

public class TestClientIdConverter {

  @Test
  public void testConvertWithClientIdNotSet() {
    MDC.clear();

    final ClientIdConverter converter = new ClientIdConverter();

    assertEquals("Unexpected convertion", "*", converter.convert(new MockILoggingEvent()));
    assertEquals("Unexpected convertion", "*", converter.convert(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message")));
    assertEquals("Unexpected convertion", "*", converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message")));

    MockILoggingEvent event = new MockILoggingEvent();
    event.setNullMDC(true);
    assertEquals("Unexpected convertion", "*", converter.convert(new MockILoggingEvent()));
  }

  @Test
  public void testConvertWithClientIdSetToNull() {
    MDC.put(Logger.CLIENTID_KEY, null);
    final ClientIdConverter converter = new ClientIdConverter();

    assertEquals("Unexpected convertion", "*", converter.convert(new MockILoggingEvent()));
    assertEquals("Unexpected convertion", "*", converter.convert(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message")));
    assertEquals("Unexpected convertion", "*", converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message")));
  }

  @Test
  public void testConvertWithClientIdSetBlank() {
    MDC.put(Logger.CLIENTID_KEY, "");
    final ClientIdConverter converter = new ClientIdConverter();

    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent()));
    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message")));
    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message")));
  }

  @Test
  public void testConvertWithClientIdSet() {
    MDC.put(Logger.CLIENTID_KEY, "id");
    final ClientIdConverter converter = new ClientIdConverter();

    assertEquals("Unexpected convertion", "id", converter.convert(new MockILoggingEvent()));
    assertEquals("Unexpected convertion", "id", converter.convert(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message")));
    assertEquals("Unexpected convertion", "id", converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message")));
  }

}
