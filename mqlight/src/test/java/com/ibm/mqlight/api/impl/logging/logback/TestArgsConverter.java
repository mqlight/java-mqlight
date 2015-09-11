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

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import com.ibm.mqlight.api.impl.logging.LogMarker;

public class TestArgsConverter {

  @Test
  public void testConvertWithUnexpectedEvents() {

    final ArgsConverter converter = new ArgsConverter();

    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent()));
    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message")));
    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message")));
  }

  @Test
  public void testConvertWithExitEvents() {

    final ArgsConverter converter = new ArgsConverter();

    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message", "arg1")));
    assertEquals("Unexpected convertion", " returns [null]", converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message", "arg1", null)));
    assertEquals("Unexpected convertion", " returns [arg2]", converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message", "arg1", "arg2")));
    assertEquals("Unexpected convertion", " returns [arg2]",
        converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message", "arg1", "arg2", "arg3")));
  }

  @Test
  public void testConvertWithNonExitEvents() throws MalformedURLException {

    final ArgsConverter converter = new ArgsConverter();

    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message", "arg1")));
    assertEquals("Unexpected convertion", " [null]", converter.convert(new MockILoggingEvent(null, LogMarker.DATA.getValue(), "message", "arg1", null)));
    assertEquals("Unexpected convertion", " [arg2]", converter.convert(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message", "arg1", "arg2")));
    assertEquals("Unexpected convertion", " [arg2] [123]",
        converter.convert(new MockILoggingEvent(null, LogMarker.DATA.getValue(), "message", "arg1", "arg2", 123)));
    assertEquals("Unexpected convertion", " [45] [foo] [true] [file://tmp/bar.txt]",
        converter.convert(new MockILoggingEvent(null, LogMarker.DATA.getValue(), "message", "arg1", 45, "foo", true, new URL("file://tmp/bar.txt"))));
    assertEquals("Unexpected convertion", " [java.lang.Exception: test exception]",
        converter.convert(new MockILoggingEvent(null, LogMarker.THROWING.getValue(), "message", "arg1", new Exception("test exception"))));
  }

}
