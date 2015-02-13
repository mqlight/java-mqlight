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

package com.ibm.mqlight.api.impl.logging;

import java.util.LinkedList;

import org.slf4j.Marker;

class MockLogger implements org.slf4j.Logger {

  private final String name;
  
  private final LinkedList<MockEvent> events = new LinkedList<MockEvent>();
  
  public MockLogger(String name) {
    this.name = name;
  }
  
  public MockEvent getEvent() {
    return events.removeFirst();
  }
  
  
  @Override
  public void debug(String msg) {
    events.addLast(new MockEvent("debug", null, msg));
  }

  @Override
  public void debug(String format, Object arg) {
    events.addLast(new MockEvent("debug", null, format, arg));
  }

  @Override
  public void debug(String format, Object... arguments) {
    events.addLast(new MockEvent("debug", null, format, arguments));
  }

  @Override
  public void debug(String msg, Throwable t) {
    events.addLast(new MockEvent("debug", null, msg, t));
  }

  @Override
  public void debug(Marker marker, String msg) {
    events.addLast(new MockEvent("debug", marker, msg));
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    events.addLast(new MockEvent("debug", null, format, arg1, arg2));
  }

  @Override
  public void debug(Marker marker, String format, Object arg) {
    events.addLast(new MockEvent("debug", marker, format, arg));
  }

  @Override
  public void debug(Marker marker, String format, Object... arguments) {
    events.addLast(new MockEvent("debug", marker, format, arguments));
  }

  @Override
  public void debug(Marker marker, String msg, Throwable t) {
    events.addLast(new MockEvent("debug", marker, msg, t));
  }

  @Override
  public void debug(Marker marker, String format, Object arg1, Object arg2) {
    events.addLast(new MockEvent("debug", marker, format, arg1, arg2));
  }

  @Override
  public void error(String msg) {
    events.addLast(new MockEvent("error", null, msg));
  }

  @Override
  public void error(String format, Object arg) {
    events.addLast(new MockEvent("error", null, format, arg));
  }

  @Override
  public void error(String format, Object... arguments) {
    events.addLast(new MockEvent("error", null, format, arguments));
  }

  @Override
  public void error(String msg, Throwable t) {
    events.addLast(new MockEvent("error", null, msg, t));
  }

  @Override
  public void error(Marker marker, String format) {
    events.addLast(new MockEvent("error", marker, format));
  }

  @Override
  public void error(String format, Object arg1, Object arg2) {
    events.addLast(new MockEvent("error", null, format, arg1, arg2));
  }

  @Override
  public void error(Marker marker, String format, Object arg) {
    events.addLast(new MockEvent("error", marker, format, arg));
  }

  @Override
  public void error(Marker marker, String format, Object... arguments) {
    events.addLast(new MockEvent("error", marker, format, arguments));
  }

  @Override
  public void error(Marker marker, String format, Throwable t) {
    events.addLast(new MockEvent("error", marker, format, t));
  }

  @Override
  public void error(Marker marker, String format, Object arg1, Object arg2) {
    events.addLast(new MockEvent("error", marker, format, arg1, arg2));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void info(String msg) {
    events.addLast(new MockEvent("info", null, msg));
  }

  @Override
  public void info(String format, Object arg) {
    events.addLast(new MockEvent("info", null, format, arg));
  }

  @Override
  public void info(String format, Object... arguments) {
    events.addLast(new MockEvent("info", null, format, arguments));
  }

  @Override
  public void info(String msg, Throwable t) {
    events.addLast(new MockEvent("info", null, msg, t));
  }

  @Override
  public void info(Marker marker, String msg) {
    events.addLast(new MockEvent("info", marker, msg));
  }

  @Override
  public void info(String format, Object arg1, Object arg2) {
    events.addLast(new MockEvent("info", null, format, arg1, arg2));
  }

  @Override
  public void info(Marker marker, String format, Object arg) {
    events.addLast(new MockEvent("info", marker, format, arg));
  }

  @Override
  public void info(Marker marker, String format, Object... arguments) {
    events.addLast(new MockEvent("info", marker, format, arguments));
  }

  @Override
  public void info(Marker marker, String msg, Throwable t) {
    events.addLast(new MockEvent("info", marker, msg, t));
  }

  @Override
  public void info(Marker marker, String format, Object arg1, Object arg2) {
    events.addLast(new MockEvent("info", marker, format, arg1, arg2));
  }

  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return true;
  }

  @Override
  public boolean isErrorEnabled() {
    return true;
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return true;
  }

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return true;
  }

  @Override
  public boolean isTraceEnabled() {
    return true;
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return true;
  }

  @Override
  public boolean isWarnEnabled() {
    return true;
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return true;
  }

  @Override
  public void trace(String msg) {
    events.addLast(new MockEvent("trace", null, msg));
  }

  @Override
  public void trace(String format, Object arg) {
    events.addLast(new MockEvent("trace", null, format, arg));
  }

  @Override
  public void trace(String format, Object... arguments) {
    events.addLast(new MockEvent("trace", null, format, arguments));
  }

  @Override
  public void trace(String msg, Throwable t) {
    events.addLast(new MockEvent("trace", null, msg, t));
  }

  @Override
  public void trace(Marker marker, String msg) {
    events.addLast(new MockEvent("trace", marker, msg));
  }

  @Override
  public void trace(String format, Object arg1, Object arg2) {
    events.addLast(new MockEvent("trace", null, format, arg1, arg2));
  }

  @Override
  public void trace(Marker marker, String format, Object arg) {
    events.addLast(new MockEvent("trace", marker, format, arg));
  }

  @Override
  public void trace(Marker marker, String format, Object... arguments) {
    events.addLast(new MockEvent("trace", marker, format, arguments));
  }

  @Override
  public void trace(Marker marker, String msg, Throwable t) {
    events.addLast(new MockEvent("trace", marker, msg, t));
  }

  @Override
  public void trace(Marker marker, String format, Object arg1, Object arg2) {
    events.addLast(new MockEvent("trace", marker, format, arg1, arg2));
  }

  @Override
  public void warn(String msg) {
    events.addLast(new MockEvent("warn", null, msg));
  }

  @Override
  public void warn(String format, Object arg) {
    events.addLast(new MockEvent("warn", null, format, arg));
  }

  @Override
  public void warn(String format, Object... arguments) {
    events.addLast(new MockEvent("warn", null, format, arguments));
  }

  @Override
  public void warn(String msg, Throwable t) {
    events.addLast(new MockEvent("warn", null, msg, t));
  }

  @Override
  public void warn(Marker marker, String msg) {
    events.addLast(new MockEvent("warn", marker, msg));
  }

  @Override
  public void warn(String format, Object arg1, Object arg2) {
    events.addLast(new MockEvent("warn", null, format, arg1, arg2));
  }

  @Override
  public void warn(Marker marker, String format, Object arg) {
    events.addLast(new MockEvent("warn", marker, format, arg));
  }

  @Override
  public void warn(Marker marker, String format, Object... arguments) {
    events.addLast(new MockEvent("warn", marker, format, arguments));
  }

  @Override
  public void warn(Marker marker, String msg, Throwable t) {
    events.addLast(new MockEvent("warn", marker, msg, t));
  }

  @Override
  public void warn(Marker marker, String format, Object arg1, Object arg2) {
    events.addLast(new MockEvent("warn", marker, format, arg1, arg2));
  }

}