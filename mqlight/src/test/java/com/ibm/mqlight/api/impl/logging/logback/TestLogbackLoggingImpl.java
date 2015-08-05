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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Test;

import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

public class TestLogbackLoggingImpl {

  private static final Logger logger = LoggerFactory.getLogger(TestLogbackLoggingImpl.class);

  @Test
  public void testNoTrace() throws IOException {
    
    LogbackLoggingImpl.stop(); // Ensures logging is stopped (in case a previous test enabled it)
    
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final PrintStream ps = new PrintStream(baos, true);
    final PrintStream savedErr = System.err;
    try {
      System.setErr(ps);
      LogbackLoggingImpl.setDefaultRequiredMQLightLogLevel(null);
      LogbackLoggingImpl.setup();
      
      System.err.flush();
      String traceData = baos.toString();
      assertFalse("unexpected header properties", traceData.contains("System properties:"));
      assertFalse("unexpected trace line", traceData.contains("test trace data"));
      logger.data(this, "testAllTrace", "test trace data");
      System.err.flush();
      traceData = baos.toString();
      assertFalse("unexpected trace line", traceData.contains("test trace data"));
    } finally {
      LogbackLoggingImpl.stop();
      System.setErr(savedErr);
    }
  }
  
  @Test
  public void testAllTrace() throws IOException {
    
    LogbackLoggingImpl.stop(); // Ensures logging is stopped (in case a previous test enabled it)
    
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final PrintStream ps = new PrintStream(baos, true);
    final PrintStream savedErr = System.err;
    try {
      System.setErr(ps);
      LogbackLoggingImpl.setDefaultRequiredMQLightLogLevel("all");
      LogbackLoggingImpl.setup();
      
      System.err.flush();
      String traceData = baos.toString();
      assertTrue("missing header properties", traceData.contains("System properties:"));
      assertFalse("unexpected trace line", traceData.contains("test trace data"));
      logger.data(this, "testAllTrace", "test trace data");
      System.err.flush();
      traceData = baos.toString();
      assertTrue("missing trace line", traceData.contains("test trace data"));
    } finally {
      LogbackLoggingImpl.stop();
      System.setErr(savedErr);
    }
  }
  
  @Test
  public void testDebugTrace() throws IOException {
    
    LogbackLoggingImpl.stop(); // Ensures logging is stopped (in case a previous test enabled it)
    
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final PrintStream ps = new PrintStream(baos, true);
    final PrintStream savedErr = System.err;
    try {
      System.setErr(ps);
      LogbackLoggingImpl.setDefaultRequiredMQLightLogLevel("debug");
      LogbackLoggingImpl.setup();
      
      System.err.flush();
      String traceData = baos.toString();
      assertFalse("missing header properties", traceData.contains("System properties:"));
      assertFalse("unexpected trace line", traceData.contains("test trace data"));
      logger.data(this, "testAllTrace", "test trace data");
      System.err.flush();
      traceData = baos.toString();
      assertFalse("unexpected trace line", traceData.contains("test trace data"));
    } finally {
      LogbackLoggingImpl.stop();
      System.setErr(savedErr);
    }
  }

  @Test
  public void testBadTrace() throws IOException {
    
    LogbackLoggingImpl.stop(); // Ensures logging is stopped (in case a previous test enabled it)
    
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final PrintStream ps = new PrintStream(baos, true);
    final PrintStream savedErr = System.err;
    String traceData = null;
    try {
      System.setErr(ps);
      LogbackLoggingImpl.setDefaultRequiredMQLightLogLevel("badLevel");
      LogbackLoggingImpl.setup();
      
      System.err.flush();
      traceData = baos.toString();
      assertFalse("missing header properties", traceData.contains("System properties:"));
      assertFalse("unexpected trace line", traceData.contains("test trace data"));
      assertTrue("missing error trace line", traceData.contains("ERROR: MQ Light log level 'badLevel'"));
      logger.data(this, "testAllTrace", "test trace data");
      System.err.flush();
      traceData = baos.toString();
      assertFalse("unexpected trace line", traceData.contains("test trace data"));
    } finally {
      LogbackLoggingImpl.stop();
      System.setErr(savedErr);
    }
  }
  
  @Test
  public void testBadLogbackConfigResource() throws IOException {
    
    LogbackLoggingImpl.stop(); // Ensures logging is stopped (in case a previous test enabled it)
    
    final String defaultLogbackConfigResource = LogbackLoggingImpl.getLogbackConfigResource();
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final PrintStream ps = new PrintStream(baos, true);
    final PrintStream savedErr = System.err;
    String traceData = null;
    try {
      System.setErr(ps);
      LogbackLoggingImpl.setDefaultRequiredMQLightLogLevel("warn");
      LogbackLoggingImpl.setLogbackConfigResource(defaultLogbackConfigResource+"oops");
      LogbackLoggingImpl.setup();
      
      System.err.flush();
      traceData = baos.toString();
      assertFalse("missing header properties", traceData.contains("System properties:"));
      assertFalse("unexpected trace line", traceData.contains("test trace data"));
      assertTrue("missing error message", traceData.contains("ERROR: MQ Light '"+LogbackLoggingImpl.getLogbackConfigResource()+"' is missing."));
      logger.data(this, "testAllTrace", "test trace data");
      System.err.flush();
      traceData = baos.toString();
      assertFalse("unexpected trace line", traceData.contains("test trace data"));
    } finally {
      LogbackLoggingImpl.stop();
      System.setErr(savedErr);
      LogbackLoggingImpl.setLogbackConfigResource(defaultLogbackConfigResource);
    }
  }
  
  // TODO add tests for when a custom log environment has been setup, to ensure it is preserve
  
}
