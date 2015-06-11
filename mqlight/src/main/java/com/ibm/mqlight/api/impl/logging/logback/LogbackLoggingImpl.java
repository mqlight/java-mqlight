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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.ILoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.StatusPrinter;

import com.ibm.mqlight.api.ClientRuntimeException;
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
  
  /** Output encoding for log and trace when output to a file. */
  private static final String outputEncoding = System.getProperty("file.encoding", "UTF-8");
  
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
        // TODO could allow the following when context already started, but:
        //      1. must not reset
        //      2. Should not be defining a rootLogger, but instead a "com.ibm.mqlight.api" logger
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
            
            // set log level to what MQLIGHT_JAVA_LOG is set to, defaulting to INFO
            rootLogger.setLevel(mqlightLogLevel);
            // TODO could implement mqlightLogLevel to specify levels for different loggers (e.g. com.ibm.mqlight.api=all)
            
            // Determine where the log output is required to go, creating the appropriate appenders and adding to the root logger       
            final LoggerOutput logOutput = getMQLightLogOutput();
            final OutputStreamAppender<ILoggingEvent> logAppender = createAppender(context, new LogFilter(), logOutput, "log.pattern", "log");
            rootLogger.addAppender(logAppender);
            
            LoggerOutput traceOutput = getMQLightTraceOutput();
            if (traceOutput.equals(logOutput)) traceOutput = logOutput;
            final OutputStreamAppender<ILoggingEvent> traceAppender = createAppender(context, new TraceFilter(), traceOutput, "trace.pattern", "trace");
            rootLogger.addAppender(traceAppender);
            
            // Output trace header to the trace output stream, when trace is enabled
            if (rootLogger.isTraceEnabled()) {
              writeTraceHeaderInfo(traceOutput.getPrintStream());
              logger.data("setup", (Object)("Trace level set to: "+mqlightLogLevel));
            }
            
            // Output the logback setup information to the log output
            StatusPrinter.setPrintStream(logOutput.getPrintStream());
            StatusPrinter.print(context);
            
          } else {
            // If the default logback configuration is set then update the level to WARN (as the default is DEBUG)
            if (ClassLoader.class.getResource("/logback.groovy") == null &&
                ClassLoader.class.getResource("/logback-test.xml") == null &&
                ClassLoader.class.getResource("/logback.xml") == null) {
              rootLogger.setLevel(Level.WARN);
            }
          }
        }
      }
    }
  }

  /**
   * Creates an {@link OutputStreamAppender} for the required filter, pattern and logger output.
   * 
   * @param context Logger context to associate the appender with. 
   * @param filter Event log filter.
   * @param logOutput Logger output information for the destination to write logger events to.
   * @param patternProperty Logger context property that defines the pattern for formatting logger event output. 
   * @param name The name of the appender.
   * @return An {@link OutputStreamAppender} for the required parameters.
   */
  private static OutputStreamAppender<ILoggingEvent> createAppender(LoggerContext context, Filter<ILoggingEvent> filter, LoggerOutput logOutput, String patternProperty, String name) {
    final PatternLayoutEncoder patternLayoutEncoder = createPatternLayoutEncoder(context, patternProperty);
    final OutputStreamAppender<ILoggingEvent> appender;
    if (logOutput.isConsole()) {
      appender = new OutputStreamAppender<>();
      appender.setContext(context);
      appender.setEncoder(patternLayoutEncoder);
      appender.setOutputStream(logOutput.getPrintStream());
      appender.setName(name);
      appender.addFilter(filter);
      appender.start();
    } else {
      RollingFileAppender<ILoggingEvent> rAppender = new RollingFileAppender<>();
      rAppender.setContext(context);
      rAppender.setEncoder(patternLayoutEncoder);
      rAppender.setFile(logOutput.getOutputName()+"."+logOutput.getOutputType());
      rAppender.setName(name);
      rAppender.addFilter(filter);
      
      final FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
      rollingPolicy.setContext(context);
      rollingPolicy.setParent(rAppender);
      rollingPolicy.setFileNamePattern(logOutput.getOutputName()+"%i"+"."+logOutput.getOutputType());
      rollingPolicy.setMinIndex(1);
      rollingPolicy.setMaxIndex(logOutput.getFileCount());
      rollingPolicy.start();

      final SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
      triggeringPolicy.setContext(context);
      triggeringPolicy.setMaxFileSize(logOutput.getFileLimit());
      triggeringPolicy.start();

      rAppender.setRollingPolicy(rollingPolicy);
      rAppender.setTriggeringPolicy(triggeringPolicy);
      rAppender.start();
      
      appender = rAppender;
    }
    return appender;
  }

  /**
   * Creates a {@link PatternLayoutEncoder} for the pattern specified for the pattern property.
   * 
   * @param context Logger context to associate the pattern layout encoder with. 
   * @param patternProperty Logger context property that contains the required pattern.
   * @return A {@link PatternLayoutEncoder} for the required pattern.
   */
  private static PatternLayoutEncoder createPatternLayoutEncoder(LoggerContext context, String patternProperty) {
    final String pattern = context.getProperty(patternProperty);
    final PatternLayoutEncoder patternLayoutEncoder = new PatternLayoutEncoder();
    patternLayoutEncoder.setContext(context);
    patternLayoutEncoder.setPattern(pattern);
    patternLayoutEncoder.start();
    return patternLayoutEncoder;
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
   * Helper class to wrap a PrintStream for a logger, so that we can store additional information and test if two logger
   * PrintStreams are for the same output destination.
   */
  private static class LoggerOutput {
    private static final String defaultFileCount = "5";
    private static final String defaultFileLimit= "20MB";
    private final String outputName;
    private final String outputType;
    private final PrintStream printStream;
    private final int fileCount;
    private final String fileLimit;

    public LoggerOutput(String outputName, String outputType, PrintStream printStream) {
      this(outputName, outputType, printStream, defaultFileCount, defaultFileLimit);
    }

    public LoggerOutput(String outputName, String outputType, PrintStream printStream, String fileCount, String fileLimit) {
      this.outputName = outputName;
      this.outputType = outputType;
      this.printStream = printStream;
      
      try {
        this.fileCount = Integer.parseInt(fileCount == null || fileCount.trim().length() == 0 ? defaultFileCount : fileCount);
      } catch (NumberFormatException e) {
        final ClientRuntimeException exception =
            new ClientRuntimeException("Invalid file count value \'"+fileCount+"\' specified");
        throw exception;
      }
      this.fileLimit = fileLimit == null || fileLimit.trim().length() == 0 ? defaultFileLimit : fileLimit;
    }
    
    public String getFileLimit() {
      return fileLimit;
    }

    public int getFileCount() {
      return fileCount;
    }

    public PrintStream getPrintStream() {
      return printStream;
    }

    public boolean isConsole() {
      return outputName.equals("stdout") || outputName.equals("stderr");
    }
    
    public String getOutputName() {
      return outputName;
    }
    
    public String getOutputType() {
      return outputType;
    }
     
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((outputName == null) ? 0 : outputName.hashCode());
      result = prime * result + ((outputType == null) ? 0 : outputType.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      LoggerOutput other = (LoggerOutput) obj;
      if (outputName == null) {
        if (other.outputName != null) return false;
      } else if (!outputName.equals(other.outputName)) return false;
      if (outputType == null) {
        if (other.outputType != null) return false;
      } else if (!outputType.equals(other.outputType)) return false;
      return true;
    }
  }
  
  /**
   * Obtains the required logger output information, for the required log type, based on environment settings.
   * <p>
   * Environment variable: {@code MQLIGHT_JAVA_LOG_STREAM} can be defined to specify the logger output destination path.
   * A value of {@code stdout} or {@code stderr} can be specified to define stdout or stderr, respectively.
   * <p>
   * Environment variable: {@code MQLIGHT_JAVA_LOG_FILE_COUNT} can be defined to specify the maximum number of archive log files to keep. If not specified then a default of 5 is used.
   * <p>
   * Environment variable: {@code MQLIGHT_JAVA_LOG_FILE_LIMIT} can be defined to specify the maximum size of the log file before it is renamed to a archive log file. If not specified then a default of 20MB is used.
   * <p>
   * When {@code MQLIGHT_JAVA_LOG_STREAM} specifies a file path the output log file path will be in the format:
   * {@code MQLIGHT_JAVA_LOG_STREAM%i.type} where {@code %i} is blank for the active log file and ranging from 1 to the defined {@code MQLIGHT_JAVA_LOG_FILE_COUNT} for the archive log files.
   * 
   * @param defaultOutput The default output for the logger information.
   * @param type The type of log output. This is used as the extension, when logging to file.
   * @return A {@link LoggerOutput} containing the required logger information.
   */
  private static LoggerOutput getMQLightOutput(String defaultOutput, String type) {
    String requiredOutput = System.getenv("MQLIGHT_JAVA_LOG_STREAM");
    if (requiredOutput == null || requiredOutput.trim().length() == 0) requiredOutput = defaultOutput;
    final LoggerOutput result;
    if (requiredOutput.equals("stdout")) {
      result = new LoggerOutput(requiredOutput, "", System.out);
    } else if (requiredOutput.equals("stderr")) {
      result = new LoggerOutput(requiredOutput, "", System.err);
    } else {
      final String logFileCount = System.getenv("MQLIGHT_JAVA_LOG_FILE_COUNT");
      final String logFileLimit = System.getenv("MQLIGHT_JAVA_LOG_FILE_LIMIT");
      PrintStream outputStream = null;
      try {
        outputStream = new PrintStream(new File(requiredOutput+"."+type), outputEncoding);
      } catch (FileNotFoundException | UnsupportedEncodingException e) {
        final ClientRuntimeException exception =
            new ClientRuntimeException("Unable to log to file: \'" + requiredOutput+"."+type + "\': " + e.getLocalizedMessage());
        throw exception;
      }
      result = new LoggerOutput(requiredOutput, type, outputStream, logFileCount, logFileLimit);
    }

    return result;
  }
  
  /**
   * @return Logger output information for event log messages.
   */
  private static LoggerOutput getMQLightLogOutput() {
    return getMQLightOutput("stdout", "log");
  }
  
  /**
   * @return Logger output information for trace log messages.
   */
  private static LoggerOutput getMQLightTraceOutput() {
    return getMQLightOutput("stderr", "trc");
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
   * Helper method to write trace header information to the specified {@link PrintStream}.
   * 
   * @param out
   */
  private static void writeTraceHeaderInfo(PrintStream out) {        
    final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz YYYY");
    
    out.println("Date: "+dateFormat.format(new Date()));
    
    out.println("\nProcess ID: "+pid);
    
    out.println("\nSystem properties:");
    final Properties sysProps = System.getProperties();
    int maxPropNameLength = 0;
    for (Entry<Object, Object> entry : sysProps.entrySet()) {
      maxPropNameLength = Math.max(maxPropNameLength, ((String)entry.getKey()).length());
    }
    String propNameStringFormat = "%-"+maxPropNameLength+"."+maxPropNameLength+"s";
    for (Entry<Object, Object> entry : sysProps.entrySet()) {
      final String prop = String.format(propNameStringFormat, entry.getKey());
      final String value = (String)entry.getValue();
      out.println("|   "+prop+"  :-  "+value);      
    }
    

    out.println("\nRuntime properties:");
    out.println( "Available processors: "+Runtime.getRuntime().availableProcessors());
    out.println("Total memory in bytes (now): "+Runtime.getRuntime().totalMemory());
    out.println("Free memory in bytes (now): "+Runtime.getRuntime().freeMemory());
    out.println("Max memory in bytes: "+Runtime.getRuntime().maxMemory());

    out.println("\nStack trace of initiating call:");
    try {
      throw new Exception();
    } catch(Exception e) {
      for (StackTraceElement element : e.getStackTrace()) {
        out.println("  "+element.getClassName()+"."+element.getMethodName()+"("+element.getFileName()+":"+element.getLineNumber()+")");
      }
    }

    out.println("\nVersion: "+Version.getVersion());
    
    out.println("\nTimeStamp    TID  ClientId     ObjectId  Class                                                                                      Data");
    out.println("======================================================================================================================================================================");
    
    out.flush();
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
