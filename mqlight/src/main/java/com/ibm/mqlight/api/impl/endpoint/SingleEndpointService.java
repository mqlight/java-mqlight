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
package com.ibm.mqlight.api.impl.endpoint;

import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointPromise;
import com.ibm.mqlight.api.impl.LogbackLogging;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

public class SingleEndpointService extends EndpointServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(SingleEndpointService.class);
  
    static {
        LogbackLogging.setup();
    }
    
    private final Endpoint endpoint;
    boolean exhausted = false;
    int retryCount = 0;
    
    @Override
    public void lookup(EndpointPromise future) {
        final String methodName = "lookup";
        logger.entry(this, methodName, future);
      
        if (exhausted) {
            exhausted = false;
            future.setWait(calculateDelay(retryCount++));
        } else {
            exhausted = true;
            future.setSuccess(endpoint);
        }
        
        logger.exit(this, methodName);
    }

    @Override
    public void onSuccess(Endpoint endpoint) {
        final String methodName = "onSuccess";
        logger.entry(this, methodName, endpoint);
      
        exhausted = false;
        retryCount = 0;
        
        logger.exit(this, methodName);
    }

    public SingleEndpointService(String uri, String user, String password) {
        final String methodName = "<init>";
        logger.entry(this, methodName, uri, user, "******");
      
        endpoint = new EndpointImpl(uri, user, password);
        
        logger.exit(this, methodName);
    }
}
