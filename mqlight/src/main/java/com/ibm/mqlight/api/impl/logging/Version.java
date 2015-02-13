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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

/**
 * Provides information about the product version.
 */
public class Version {
  
  private static final Class<Version> cclass = Version.class;
  
  private static final Logger logger = LoggerFactory.getLogger(cclass);
  
  /**
   * obtains the MQ Light version information from the manifest.
   * 
   * @return The MQ Light version.
   */
  public static String getVersion() {
    String version = "unknown";
    final URLClassLoader cl = (URLClassLoader)cclass.getClassLoader();
    try {
      final URL url = cl.findResource("META-INF/MANIFEST.MF");
      final Manifest manifest = new Manifest(url.openStream());
      for (Entry<Object,Object> entry : manifest.getMainAttributes().entrySet()) {
        final Attributes.Name key = (Attributes.Name)entry.getKey();
        if(Attributes.Name.IMPLEMENTATION_VERSION.equals(key)) {
          version = (String)entry.getValue();
        }
      }
    } catch (IOException e) {
      logger.error("Unable to determine the product version due to error", e);
    }
    return version;
  }
}
