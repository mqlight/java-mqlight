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
package com.ibm.mqlight.api.impl.network;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.AssertionFailedError;

import com.ibm.mqlight.api.Promise;

class MockNetworkWritePromise implements Promise<Boolean> {
    private AtomicBoolean done = new AtomicBoolean(false);

    @Override
    public boolean isComplete() {
        return done.get();
    }

    @Override
    public void setSuccess(Boolean drained) {
        done.set(true);
    }

    @Override
    public void setFailure(Exception exception) {
        done.set(true);
        throw new AssertionFailedError("setFailure should never be called!");
    }
}