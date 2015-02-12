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

import com.ibm.mqlight.api.endpoint.EndpointService;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

abstract class EndpointServiceImpl implements EndpointService {
    
  private static final Logger logger = LoggerFactory.getLogger(EndpointServiceImpl.class);
  
    protected long calculateDelay(int retryCount) {
        final String methodName = "calculateDelay";
        logger.entry(this, methodName, retryCount);
      
        double upperBound = 1 << (retryCount < 9 ? retryCount : 8);
        double lowerBound = 0.75 * upperBound;
        double jitter = Math.random() * (0.25 * upperBound);
        double interval = Math.min(60000, (lowerBound + jitter) * 1000);
        
        long result = Math.round(interval);
        
        logger.exit(this, methodName, result);
        
        return result;
    }
}
