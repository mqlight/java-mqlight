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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Marker;

import com.ibm.mqlight.api.impl.logging.LogMarker;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * A logback converter to support an indent customer conversion specifier.
 * <p>
 * Note that the {@link #convert(ILoggingEvent)} method simply returns a {@link String} with a single character based on the passed event's marker (if the marker is not recognised
 * or no marker is present, a {@link String} with a single space character is returned).
 * <p>
 * Actual indenting has not been implemented as it will affect trace performance. Using a suitable Boss trace filter it will be possible for Boss trace to format and indent as
 * required anyway, hence indentation is not required by the service team.
 */
public class IndentConverter extends ClassicConverter {

  private static Map<Marker, String> markerMap = new HashMap<>();
  static {
    markerMap.put(LogMarker.INFO.getValue(),     "i");
    markerMap.put(LogMarker.WARNING.getValue(),  "w");
    markerMap.put(LogMarker.ERROR.getValue(),    "e");
    markerMap.put(LogMarker.FFDC.getValue(),     "f");

    markerMap.put(LogMarker.ENTRY.getValue(),    "{");
    markerMap.put(LogMarker.EXIT.getValue(),     "}");
    markerMap.put(LogMarker.DATA.getValue(),     "d");
    markerMap.put(LogMarker.THROWING.getValue(), "!");
  }

  @Override
  public String convert(ILoggingEvent event) {
    final Marker marker = event.getMarker();
    String indent = null;
    if (marker != null) indent = markerMap.get(marker);
    if (indent == null) indent = " ";
    return indent;
  }

}
