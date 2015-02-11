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

import org.slf4j.MDC;
import org.slf4j.Marker;

import com.ibm.mqlight.api.logging.Logger;

/**
 * A trace/logger implementation that utilizes a SLF4J logger.
 * <p>
 * SLF4J loggers do not have methods to directly trace method entry, exit and exception throwing. But a {@link Marker} can be passed to various methods. Thus we use {@link Maker}s
 * to "mark" calls as entry, exit, throw, etc. It will then be up to the logger to filter and display appropriately based on these markers.
 */
class LoggerImpl implements Logger {
  
  /**
   * The underlying SLFJ logger.
   */
  private final org.slf4j.Logger logger;
  
  /**
   * Constructor.
   * 
   * @param clazz The class associated with this logger instance.
   */
  public LoggerImpl(Class<?> clazz) {
    logger = org.slf4j.LoggerFactory.getLogger(clazz);
  }

  /**
   * Constructor, from an existing {@link org.slf4j.Logger}.
   * 
   * @param logger The SLF4J logger instance.
   */
  public LoggerImpl(org.slf4j.Logger logger) {
    this.logger = logger;
  }
  
  @Override
  public void setClientId(String clientId) {
    MDC.put(CLIENTID_KEY, clientId);
  }

  
  @Override
  public void info(String message) {
    logger.info(LogMarker.INFO.getValue(), message);
  }
  
  @Override
  public void warning(String message) {
    logger.warn(LogMarker.WARNING.getValue(), message);
  }
  
  @Override
  public void error(String message) {
    logger.error(LogMarker.ERROR.getValue(), message);
  }
  
  @Override
  public void error(String message, Throwable throwable) {
    logger.error(LogMarker.ERROR.getValue(), message, throwable);
  }
  
  
  @Override
  public void entry(String methodName) {
    logger.trace(LogMarker.ENTRY.getValue(), methodName, (Object)null);
  }
  
  @Override
  public void entry(String methodName, Object... objects) {
    if (logger.isTraceEnabled()) {
      final Object[] objs = new Object[objects.length + 1];
      objs[0] = null;
      for (int i = 0; i < objects.length; i++)
        objs[i + 1] = objects[i];
      logger.trace(LogMarker.ENTRY.getValue(), methodName, objs);
    }
  }

  @Override
  public void entry(Object source, String methodName) {
    logger.trace(LogMarker.ENTRY.getValue(), methodName, source);
  }

  @Override
  public void entry(Object source, String methodName, Object... objects) {
    if (logger.isTraceEnabled()) {
      final Object[] objs = new Object[objects.length + 1];
      objs[0] = source;
      for (int i = 0; i < objects.length; i++)
        objs[i + 1] = objects[i];
      logger.trace(LogMarker.ENTRY.getValue(), methodName, objs);
    }
  }

  @Override
  public void exit(String methodName) {
    logger.trace(LogMarker.EXIT.getValue(), methodName, (Object)null);
  }

  @Override
  public void exit(String methodName, Object result) {
    logger.trace(LogMarker.EXIT.getValue(), methodName, null, result);
  }

  @Override
  public void exit(Object source, String methodName) {
    logger.trace(LogMarker.EXIT.getValue(), methodName, source);
  }

  @Override
  public void exit(Object source, String methodName, Object result) {
    logger.trace(LogMarker.EXIT.getValue(), methodName, source, result);
  }

  
  @Override
  public void data(String methodName) {
    logger.trace(LogMarker.DATA.getValue(), methodName, (Object)null);
  }

  @Override
  public void data(String methodName, Object... objects) {
    if (logger.isTraceEnabled()) {
      final Object[] objs = new Object[objects.length + 1];
      objs[0] = null;
      for (int i = 0; i < objects.length; i++)
        objs[i + 1] = objects[i];
      logger.trace(LogMarker.DATA.getValue(), methodName, objs);
    }
  }

  @Override
  public void data(Object source, String methodName) {
    logger.trace(LogMarker.DATA.getValue(), methodName, source);
  }
  
  @Override
  public void data(Object source, String methodName, Object... objects) {
    if (logger.isTraceEnabled()) {
      final Object[] objs = new Object[objects.length + 1];
      objs[0] = source;
      for (int i = 0; i < objects.length; i++)
        objs[i + 1] = objects[i];
      logger.trace(LogMarker.DATA.getValue(), methodName, objs);
    }
  }

  @Override
  public void throwing(String methodName, Throwable throwable) {
    // TODO this is invoking the Logger.trace(Marker, String, Object, Object) method so not sure if this will work as intended for all Logger implementations (i.e. the fact that an
    // exception has been thrown may be lost)
    logger.trace(LogMarker.THROWING.getValue(), methodName, null, throwable);
  }

  @Override
  public void throwing(Object source, String methodName, Throwable throwable) {
    // TODO this is invoking the Logger.trace(Marker, String, Object, Object) method so not sure if this will work as intended for all Logger implementations (i.e. the fact that an
    // exception has been thrown may be lost)
    logger.trace(LogMarker.THROWING.getValue(), methodName, source, throwable);
  }
}
