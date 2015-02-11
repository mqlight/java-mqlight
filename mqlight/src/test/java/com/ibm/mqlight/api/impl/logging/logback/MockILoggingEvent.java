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

import java.util.Map;

import org.slf4j.MDC;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;

public class MockILoggingEvent implements ILoggingEvent {
  public final Level level;
  public final Marker marker;
  public final String message;
  public final Object [] args;
  
  private boolean  nullMDC = false;
  
  public MockILoggingEvent() {
    this.level = null;
    this.marker = null;
    this.message = null;
    this.args = null;    
  }
  
  public MockILoggingEvent(Level level, Marker marker, String message, Object... args) {
    this.level = level;
    this.marker = marker;
    this.message = message;
    this.args = args;
  }
  
  public void setNullMDC(boolean value) {
    nullMDC = value;
  }
  
  @Override
  public Object[] getArgumentArray() {
    return args;
  }

  @Override
  public StackTraceElement[] getCallerData() {
    return null;
  }

  @Override
  public String getFormattedMessage() {
    return null;
  }

  @Override
  public Level getLevel() {
    return level;
  }

  @Override
  public LoggerContextVO getLoggerContextVO() {
    return null;
  }

  @Override
  public String getLoggerName() {

    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, String> getMDCPropertyMap() {
    return nullMDC ? null : MDC.getCopyOfContextMap();
  }

  @Override
  public Marker getMarker() {
    return marker;
  }

  @Override
  public Map<String, String> getMdc() {
    return null;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public String getThreadName() {
    return null;
  }

  @Override
  public IThrowableProxy getThrowableProxy() {
    return null;
  }

  @Override
  public long getTimeStamp() {
    return 0;
  }

  @Override
  public boolean hasCallerData() {
    return false;
  }

  @Override
  public void prepareForDeferredProcessing() {
  }

}
