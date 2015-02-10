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

/**
 * A logback converter to support an object id customer conversion specifier.
 * <p>
 * Note that this assumes that whenever the event has one of the trace markers (defined in {@link TraceFilter}) then the first argument for the event specifies the object id, or
 * null implying no object id is appropriate.
 */
package com.ibm.mqlight.api.impl.logging.logback;

import org.slf4j.Marker;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class ObjectIdConverter extends ClassicConverter {
  
  @Override
  public String convert(ILoggingEvent event) {
    final String result;
    final Marker marker = event.getMarker();
    if (TraceFilter.traceMarkerMap.containsKey(marker)) {
      final Object args[] = event.getArgumentArray();
      if (args != null && args.length > 0) {
        Object obj = event.getArgumentArray()[0];
        result = obj == null ? "static" : "@"+Integer.toHexString(System.identityHashCode(obj));
      } else {
        result = "";
      }
     } else {
      result = "";
    }
    return result;
  }

}
