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
package com.ibm.mqlight.api.impl;

import com.ibm.mqlight.api.impl.logging.logback.LogbackLoggingImpl;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

/**
 * Sets up logging using logback, when it is available.
 * <p>
 * This class is a simple wrapper to {@link LogbackLoggingImpl} such that the program can function when logback is not available on the classpath (as LogbackLoggingImpl will only
 * be loaded if we have determined that logback is available).
 */
public class LogbackLogging {
    
    private static final Logger logger = LoggerFactory.getLogger(LogbackLogging.class);
  
    /** Indicates whether or not logback is available. If it is not, methods in this class do nothing. */
    private static final boolean logbackAvailable;
    static {
      boolean available = false;
      try {
        Class.forName("ch.qos.logback.classic.LoggerContext");
        available = true;
      } catch (ClassNotFoundException cnfe) {
        // Ignore: this indicates that we don't have logback on the classpath
      }
      logbackAvailable = available;
      
      logger.data("<clinit>", "logbackAvailable: "+logbackAvailable);
    }

    /**
     * Sets up logging.  Can be called multiple times with no side-effect on all but the first
     * invocation.  Should be invoked from any class that an application writer might invoke
     * (e.g. the client and any pluggable components) ahead of any calls to the SLF4J logging
     * framework (e.g. a static constructor would be a good place).
     * <p>
     * This method only attempts to setup Logback-based logging if all of the following conditions
     * are met:
     * <ol>
     * <li>Logback is available on the classpath.</li>
     * <li>Logback is being used as the implementation of SLF4J.</li>
     * <li>Logback is not already started.</li>
     * </ol>
     * The intent is to integrate with applications that have already configured SLF4J based
     * on their own preferences, while still supporting a logging capability if the client is
     * used in an environment where no prior SLF4-based logging has been configured.
     */
    public static void setup() {
      if (logbackAvailable) LogbackLoggingImpl.setup();
    }

    /**
     * Stops the logging.
     */
    public static void stop() {
      if (logbackAvailable) LogbackLoggingImpl.stop();
    }
}
