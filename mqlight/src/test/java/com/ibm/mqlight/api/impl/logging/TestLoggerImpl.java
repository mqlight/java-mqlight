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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import org.junit.Test;
import org.slf4j.MDC;

import com.ibm.mqlight.api.logging.FFDCProbeId;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

public class TestLoggerImpl {

  private static class ReloadClassLoader extends ClassLoader {
    private final String loadFailClassName;

    public ReloadClassLoader(String loadFailClassName) {
      this.loadFailClassName = loadFailClassName;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
      if (className.startsWith("com.ibm.mqlight")) {
        if (className.equals(loadFailClassName)) {
          throw new ClassNotFoundException("Forcibly failed to load class "+loadFailClassName);
        }

        final String resourceName = className.replaceAll("\\.", "/") + ".class";
        final ByteBuffer bb = ByteBuffer.allocate(65536);
        InputStream is = null;
        try {
          try {
            is = this.getResourceAsStream(resourceName);
            int b;
            while ((b = is.read()) != -1)
              bb.put((byte) b);
          } finally {
            if (is != null) is.close();
          }
        } catch (IOException e) {
          throw new Error("not found");
        }
        bb.flip();
        byte[] data = new byte[bb.remaining()];
        bb.get(data);
        return defineClass(className, data, 0, data.length);
      } else {
        return super.loadClass(className);
      }
    }
  }

