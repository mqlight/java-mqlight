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

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.ILoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.ibm.mqlight.api.impl.logging.Version;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

/**
 * Sets up logging using logback, when it is available.
 * <p>
 * Note that this class requires logback classes to be available on the classpath. Hence it should only be accessed via the {@link com.ibm.mqlight.api.impl.LogbackLogging} class.
 */
public class LogbackLoggingImpl {

  protected static final Class<LogbackLoggingImpl> cclass = LogbackLoggingImpl.class;
  
  private static final Logger logger = LoggerFactory.getLogger(cclass);

  /** The process id. */
  private static final String pid;
  static {
    final String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
    pid = processName.split("@")[0];
  }
  
  /** Indicates whether or not we have been setup. */
  private static AtomicBoolean setup = new AtomicBoolean(false);

  /**
   * The MQ Light log level required when it has not been specified via the MQLIGHT_JAVA_LOG environment variable.
   * Note that this is to support unit testing.
   */
  private static String defaultRequiredMQLightLogLevel = null;
  
  /**
   * Sets up logging. Can be called multiple times with no side-effect on all but the first invocation. Should be invoked from any class that an application writer might invoke
   * (e.g. the client and any pluggable components) ahead of any calls to the SLF4J logging framework (e.g. a static constructor would be a good place).
   * <p>
   * This method only attempts to setup Logback-based logging if all of the following conditions are met:
   * <ol>
   * <li>Logback is available on the classpath.</li>
   * <li>Logback is being used as the implementation of SLF4J.</li>
   * <li>Logback is not already started.</li>
   * </ol>
   * The intent is to integrate with applications that have already configured SLF4J based on their own preferences, while still supporting a logging capability if the client is
   * used in an environment where no prior SLF4-based logging has been configured.
   */
  public static void setup() {
    if (!setup.getAndSet(true)) {
      
      final ILoggerFactory loggerFactory = org.slf4j.LoggerFactory.getILoggerFactory();
      if (loggerFactory instanceof LoggerContext) {
        final LoggerContext context = (LoggerContext) loggerFactory;
        if (!context.isStarted()) {
          final ch.qos.logback.classic.Logger rootLogger = context.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
          
          // Obtain the required log level
          final Level mqlightLogLevel = getMQLightLogLevel();
          
          // When the MQ Light log level is set, configure the logback trace for MQ Light
          // Note that this replaces any existing logback settings
          if (mqlightLogLevel != null) {
            final String logbackConfigResource = "/com/ibm/mqlight/api/resources/mqlight-logback.xml";
            InputStream logbackConfigResourceStream = null;
            try {
              logbackConfigResourceStream = ClassLoader.class.getResourceAsStream(logbackConfigResource);
              if (logbackConfigResourceStream == null) {
                rootLogger.error("ERROR: MQ Light "+logbackConfigResource+" is missing.");
              } else {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                context.reset();
                configurator.doConfigure(logbackConfigResourceStream);
              }
            } catch (JoranException je) {
              // StatusPrinter will handle this
            } finally {
              if (logbackConfigResourceStream != null) {
                try {
                  logbackConfigResourceStream.close();
                } catch (IOException e) {
                  rootLogger.error("WARNING: Failed to close "+logbackConfigResource+", reason: "+e.getLocalizedMessage());
                }
              }
            }
            
            // set log level to what MQLIGHT_JAVA_LOG is set to defaulting to INFO
            rootLogger.setLevel(mqlightLogLevel);
            
            StatusPrinter.print(context);
            
            // Output trace header, when trace is enabled 
            if (rootLogger.isTraceEnabled()) {
              final org.slf4j.Logger headerLogger = org.slf4j.LoggerFactory.getLogger("");
              writeHeaderInfo(headerLogger);
              logger.data("setup", (Object)("Trace level set to: "+mqlightLogLevel));
            }
          } else {
            // If the default logback configuration is set then update the level to WARN (as the default is DEBUG)
            if (ClassLoader.class.getResource("logback.groovy") == null &&
                ClassLoader.class.getResource("logback-test.xml") == null &&
                ClassLoader.class.getResource("logback.xml") == null) {
              rootLogger.setLevel(Level.WARN);
            }
          }
        }
      }
    }
  }

