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

import com.ibm.mqlight.api.ClientRuntimeException;
import com.ibm.mqlight.api.impl.logging.LogMarker;

/**
 * A logback converter to support a method arguments customer conversion specifier.
 * <p>
 * By default only the first 1024 bytes of an argument will be written out. This can be changed by defining environment varaible
 * MQLIGHT_JAVA_LOG_TRACE_ARG_MAX_BYTES, to specify a different value.
 * <p>
 * Note that this assumes that whenever the event has one of the trace markers (defined in {@link TraceFilter}) then the first argument for the event specifies the object id, so
 * this is skipped over.
 */
public class ArgsConverter extends ClassicConverter {

  /**
   * Default the maximum length of an argument written to the log to 1024 bytes, allowing an environment variable to
   * be set to specify a different value.
   */
  private static final String ARG_MAX_BYTES_ENVVAR = "MQLIGHT_JAVA_LOG_TRACE_ARG_MAX_BYTES";
  private final static int MAX_ARG_LENGTH;
  static {
    int max = 1024;
    final String maxArgLengthStr = System.getProperty(ARG_MAX_BYTES_ENVVAR);
    if (maxArgLengthStr != null) {
      try {
        max = Integer.parseInt(maxArgLengthStr);
        if (max < 0) {
           throw new ClientRuntimeException("Invalid "+ARG_MAX_BYTES_ENVVAR+" setting. Value must be a positive integer.");
        }
      } catch(NumberFormatException e) {
        throw new ClientRuntimeException("Invalid "+ARG_MAX_BYTES_ENVVAR+" setting. Value must be a positive integer.");
      }
    }

    MAX_ARG_LENGTH = max;
  }

  @Override
  public String convert(ILoggingEvent event) {
    final Marker marker = event.getMarker();
    final Object [] args = event.getArgumentArray();
    final int offset = TraceFilter.traceMarkerMap.containsKey(marker) ? 1 : 0;
    final StringBuilder sb = new StringBuilder();
    if (args != null) {
      if (marker == LogMarker.EXIT.getValue()) {
        if (args.length > offset) {
          sb.append(" returns");
          String arg = args[offset] == null ? null : args[offset].toString();
          if (arg != null && arg.length() > MAX_ARG_LENGTH) arg = arg.substring(0,  MAX_ARG_LENGTH)+"...";
          sb.append(" [");
          sb.append(arg);
          sb.append("]");
        }
      } else {
        for (int i = offset; i < args.length; i++) {
          String arg = args[i] == null ? null : args[i].toString();
          if (arg != null && arg.length() > MAX_ARG_LENGTH) arg = arg.substring(0,  MAX_ARG_LENGTH)+"...";
          sb.append(" [");
          sb.append(arg);
          sb.append("]");
        }
      }
    }
    return sb.toString();
  }

}
