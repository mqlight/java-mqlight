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
    return events.pop();
  }
  
  
  @Override
  public void debug(String arg0) {
    events.push(new MockEvent("debug", null, arg0));
  }

  @Override
  public void debug(String arg0, Object arg1) {
    events.push(new MockEvent("debug", null, arg0, arg1));
  }

  @Override
  public void debug(String arg0, Object... arg1) {
    events.push(new MockEvent("debug", null, arg0, arg1));
  }

  @Override
  public void debug(String arg0, Throwable arg1) {
    events.push(new MockEvent("debug", null, arg0, arg1));
  }

  @Override
  public void debug(Marker arg0, String arg1) {
    events.push(new MockEvent("debug", arg0, arg1));
  }

  @Override
  public void debug(String arg0, Object arg1, Object arg2) {
    events.push(new MockEvent("debug", null, arg0, arg1, arg2));
  }

  @Override
  public void debug(Marker arg0, String arg1, Object arg2) {
    events.push(new MockEvent("debug", arg0, arg1, arg2));
  }

  @Override
  public void debug(Marker arg0, String arg1, Object... arg2) {
    events.push(new MockEvent("debug", arg0, arg1, arg2));
  }

  @Override
  public void debug(Marker arg0, String arg1, Throwable arg2) {
    events.push(new MockEvent("debug", arg0, arg1, arg2));
  }

  @Override
  public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
    events.push(new MockEvent("debug", arg0, arg1, arg2));
  }

  @Override
  public void error(String arg0) {
    events.push(new MockEvent("error", null, arg0));
  }

  @Override
  public void error(String arg0, Object arg1) {
    events.push(new MockEvent("error", null, arg0, arg1));
  }

  @Override
  public void error(String arg0, Object... arg1) {
    events.push(new MockEvent("error", null, arg0, arg1));
  }

  @Override
  public void error(String arg0, Throwable arg1) {
    events.push(new MockEvent("error", null, arg0, arg1));
  }

  @Override
  public void error(Marker arg0, String arg1) {
    events.push(new MockEvent("error", arg0, arg1));
  }

  @Override
  public void error(String arg0, Object arg1, Object arg2) {
    events.push(new MockEvent("error", null, arg0, arg1, arg2));
  }

  @Override
  public void error(Marker arg0, String arg1, Object arg2) {
    events.push(new MockEvent("error", arg0, arg1, arg2));
  }

  @Override
  public void error(Marker arg0, String arg1, Object... arg2) {
    events.push(new MockEvent("error", arg0, arg1, arg2));
  }

  @Override
  public void error(Marker arg0, String arg1, Throwable arg2) {
    events.push(new MockEvent("error", arg0, arg1, arg2));
  }

  @Override
  public void error(Marker arg0, String arg1, Object arg2, Object arg3) {
    events.push(new MockEvent("error", arg0, arg1, arg2, arg3));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void info(String arg0) {
    events.push(new MockEvent("info", null, arg0));
  }

  @Override
  public void info(String arg0, Object arg1) {
    events.push(new MockEvent("info", null, arg0, arg1));
  }

  @Override
  public void info(String arg0, Object... arg1) {
    events.push(new MockEvent("info", null, arg0, arg1));
  }

  @Override
  public void info(String arg0, Throwable arg1) {
    events.push(new MockEvent("info", null, arg0, arg1));
  }

  @Override
  public void info(Marker arg0, String arg1) {
    events.push(new MockEvent("info", arg0, arg1));
  }

  @Override
  public void info(String arg0, Object arg1, Object arg2) {
    events.push(new MockEvent("info", null, arg0, arg1, arg2));
  }

  @Override
  public void info(Marker arg0, String arg1, Object arg2) {
    events.push(new MockEvent("info", arg0, arg1, arg2));
  }

  @Override
  public void info(Marker arg0, String arg1, Object... arg2) {
    events.push(new MockEvent("info", arg0, arg1, arg2));
  }

  @Override
  public void info(Marker arg0, String arg1, Throwable arg2) {
    events.push(new MockEvent("info", arg0, arg1, arg2));
  }

  @Override
  public void info(Marker arg0, String arg1, Object arg2, Object arg3) {
    events.push(new MockEvent("info", arg0, arg1, arg2, arg3));
  }

  @Override
  public boolean isDebugEnabled() {
    return true;
  }

  @Override
  public boolean isDebugEnabled(Marker arg0) {
    return true;
  }

  @Override
  public boolean isErrorEnabled() {
    return true;
  }

  @Override
  public boolean isErrorEnabled(Marker arg0) {
    return true;
  }

  @Override
  public boolean isInfoEnabled() {
    return true;
  }

  @Override
  public boolean isInfoEnabled(Marker arg0) {
    return true;
  }

  @Override
  public boolean isTraceEnabled() {
    return true;
  }

  @Override
  public boolean isTraceEnabled(Marker arg0) {
    return true;
  }

  @Override
  public boolean isWarnEnabled() {
    return true;
  }

  @Override
  public boolean isWarnEnabled(Marker arg0) {
    return true;
  }

  @Override
  public void trace(String arg0) {
    events.push(new MockEvent("trace", null, arg0));
  }

  @Override
  public void trace(String arg0, Object arg1) {
    events.push(new MockEvent("trace", null, arg0, arg1));
  }

  @Override
  public void trace(String arg0, Object... arg1) {
    events.push(new MockEvent("trace", null, arg0, arg1));
  }

  @Override
  public void trace(String arg0, Throwable arg1) {
    events.push(new MockEvent("trace", null, arg0, arg1));
  }

  @Override
  public void trace(Marker arg0, String arg1) {
    events.push(new MockEvent("trace", arg0, arg1));
  }

  @Override
  public void trace(String arg0, Object arg1, Object arg2) {
    events.push(new MockEvent("trace", null, arg0, arg1, arg2));
  }

  @Override
  public void trace(Marker arg0, String arg1, Object arg2) {
    events.push(new MockEvent("trace", arg0, arg1, arg2));
  }

  @Override
  public void trace(Marker arg0, String arg1, Object... arg2) {
    events.push(new MockEvent("trace", arg0, arg1, arg2));
  }

  @Override
  public void trace(Marker arg0, String arg1, Throwable arg2) {
    events.push(new MockEvent("trace", arg0, arg1, arg2));
  }

  @Override
  public void trace(Marker arg0, String arg1, Object arg2, Object arg3) {
    events.push(new MockEvent("trace", arg0, arg1, arg2, arg3));
  }

  @Override
  public void warn(String arg0) {
    events.push(new MockEvent("warn", null, arg0));
  }

  @Override
  public void warn(String arg0, Object arg1) {
    events.push(new MockEvent("warn", null, arg0, arg1));
  }

  @Override
  public void warn(String arg0, Object... arg1) {
    events.push(new MockEvent("warn", null, arg0, arg1));
  }

  @Override
  public void warn(String arg0, Throwable arg1) {
    events.push(new MockEvent("warn", null, arg0, arg1));
  }

  @Override
  public void warn(Marker arg0, String arg1) {
    events.push(new MockEvent("warn", arg0, arg1));
  }

  @Override
  public void warn(String arg0, Object arg1, Object arg2) {
    events.push(new MockEvent("warn", null, arg0, arg1, arg2));
  }

  @Override
  public void warn(Marker arg0, String arg1, Object arg2) {
    events.push(new MockEvent("warn", arg0, arg1, arg2));
  }

  @Override
  public void warn(Marker arg0, String arg1, Object... arg2) {
    events.push(new MockEvent("warn", arg0, arg1, arg2));
  }

  @Override
  public void warn(Marker arg0, String arg1, Throwable arg2) {
    events.push(new MockEvent("warn", arg0, arg1, arg2));
  }

  @Override
  public void warn(Marker arg0, String arg1, Object arg2, Object arg3) {
    events.push(new MockEvent("warn", arg0, arg1, arg2, arg3));
  }

}