  private static Level getMQLightLogLevel() {
    // Obtain the MQ Light log level from the MQLIGHT_JAVA_LOG environment variable
    String requiredMQLightLogLevel = System.getenv("MQLIGHT_JAVA_LOG");
    if (requiredMQLightLogLevel == null) requiredMQLightLogLevel = defaultRequiredMQLightLogLevel;
    Level mqlightLogLevel = null;
    if (requiredMQLightLogLevel != null) {
      mqlightLogLevel = Level.toLevel(requiredMQLightLogLevel);
      if (mqlightLogLevel == null) {
        logger.error("ERROR: MQ Light log level '"+requiredMQLightLogLevel+"' is invalid");
      }
    }
    
    return mqlightLogLevel;
  }

  /**
   * *** For Unit testing purposes only ***
   * <p>
   * Sets the default MQ Light log level, when the MQLIGHT_JAVA_LOG environment variable has not been set.
   * 
   * @param value The default log level required, as a {@link String}.
   */
  static void setDefaultRequiredMQLightLogLevel(String value) {
    defaultRequiredMQLightLogLevel = value;
  }
  
  /**
   * Helper method to write header information to the log.
   * 
   * @param logger
   * @param mqlightLogLevel
   */
  private static void writeHeaderInfo(org.slf4j.Logger logger) {        
    final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz YYYY");
    
    final Marker headerMarker = MarkerFactory.getMarker("HEADER");
    
    logger.info(headerMarker, "Date: "+dateFormat.format(new Date()));
    
    logger.info(headerMarker, "\nProcess ID: "+pid);
    
    logger.info(headerMarker, "\nSystem properties:");
    final Properties sysProps = System.getProperties();
    int maxPropNameLength = 0;
    for (Entry<Object, Object> entry : sysProps.entrySet()) {
      maxPropNameLength = Math.max(maxPropNameLength, ((String)entry.getKey()).length());
    }
    String propNameStringFormat = "%-"+maxPropNameLength+"."+maxPropNameLength+"s";
    for (Entry<Object, Object> entry : sysProps.entrySet()) {
      final String prop = String.format(propNameStringFormat, entry.getKey());
      final String value = (String)entry.getValue();
      logger.info(headerMarker, "|   "+prop+"  :-  "+value);      
    }
    

    logger.info(headerMarker, "\nRuntime properties:");
    logger.info(headerMarker, "Available processors: "+Runtime.getRuntime().availableProcessors());
    logger.info(headerMarker, "Total memory in bytes (now): "+Runtime.getRuntime().totalMemory());
    logger.info(headerMarker, "Free memory in bytes (now): "+Runtime.getRuntime().freeMemory());
    logger.info(headerMarker, "Max memory in bytes: "+Runtime.getRuntime().maxMemory());

    logger.info(headerMarker, "\nStack trace of initiating call:");
    try {
      throw new Exception();
    } catch(Exception e) {
      for (StackTraceElement element : e.getStackTrace()) {
        logger.info(headerMarker, "  "+element.getClassName()+"."+element.getMethodName()+"("+element.getFileName()+":"+element.getLineNumber()+")");
      }
    }

    logger.info(headerMarker, "\nVersion: "+Version.getVersion());
    
    logger.info(headerMarker, "\nTimeStamp    TID  ClientId     ObjectId  Class                                                                                      Data");
    logger.info(headerMarker, "======================================================================================================================================================================");
  }

  /**
   * Stops the logging.
   */
  public static void stop() {
    final ILoggerFactory loggerFactory = org.slf4j.LoggerFactory.getILoggerFactory();
    if (loggerFactory instanceof LoggerContext) {
      final LoggerContext context = (LoggerContext) loggerFactory;
      context.stop();
    }
    setup.getAndSet(false);
  }
}
