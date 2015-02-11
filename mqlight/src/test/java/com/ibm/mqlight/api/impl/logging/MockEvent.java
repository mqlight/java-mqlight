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

import org.slf4j.Marker;

public class MockEvent {
  public final String type;
  public final Marker marker;
  public final String message;
  public final Object [] args;
  public final Throwable throwable;
  
  public MockEvent(String type, Marker marker, String message, Object... args) {
    this.type = type;
    this.marker = marker;
    this.message = message;
    if (args.length > 1 && args[1] instanceof Throwable) {
      this.throwable = (Throwable)args[1];
      this.args = new Object[args.length-1];
      this.args[0] = args[0];
      for (int i=2; i<args.length; i++) this.args[i-1] = args[i];
    } else {
      this.args = args;
      this.throwable = null;
    }
  }

  public MockEvent(String type, Marker marker, String message, Throwable throwable) {
    this.type = type;
    this.marker = marker;
    this.message = message;
    this.args = new Object[0];
    this.throwable = throwable;
  }
  
  public String toString() {
    return "type: "+type+" marker: "+marker+" message: "+message+" args: "+args+" throwable: "+throwable;
  }
}
