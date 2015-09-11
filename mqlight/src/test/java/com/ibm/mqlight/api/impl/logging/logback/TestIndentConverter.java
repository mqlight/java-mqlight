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

import com.ibm.mqlight.api.impl.logging.LogMarker;

public class TestIndentConverter {

  @Test
  public void testConvert() {

    final IndentConverter converter = new IndentConverter();

    assertEquals("Unexpected convertion", " ", converter.convert(new MockILoggingEvent()));
    assertEquals("Unexpected convertion", " ", converter.convert(new MockILoggingEvent(null, MarkerFactory.getMarker("unknown"), "message")));
    assertEquals("Unexpected convertion", "{", converter.convert(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message")));
    assertEquals("Unexpected convertion", "}", converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message")));
    assertEquals("Unexpected convertion", "d", converter.convert(new MockILoggingEvent(null, LogMarker.DATA.getValue(), "message")));
    assertEquals("Unexpected convertion", "!", converter.convert(new MockILoggingEvent(null, LogMarker.THROWING.getValue(), "message")));
    assertEquals("Unexpected convertion", "i", converter.convert(new MockILoggingEvent(null, LogMarker.INFO.getValue(), "message")));
    assertEquals("Unexpected convertion", "w", converter.convert(new MockILoggingEvent(null, LogMarker.WARNING.getValue(), "message")));
    assertEquals("Unexpected convertion", "e", converter.convert(new MockILoggingEvent(null, LogMarker.ERROR.getValue(), "message")));
    assertEquals("Unexpected convertion", "f", converter.convert(new MockILoggingEvent(null, LogMarker.FFDC.getValue(), "message")));
  }
}
