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
 * unnecessary conversion when trace is off (note that with this approach there is no need to use a {@code if (Trace.enabled())) type test when invoking the trace methods).
 */
public interface Logger {
  
  
  /** The client id key, which can be used to record the client id by implementations of the {@link #setClientId(String)} method. */
  public static final String CLIENTID_KEY = "clientid";
  
  /**
   * Associate the specified client id for the current thread, and its child threads, for tracing and logging
   * 
   * @param clientId
   */
  public abstract void setClientId(String clientId);
  

  /**
   * Logs an information message.
   * 
   * @param message The information message text.
   */
  public abstract void info(String message);
  
  /**
   * Logs a warning.
   * 
   * @param message The warning message text.
   */
  public abstract void warning(String message);
  
  /**
   * Logs an error message.
   * 
   * @param message The error message text.
   */
  public abstract void error(String message);
  
  /**
   * Logs an error message.
   * 
   * @param message The error message text.
   * @param throwable The exception causing the error.
   */
  public abstract void error(String message, Throwable throwable);
  
  
  /**
   * Method entry tracing for static classes.
   * 
   * @param methodName Name of the calling method.
   */
  public abstract void entry(String methodName);
  
  /**
   * Method entry tracing for static classes. Note that this may not be that efficient.
   * 
   * @param methodName Name of the calling method.
   * @param object Objects on which toString() is called.
   */
  public abstract void entry(String methodName, Object... objects);

  /**
   * Method entry tracing.
   * 
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   */
  public abstract void entry(Object source, String methodName);
    
  /**
   * Method entry tracing. Note that this may not be that efficient.
   * 
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   * @param object Objects on which toString() is called.
   */
  public abstract void entry(Object source, String methodName, Object... objects);
  
  
  /**
   * Method exit tracing for static classes.
   * 
   * @param methodName Name of the calling method.
   */
  public abstract void exit(String methodName);

  /**
   * Method exit tracing for static classes.
   * 
   * @param methodName Name of the calling method.
   * @param object Object on which toString() is called, containing the return value.
   */
  public abstract void exit(String methodName, Object result);

  /**
   * Method exit tracing.
   * 
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   */
  public abstract void exit(Object source, String methodName);

  /**
   * Method exit tracing.
   * 
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   * @param object Object on which toString() is called, containing the return value.
   */
  public abstract void exit(Object source, String methodName, Object result);
  
  
  /**
   * Method data tracing for static classes.
   * 
   * @param methodName Name of the calling method.
   */
  public abstract void data(String methodName);

  /**
   * Method data tracing for static classes. Note that this may not be that efficient.
   * 
   * @param methodName Name of the calling method.
   * @param object Objects on which toString() is called.
   */
  public abstract void data(String methodName, Object... objects);

  /**
   * Method data tracing.
   * 
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   */
  public abstract void data(Object source, String methodName);
  
  /**
   * Method data tracing. Note that this may not be that efficient.
   * 
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   * @param object Objects on which toString() is called.
   */
  public abstract void data(Object source, String methodName, Object... objects);
  
  
  /**
   * Exception tracing when a throwable is caught in a static class.
   * 
   * @param methodName Name of the calling method.
   * @param throwable Causing the event.
   */
  public abstract void throwing(String methodName, Throwable throwable);

  /**
   * Exception tracing.
   * 
   * @param source Object making the trace call.
   * @param methodName Name of the calling method.
   * @param throwable Causing the event.
   */
  public abstract void throwing(Object source, String methodName, Throwable throwable);
}

