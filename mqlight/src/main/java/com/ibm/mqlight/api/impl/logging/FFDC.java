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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;

import com.ibm.mqlight.api.logging.FFDCProbeId;

class FFDC {

  /** Character sequence to use as the line separator */
  private static final String lineSeparator = System.getProperty("line.separator");
  
  /**
   * Captures FFDC information.
   * 
   * @param logger Logger for identifying caller, and for outputting FFDC information to.
   * @param methodName Name of the calling method.
   * @param probeId A probe identifier that can be used to uniquely identify the point in the code that requested the data capture. The probe identifier should be unique within a
   *          class.
   * @param throwable The throwable that triggered capturing of FFDC information. This can be null if there is no obvious throwable cause.
   * @param data Arbitrary data that is captured into the FFDC record.
   */
  public static void capture(Logger logger, String methodName, FFDCProbeId probe, Throwable throwable, Object... data) {
    capture(logger, null, methodName, probe, throwable, data);
  }

  /**
   * Captures FFDC information.
   * 
   * @param logger Logger for identifying caller, and for outputting FFDC information to.
   * @param callingObject Object requesting the FFDC.
   * @param methodName Name of the calling method.
   * @param probeId A probe identifier that can be used to uniquely identify the point in the code that requested the data capture. The probe identifier should be unique within a
   *          class.
   * @param throwable The throwable that triggered capturing of FFDC information. This can be null if there is no obvious throwable cause.
   * @param data Arbitrary data that is captured into the FFDC record.
   */
  public static void capture(Logger logger, Object callingObject, String methodName, FFDCProbeId probeId, Throwable throwable, Object[] data) {
    long ffdcTimestamp = System.currentTimeMillis();
    final String className = logger.getName();
    final StringBuilder sb = new StringBuilder();
    
    sb.append("Level:      ");
    sb.append(Version.getVersion());
    sb.append(lineSeparator);
    
    final SimpleDateFormat recordFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS z");
    sb.append("Time:       ");
    sb.append(recordFormatter.format(ffdcTimestamp));
    sb.append(lineSeparator);
            
    Thread currentThread = Thread.currentThread();
    sb.append("Thread:     ");
    sb.append(currentThread.getId());
    sb.append(" (");
    sb.append(currentThread.getName());
    sb.append(")");
    sb.append(lineSeparator);
            
    if (className != null) {
      sb.append("Class:      ");
      sb.append(className);
      sb.append(lineSeparator);
    }
            
    if (callingObject != null) {
      sb.append("Instance:   ");
      sb.append(Integer.toHexString(System.identityHashCode(callingObject)));
      sb.append(lineSeparator);
    }
            
    if (methodName != null) {
      sb.append("Method:     ");
      sb.append(methodName);
      sb.append(lineSeparator);
    }
            
    sb.append("Probe:      ");
    sb.append(probeId == null ? "null" : probeId);
    sb.append(lineSeparator);
    
    sb.append("Cause:      ");
    sb.append(throwable == null ? "null" : throwable.toString());
    sb.append(lineSeparator);
    
    try {
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      PrintStream stream = new PrintStream(byteOut, false, "UTF-8");
      final Throwable displayThrowable;
      if (throwable != null) {
        displayThrowable = throwable;
      } else {
        displayThrowable = new Exception("Call stack");
      }
      displayThrowable.printStackTrace(stream);
      stream.flush();
      sb.append(byteOut.toString("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      logger.error("Failed to generate FFDC call stack. Reason: ", e.getLocalizedMessage());
    }
    
    if (data != null) {
      for (int i = 0; i < data.length; ++i) {
        Object item = data[i];
        sb.append("Arg[");
        sb.append(i);
        sb.append("]:     ");
        sb.append(item);
        sb.append(lineSeparator);
      }
    }
    
    for (Thread thread : Thread.getAllStackTraces().keySet()) {
      sb.append(getThreadInfo(thread));
    }
        
    logger.error(LogMarker.FFDC.getValue(), sb.toString());
    
    // Additionally output a javacore (to capture the full information to file)
    try {
      final String javacoreFilePath = Javacore.generateJavaCore();
      logger.info("Javacore diagnostic information written to: "+javacoreFilePath);
    } catch (Throwable e) {
      logger.error("Failed to generate a javacore. Reason: {}", e.getLocalizedMessage());
    }
  }

  /**
   * Gets and formats the specified thread's information.
   * 
   * @param thread The thread to obtain the information from.
   * @return A formatted string for the thread information.
   */
  private static String getThreadInfo(Thread thread) {
    final StringBuilder sb = new StringBuilder();

    sb.append("Thread:     ");
    sb.append(thread.getId());
    sb.append(" (");
    sb.append(thread.getName());
    sb.append(")");
    sb.append(lineSeparator);
    final StackTraceElement[] stack = thread.getStackTrace();
    if (stack.length == 0) {
      sb.append("        No Java callstack associated with this thread");
      sb.append(lineSeparator);
    } else {
      for (StackTraceElement element : stack) {
        sb.append("        at ");
        sb.append(element.getClassName());
        sb.append(".");
        sb.append(element.getMethodName());
        sb.append("(");
        final int lineNumber = element.getLineNumber();
        if (lineNumber == -2) {
          sb.append("Native Method");
        } else if (lineNumber >=0) {
          sb.append(element.getFileName());
          sb.append(":");
          sb.append(element.getLineNumber());
        } else {
          sb.append(element.getFileName());        
        }
        sb.append(")");
        sb.append(lineSeparator);
      }
    }
    sb.append(lineSeparator);

    return sb.toString();
  }

  
}
