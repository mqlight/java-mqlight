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

package com.ibm.mqlight.api.logging;

/**
 * Interface for trace and information logging.
 * <p>
 * The {@link #setClientId(String)} method can be called to associate a client id for the current thread and its child threads for tracing and logging.
 * <p>
 * The {@code info}, {@code warning}, and {@code error} methods are for general logging, at the respective level.
 * <p>
 * The {@code entry}, {@code exit}, {@code data}, {@code throwing}, methods are for tracing. Each has a number of variants allowing varying numbers of {@link Object} type arguments
 * to be provided. These {@link Object} type arguments will be converted to {@link String}s for logging. For efficiency objects should be passed in the arguments as is to save
 * unnecessary conversion when trace is off (note that with this approach there is no need to use a {@code if (Trace.enabled()))} type test when invoking the trace methods).
 */
public interface Logger {


  /** The client id key, which can be used to record the client id by implementations of the {@link #setClientId(String)} method. */
  String CLIENTID_KEY = "clientid";

  /**
   * Associate the specified client id for the current thread, and its child threads, for tracing and logging
   *
   * @param clientId the client ID to associate.
   */
  void setClientId(String clientId);


  /**
   * Logs an information message.
   *
   * @param message The information message text.
   */
  void info(String message);

  /**
   * Logs a warning.
   *
   * @param message The warning message text.
   */
  void warning(String message);

  /**
   * Logs an error message.
   *
   * @param message The error message text.
   */
  void error(String message);

  /**
   * Logs an error message.
   *
   * @param message The error message text.
   * @param throwable The exception causing the error.
   */
  void error(String message, Throwable throwable);


  /**
   * Method entry tracing for static classes.
   *
   * @param methodName Name of the calling method.
   */
  void entry(String methodName);

  /**
   * Method entry tracing for static classes. Note that this may not be that efficient.
   *
   * @param methodName Name of the calling method.
   * @param objects Objects on which toString() is called.
   */
  void entry(String methodName, Object... objects);

  /**
   * Method entry tracing.
   *
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   */
  void entry(Object source, String methodName);

  /**
   * Method entry tracing. Note that this may not be that efficient.
   *
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   * @param objects Objects on which toString() is called.
   */
  void entry(Object source, String methodName, Object... objects);


  /**
   * Method exit tracing for static classes.
   *
   * @param methodName Name of the calling method.
   */
  void exit(String methodName);

  /**
   * Method exit tracing for static classes.
   *
   * @param methodName Name of the calling method.
   * @param result Object on which toString() is called, containing the return value.
   */
  void exit(String methodName, Object result);

  /**
   * Method exit tracing.
   *
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   */
  void exit(Object source, String methodName);

  /**
   * Method exit tracing.
   *
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   * @param result Object on which toString() is called, containing the return value.
   */
  void exit(Object source, String methodName, Object result);


  /**
   * Method data tracing for static classes.
   *
   * @param methodName Name of the calling method.
   */
  void data(String methodName);

  /**
   * Method data tracing for static classes. Note that this may not be that efficient.
   *
   * @param methodName Name of the calling method.
   * @param objects Objects on which toString() is called.
   */
  void data(String methodName, Object... objects);

  /**
   * Method data tracing.
   *
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   */
  void data(Object source, String methodName);

  /**
   * Method data tracing. Note that this may not be that efficient.
   *
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   * @param objects Objects on which toString() is called.
   */
  void data(Object source, String methodName, Object... objects);


  /**
   * Exception tracing when a throwable is caught in a static class.
   *
   * @param methodName Name of the calling method.
   * @param throwable Causing the event.
   */
  void throwing(String methodName, Throwable throwable);

  /**
   * Exception tracing.
   *
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   * @param throwable Causing the event.
   */
  void throwing(Object source, String methodName, Throwable throwable);

  /**
   * Captures FFDC information.
   *
   * @param methodName Name of the calling method.
   * @param probeId A probe identifier that can be used to uniquely identify the point in the code that requested the data capture. The probe identifier should be unique within a
   *          class.
   * @param throwable The throwable that triggered capturing of FFDC information. This can be null if there is no obvious throwable cause.
   * @param data Arbitrary data that is captured into the FFDC record.
   */
  void ffdc(String methodName, FFDCProbeId probeId, Throwable throwable, Object... data);

  /**
   * Captures FFDC information.
   *
   * @param source Object requesting the FFDC.
   * @param methodName Name of the calling method.
   * @param probeId A probe identifier that can be used to uniquely identify the point in the code that requested the data capture. The probe identifier should be unique within a
   *          class.
   * @param throwable The throwable that triggered capturing of FFDC information. This can be null if there is no obvious throwable cause.
   * @param data Arbitrary data that is captured into the FFDC record.
   */
  void ffdc(Object source, String methodName, FFDCProbeId probeId, Throwable throwable, Object... data);
}

