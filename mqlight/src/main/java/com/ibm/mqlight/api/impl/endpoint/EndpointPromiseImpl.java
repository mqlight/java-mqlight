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

import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointPromise;
import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

public class EndpointPromiseImpl implements EndpointPromise {

    private static final Logger logger = LoggerFactory.getLogger(EndpointPromiseImpl.class);
  
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final Component component;
    
    public EndpointPromiseImpl(Component component) {
        final String methodName = "<init>";
        logger.entry(this, methodName, component);
      
        this.component = component;
        
        logger.exit(this, methodName);
    }
    
    @Override
    public boolean isComplete() {
        return complete.get();
    }

    @Override
    public void setSuccess(Endpoint endpoint) throws IllegalStateException {
        final String methodName = "setSuccess";
        logger.entry(this, methodName, endpoint);
      
        if (complete.getAndSet(true)) {
            final IllegalStateException exception = new IllegalStateException("Promise already completed");
            logger.throwing(this, methodName, exception);
            throw exception;
        } else {
            component.tell(new EndpointResponse(endpoint, null), Component.NOBODY);
        }
        
        logger.exit(this, methodName);
    }

    @Override
    public void setWait(long delay) throws IllegalStateException {
        final String methodName = "setWait";
        logger.entry(this, methodName, delay);
      
        if (complete.getAndSet(true)) {
            final IllegalStateException exception = new IllegalStateException("Promise already completed");
            logger.throwing(this, methodName, exception);
            throw exception;
        } else {
            component.tell(new ExhaustedResponse(delay), Component.NOBODY);
        }
        
        logger.exit(this, methodName);
    }

    @Override
    public void setFailure(Exception exception) throws IllegalStateException {
        final String methodName = "setFailure";
        logger.entry(this, methodName, exception);
      
        if (complete.getAndSet(true)) {
            final IllegalStateException ex = new IllegalStateException("Promise already completed");
            logger.throwing(this, methodName, ex);
            throw ex;
        } else {
            ClientException clientException;
            if (exception instanceof ClientException) {
                clientException = (ClientException)exception;
            } else {
                clientException = new ClientException(
                        "A problem occurred when trying to locate an instance of the MQ Light server.  See linked exception for more details",
                        exception);
                logger.data(this, methodName, clientException);
            }
            component.tell(new EndpointResponse(null, clientException), Component.NOBODY);
        }
        
        logger.exit(this, methodName);
    }

}
