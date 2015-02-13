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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

/**
 * Enables a javacore to be taken. For IBM JREs this uses the builtin {@link com.ibm.jvm.Dump.JavaDump()) service, for non-IBM JREs we simulate a javacore.
 */
class Javacore {

  private static final Logger logger = LoggerFactory.getLogger(Javacore.class);

  /** The directory where javacore files are expected to be generated. */
  private static String javacoreDirectoryPath;
  static {
    
    // Determine if an alternative javcore directory is configured. Only applicable to IBM JREs.
    // Note z/OS currently not supported by MQ Light, but check included here for completeness
    final String alternativeJavacoreDirectoryPath;
    final boolean isZOS = System.getProperty("os.name", "").equalsIgnoreCase("OS/390") || System.getProperty("os.name", "").equalsIgnoreCase("z/OS");
    if (isZOS) {
      alternativeJavacoreDirectoryPath = System.getenv("_CEE_DMPTARG");
    } else {
      alternativeJavacoreDirectoryPath = System.getenv("IBM_JAVACOREDIR");
    }

    if (alternativeJavacoreDirectoryPath != null) {
      javacoreDirectoryPath = alternativeJavacoreDirectoryPath;
    } else {
      javacoreDirectoryPath = System.getProperty("user.dir", "");
    }
  }

  private static Method javaDumpMethod;

  private static final String endOfLineCharacter = System.getProperty("line.separator");

  static {
    logger.data("<clinit>");

    // Set up the methods to use for taking javacores
    try {
      final Class<?> dumpClass = Class.forName("com.ibm.jvm.Dump");
      javaDumpMethod = dumpClass.getMethod("JavaDump", new Class<?>[0]);
    } catch (final Exception exception) {
      // No FFDC required - we might not be running on a JVM which
      // supports the IBM JavaDump classes.
      logger.data("<clinit>", "Can't load com.ibm.jvm.Dump");
    }
  }

  /**
   * Public method that allows the caller to ask that a javacore be generated.
   * 
   * @return The file path for the generated java core file.
   * @throws Throwable
   */
  public static String generateJavaCore() throws Throwable {
    final String filePath;
    if (javaDumpMethod != null) {
      // If javaDumpMethod has been assigned a value then the JVM
      // appears to have the appropriate IBM Java classes to take
      // a java core.
      filePath = takeJavaCore();
    } else {
      // For JVMs which don't have the IBM value-add function for taking
      // java cores, use a portal method for dumping thread stack traces
      filePath = simulateJavaCore();
    }

    return filePath;
  }

  /**
   * Causes an JVM with the IBM com.ibm.jvm.Dump class present to take a java core.
   * 
   * @return The file path for the generated java core file.
   * @throws Throwable
   */
  private static String takeJavaCore() throws Throwable {
    logger.entry("takeJavaCore");

    String filePath;
    try {
      javaDumpMethod.invoke(null, new Object[0]);

      final File currentJavacoreFile = getLatestJavacoreFile();
      File javacoreFile = currentJavacoreFile;
      filePath = javacoreFile.getAbsolutePath();

    } catch (final InvocationTargetException invocationException) {
      // Unpack invocation target exception and throw to outer catch block.
      throw (invocationException.getTargetException() == null) ? invocationException : invocationException.getTargetException();
    }
    logger.exit("takeJavaCore", (Object) filePath);

    return filePath;
  }

  /**
   * @return A file object for the latest (newest) javacore file. Javacore files are assumed to be generated in the current default directory for the application (this should be
   *         the log root directory for an agent or logger).
   * @throws FileNotFoundException If a javacore file cannot be found.
   */
  private static File getLatestJavacoreFile() throws FileNotFoundException {
    logger.entry("getLatestJavacoreFilePath");

    final File javacoreDirectory = new File(javacoreDirectoryPath);
    final String[] javacoreFilePaths = javacoreDirectory.list(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.startsWith("javacore.");
      }
    });
    String requiredJavacoreFilePath = "";
    if (javacoreFilePaths != null && javacoreFilePaths.length > 0) {
      requiredJavacoreFilePath = javacoreFilePaths[0];
      for (String filePath : javacoreFilePaths) {
        if (filePath.compareTo(requiredJavacoreFilePath) > 0) requiredJavacoreFilePath = filePath;
      }
    }

    if (requiredJavacoreFilePath.length() == 0) {
      final FileNotFoundException exception =
          new FileNotFoundException("The javacore file has been generated but was not found in the expected directory: " + javacoreDirectory.getAbsolutePath());
      logger.throwing("getLatestJavacoreFilePath", exception);
      throw exception;
    }

    final File result = new File(javacoreDirectory, requiredJavacoreFilePath);

    logger.exit("getLatestJavacoreFilePath", (Object) result);

    return result;
  }

  /**
   * Simulates a java core, for use with JVMs which do not have the com.ibm.jvm.Dump class available. The file is given a name which looks like a java core to avoid introducing new
   * 'must gathers' for service.
   * 
   * @return The file path for the generated java core file.
   */
  private static String simulateJavaCore() {
    logger.entry("simulateJavaCore");

    // Make up a unique filename using the same format as that used by the JVM's dump functions
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.kkmmss.SSS0.");
    final String dateString = dateFormat.format(new Date());
    boolean unique = false;
    int counter = 1;
    String filename = "";
    while (!unique) {
      filename = "javacore." + dateString + String.format("%04d", counter) + ".txt";
      unique = !new File(filename).exists();
      counter++;
    }

    // Write a header onto the simulated java core file explaining what it is and who you should
    // talk to if you find it.
    final StringBuilder sb = new StringBuilder("This JVM does not support com.ibm.jvm.Dump.JavaDump() method");
    sb.append(endOfLineCharacter);
    sb.append("This file was generated by WebSphere MQ Managed File Transfer");
    sb.append(endOfLineCharacter);
    sb.append(endOfLineCharacter);

    // Iterate over each thread known to the JVM
    final Map<Thread, StackTraceElement[]> threadToStackArrayMap = Thread.getAllStackTraces();
    for (final Map.Entry<Thread, StackTraceElement[]> entry : threadToStackArrayMap.entrySet()) {

      // Generate a thread description in a java core like style.
      final Thread thread = entry.getKey();
      sb.append("\"" + thread.getName() + "\" ");
      sb.append("(id: " + thread.getId() + ", state: " + thread.getState() + ") ");
      sb.append("priority=" + thread.getPriority() + ", interrupted=" + thread.isInterrupted() + ", daemon=" + thread.isDaemon());
      sb.append(endOfLineCharacter);

      // Dump the stack in a java core like style
      for (final StackTraceElement element : entry.getValue()) {
        sb.append("   at " + element.getClassName() + "." + element.getMethodName());
        if (element.isNativeMethod()) sb.append("(Native Method)");
        else {
          sb.append("(" + element.getFileName() + ":" + element.getLineNumber() + ")");
        }
        sb.append(endOfLineCharacter);
      }
      sb.append(endOfLineCharacter);
    }
    sb.append("[EOF]");

    // Write the string buffer into a file
    final File outputFile = new File(filename);
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(outputFile);
      out.write(sb.toString().getBytes("UTF-8"));
    } catch (final IOException ioException) {
      logger.error("Unable to generate diagnostic information: " + ioException.getLocalizedMessage());
    } finally {
      try {
        if (out != null) out.close();
      } catch (final IOException ioException) {
        // No FFDC code needed.
        // No reasonable action to take - so ignore.
      }
    }

    logger.exit("simulateJavaCore", (Object) outputFile.getAbsolutePath());

    return outputFile.getAbsolutePath();
  }
}