  @Test
  public void testLoadError() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
    final ReloadClassLoader loader = new ReloadClassLoader(LoggerFactory.LOGGER_FACTORY_IMPL_CLASS_NAME);
    try {
      Class<?> loggerFactory = loader.loadClass("com.ibm.mqlight.api.logging.LoggerFactory");
      Method method = loggerFactory.getMethod("getLogger", Class.class);
      method.invoke(null, getClass());
      fail("expected and exception to be thrown");
    } catch (Error e) {
    }
  }

  @Test
  public void testClientId() {

    final MockLogger logger = new MockLogger("testClientId");
    final Logger testLogger = LoggerFactory.getLogger(logger);
    testLogger.setClientId("TestClientId");
    final String clientId = MDC.get(Logger.CLIENTID_KEY);

    assertEquals("Expected client id to be set", "TestClientId", clientId);
  }

  @Test
  public void testInfo() {

    final MockLogger logger = new MockLogger("testInfo");
    final Logger testLogger = LoggerFactory.getLogger(logger);
    testLogger.info("test info message");
    MockEvent event = logger.getEvent();
    assertEquals("Unexpected type", "info", event.type);
    assertEquals("Unexpected marker", LogMarker.INFO.getValue(), event.marker);
    assertEquals("Unexpected message", "test info message", event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertEquals("Unexpected args", 0, event.args.length);

    try {
      event = logger.getEvent();
      fail("Unexpected event: "+event);
    } catch(NoSuchElementException e) {
    }
  }

  @Test
  public void testWarning() {

    final MockLogger logger = new MockLogger("testWarning");
    final Logger testLogger = LoggerFactory.getLogger(logger);
    testLogger.warning("test warning message");
    MockEvent event = logger.getEvent();
    assertEquals("Unexpected type", "warn", event.type);
    assertEquals("Unexpected marker", LogMarker.WARNING.getValue(), event.marker);
    assertEquals("Unexpected message", "test warning message", event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertEquals("Unexpected args", 0, event.args.length);

    try {
      event = logger.getEvent();
      fail("Unexpected event: "+event);
    } catch(NoSuchElementException e) {
    }
  }

  @Test
  public void testError() {

    final MockLogger logger = new MockLogger("testError");
    final Logger testLogger = LoggerFactory.getLogger(logger);
    testLogger.error("test error message");
    MockEvent event = logger.getEvent();
    assertEquals("Unexpected type", "error", event.type);
    assertEquals("Unexpected marker", LogMarker.ERROR.getValue(), event.marker);
    assertEquals("Unexpected message", "test error message", event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertEquals("Unexpected args", 0, event.args.length);

    Exception testException = new Exception("error exception");
    testLogger.error("test error message2", testException);
    event = logger.getEvent();
    assertEquals("Unexpected type", "error", event.type);
    assertEquals("Unexpected marker", LogMarker.ERROR.getValue(), event.marker);
    assertEquals("Unexpected message", "test error message2", event.message);
    assertEquals("Unexpected throwable", testException, event.throwable);
    assertEquals("Unexpected args", 0, event.args.length);

    try {
      event = logger.getEvent();
      fail("Unexpected event: "+event);
    } catch(NoSuchElementException e) {
    }
  }

  @Test
  public void testEntry() {

    final MockLogger logger = new MockLogger("testEntry");
    final Logger testLogger = LoggerFactory.getLogger(logger);
    final String methodName = "testEntryMethod";
    testLogger.entry(methodName);
    MockEvent event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.ENTRY.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertNull("Unexpected object", event.args[0]);
    assertEquals("Unexpected args", 1, event.args.length);

    testLogger.entry(this, methodName);
    event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.ENTRY.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertEquals("Unexpected object", this, event.args[0]);
    assertEquals("Unexpected args", 1, event.args.length);

    Integer arg1 = 123;
    String arg2 = "abc";
    testLogger.entry(methodName, arg1, arg2);
    event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.ENTRY.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertNull("Unexpected object", event.args[0]);
    assertEquals("Unexpected arg1", arg1, event.args[1]);
    assertEquals("Unexpected arg2", arg2, event.args[2]);
    assertEquals("Unexpected args", 3, event.args.length);

    arg1 = 567;
    arg2 = "jgf";
    ProcessBuilder arg3 = new ProcessBuilder();
    testLogger.entry(this, methodName, arg1, arg2, arg3);
    event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.ENTRY.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertEquals("Unexpected object", this, event.args[0]);
    assertEquals("Unexpected arg1", arg1, event.args[1]);
    assertEquals("Unexpected arg2", arg2, event.args[2]);
    assertEquals("Unexpected arg2", arg3, event.args[3]);
    assertEquals("Unexpected args", 4, event.args.length);

    try {
      event = logger.getEvent();
      fail("Unexpected event: "+event);
    } catch(NoSuchElementException e) {
    }
  }

  @Test
  public void testExit() {

    final MockLogger logger = new MockLogger("testExit");
    final Logger testLogger = LoggerFactory.getLogger(logger);
    final String methodName = "testExitMethod";
    testLogger.exit(methodName);
    MockEvent event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.EXIT.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertNull("Unexpected object", event.args[0]);
    assertEquals("Unexpected args", 1, event.args.length);

    testLogger.exit(this, methodName);
    event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.EXIT.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertEquals("Unexpected object", this, event.args[0]);
    assertEquals("Unexpected args", 1, event.args.length);

    Integer result1 = 123;
    testLogger.exit(methodName, result1);
    event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.EXIT.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertNull("Unexpected object", event.args[0]);
    assertEquals("Unexpected arg1", result1, event.args[1]);
    assertEquals("Unexpected args", 2, event.args.length);

    ProcessBuilder result2 = new ProcessBuilder();
    testLogger.exit(this, methodName, result2);
    event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.EXIT.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertEquals("Unexpected object", this, event.args[0]);
    assertEquals("Unexpected arg1", result2, event.args[1]);
    assertEquals("Unexpected args", 2, event.args.length);

    try {
      event = logger.getEvent();
      fail("Unexpected event: "+event);
    } catch(NoSuchElementException e) {
    }
  }

  @Test
  public void testData() {

    final MockLogger logger = new MockLogger("testData");
    final Logger testLogger = LoggerFactory.getLogger(logger);
    final String methodName = "testDataMethod";
    testLogger.data(methodName);
    MockEvent event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.DATA.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertNull("Unexpected object", event.args[0]);
    assertEquals("Unexpected args", 1, event.args.length);

    testLogger.data(this, methodName);
    event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.DATA.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertEquals("Unexpected object", this, event.args[0]);
    assertEquals("Unexpected args", 1, event.args.length);

    Integer arg1 = 123;
    String arg2 = "abc";
    testLogger.data(methodName, arg1, arg2);
    event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.DATA.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertNull("Unexpected object", event.args[0]);
    assertEquals("Unexpected arg1", arg1, event.args[1]);
    assertEquals("Unexpected arg2", arg2, event.args[2]);
    assertEquals("Unexpected args", 3, event.args.length);

    arg1 = 567;
    arg2 = "jgf";
    ProcessBuilder arg3 = new ProcessBuilder();
    testLogger.data(this, methodName, arg1, arg2, arg3);
    event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.DATA.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertNull("Unexpected throwable", event.throwable);
    assertEquals("Unexpected object", this, event.args[0]);
    assertEquals("Unexpected arg1", arg1, event.args[1]);
    assertEquals("Unexpected arg2", arg2, event.args[2]);
    assertEquals("Unexpected arg2", arg3, event.args[3]);
    assertEquals("Unexpected args", 4, event.args.length);

    try {
      event = logger.getEvent();
      fail("Unexpected event: "+event);
    } catch(NoSuchElementException e) {
    }
  }

  @Test
  public void testThrowing() {

    final MockLogger logger = new MockLogger("testThrowing");
    final Logger testLogger = LoggerFactory.getLogger(logger);
    final String methodName = "testThrowingMethod";
    Exception testException = new Exception("throwing exception");
    testLogger.throwing("testThrowingMethod", testException);
    MockEvent event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.THROWING.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertEquals("Unexpected throwable", testException, event.throwable);
    assertNull("Unexpected object", event.args[0]);
    assertEquals("Unexpected args", 1, event.args.length);

    // Note that the test below shows up potentially unexpected behaviour as the testException gets treated as a regular argument (i.e. the underlying called org.slf4j.Logger.trace
    // method is not one with a Throwable argument (the MockEvent test class has logic to deal with this behaviour)
    testLogger.throwing(this, "testThrowingMethod", testException);
    event = logger.getEvent();
    assertEquals("Unexpected type", "trace", event.type);
    assertEquals("Unexpected marker", LogMarker.THROWING.getValue(), event.marker);
    assertEquals("Unexpected method name", methodName, event.message);
    assertEquals("Unexpected throwable", testException, event.throwable);
    assertEquals("Unexpected object", this, event.args[0]);
    assertEquals("Unexpected args", 1, event.args.length);

    try {
      event = logger.getEvent();
      fail("Unexpected event: "+event);
    } catch(NoSuchElementException e) {
    }
  }

  @Test
  public void testFFDC() {

    final MockLogger logger = new MockLogger(this.getClass().getName());
    final Logger testLogger = LoggerFactory.getLogger(logger);
    final String methodName = "testFFDCMethod";
    final Exception exception = new Exception("TestExceptionForFFDC");
    testLogger.ffdc(this, methodName, FFDCProbeId.PROBE_007, exception, "data1", "data2");

    MockEvent event = logger.getEvent();
    System.out.println(event.message);
    assertEquals("Unexpected type", "error", event.type);
    assertEquals("Unexpected marker", LogMarker.FFDC.getValue(), event.marker);
    assertTrue("Unexpected message", event.message.contains(FFDCProbeId.PROBE_007.toString()));
    assertTrue("Unexpected message", event.message.contains("testFFDCMethod"));
    assertTrue("Unexpected message", event.message.contains(this.getClass().getName()));
    assertTrue("Unexpected message", event.message.contains("java.lang.Exception: TestExceptionForFFDC"));
    assertTrue("Unexpected message", event.message.contains("data1"));
    assertTrue("Unexpected message", event.message.contains("data2"));

    event = logger.getEvent();
    System.out.println(event.message);
    assertEquals("Unexpected type", "info", event.type);
    assertTrue("Unexpected message", event.message.startsWith("Javacore diagnostic information"));

    try {
      event = logger.getEvent();
      fail("Unexpected event: "+event);
    } catch(NoSuchElementException e) {
    }
  }

}
