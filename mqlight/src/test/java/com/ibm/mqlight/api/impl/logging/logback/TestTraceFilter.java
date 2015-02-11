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
import org.slf4j.MarkerFactory;

import ch.qos.logback.core.spi.FilterReply;

import com.ibm.mqlight.api.impl.logging.LogMarker;

public class TestTraceFilter {

  @Test
  public void testDecide() {

    final TraceFilter filter = new TraceFilter();

    assertEquals("Unexpected filter response", FilterReply.DENY, filter.decide(new MockILoggingEvent()));
    assertEquals("Unexpected filter response", FilterReply.ACCEPT, filter.decide(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected filter response", FilterReply.ACCEPT, filter.decide(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected filter response", FilterReply.ACCEPT, filter.decide(new MockILoggingEvent(null, LogMarker.DATA.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected filter response", FilterReply.ACCEPT, filter.decide(new MockILoggingEvent(null, LogMarker.THROWING.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected filter response", FilterReply.DENY, filter.decide(new MockILoggingEvent(null, HeaderFilter.HEADER_MARKER, "message", new Object[] { null })));
    assertEquals("Unexpected filter response", FilterReply.DENY, filter.decide(new MockILoggingEvent(null, LogMarker.INFO.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected filter response", FilterReply.DENY, filter.decide(new MockILoggingEvent(null, LogMarker.WARNING.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected filter response", FilterReply.DENY, filter.decide(new MockILoggingEvent(null, LogMarker.ERROR.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected filter response", FilterReply.DENY, filter.decide(new MockILoggingEvent(null, LogMarker.FFDC.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected filter response", FilterReply.DENY, filter.decide(new MockILoggingEvent(null, MarkerFactory.getMarker("unknown"), "message", new Object[] { null })));
  }
}
