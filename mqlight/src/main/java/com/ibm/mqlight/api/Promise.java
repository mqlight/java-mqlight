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
package com.ibm.mqlight.api;

/**
 * Represents a promise that the recipient of an instance of this object will
 * perform an operation and then <em>complete</em> the promise by invoking
 * one of the set functions.
 * 
 * @param <T> the type of object passed back when the promise is completed
 *            successfully via the {@link Promise#setSuccess(Object)} method.
 */
public interface Promise<T> {
    
    /**
     * Called to indicate that the related operation failed in some way.
     * @param exception an indication of why the operation failed.
     * @throws IllegalStateException if the promise has already been completed.
     */
    void setFailure(Exception exception) throws IllegalStateException;
    
    /**
     * Called to indicate that the related operation succeeded.
     * @param result an object that represents the result of the operation.
     *               This can be passed back to the class that issued the promise.
     * @throws IllegalStateException if the promise has already been completed.
     */
    void setSuccess(T result) throws IllegalStateException;
    
    /**
     * @return true if the promise has been completed (by either the 
     * {@link Promise#setSuccess(Object)} or the {@link Promise#setFailure(Exception)}
     * method being invoked).
     */
    boolean isComplete();
}
