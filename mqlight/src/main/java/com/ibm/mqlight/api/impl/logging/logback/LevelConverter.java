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

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * A logback converter to support a level, based on the marker, customer conversion specifier.
 * <p>
 * When a passed event contains a matrker, the {@link #convert(ILoggingEvent)} method simply returns a {@link String} with text for the marker, otherwise the a {@link String} for the event's level is returned.
 */
public class LevelConverter extends ClassicConverter {
  
  @Override
  public String convert(ILoggingEvent event) {
    final Marker marker = event.getMarker();
    final String result = marker == null ? (event.getLevel() == null ? "" : event.getLevel().toString()) : marker.getName();
    return result;
  }

}
