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

import org.slf4j.Marker;

import com.ibm.mqlight.api.impl.logging.LogMarker;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * A logback converter to support a method arguments customer conversion specifier.
 * <p>
 * Note that this assumes that whenever the event has one of the trace markers (defined in {@link TraceFilter}) then the first argument for the event specifies the object id, so
 * this is skipped over.
 */
public class ArgsConverter extends ClassicConverter {
  
  @Override
  public String convert(ILoggingEvent event) {
    final Marker marker = event.getMarker();
    final Object [] args = event.getArgumentArray();
    final int offset = TraceFilter.traceMarkerMap.containsKey(marker) ? 1 :0;
    final StringBuilder sb = new StringBuilder();
    if (marker == LogMarker.EXIT.getValue() && args.length > offset) sb.append(" returns");
    for (int i=offset; i < args.length; i++) {
      if (args[i] == null) {
        sb.append(" <null>");
      } else {
        sb.append(" ["+args[i]+"]");
      }
    }
    return sb.toString();
  }

}
