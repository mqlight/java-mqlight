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

import com.ibm.mqlight.api.impl.logging.LogMarker;

public class TestObjectIdlConverter {

  @Test
  public void testConvert() {

    final ObjectIdConverter converter = new ObjectIdConverter();

    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent()));
    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, null, "message", (Object[]) null)));
    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, null, "message")));
    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, null, "message", new Object[] { null })));
    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, LogMarker.ERROR.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message")));
    assertEquals("Unexpected convertion", "static", converter.convert(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected convertion", "static", converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected convertion", "static", converter.convert(new MockILoggingEvent(null, LogMarker.DATA.getValue(), "message", new Object[] { null })));
    assertEquals("Unexpected convertion", "static", converter.convert(new MockILoggingEvent(null, LogMarker.THROWING.getValue(), "message", new Object[] { null })));

    final String obj = new String("id");
    final String objId = Integer.toHexString(System.identityHashCode(obj));
    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, null, "message", obj)));
    assertEquals("Unexpected convertion", "", converter.convert(new MockILoggingEvent(null, LogMarker.WARNING.getValue(), "message", obj)));
    assertEquals("Unexpected convertion", "@"+objId, converter.convert(new MockILoggingEvent(null, LogMarker.ENTRY.getValue(), "message", obj)));
    assertEquals("Unexpected convertion", "@"+objId, converter.convert(new MockILoggingEvent(null, LogMarker.EXIT.getValue(), "message", obj)));
    assertEquals("Unexpected convertion", "@"+objId, converter.convert(new MockILoggingEvent(null, LogMarker.DATA.getValue(), "message", obj)));
    assertEquals("Unexpected convertion", "@"+objId, converter.convert(new MockILoggingEvent(null, LogMarker.THROWING.getValue(), "message", obj)));
  }
}
