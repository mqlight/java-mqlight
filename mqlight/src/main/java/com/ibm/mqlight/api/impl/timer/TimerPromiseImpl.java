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
package com.ibm.mqlight.api.impl.timer;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.impl.Component;

public class TimerPromiseImpl implements Promise<Void> {

    private final Component component;
    private final Object context;
    private AtomicBoolean complete = new AtomicBoolean(false);
    
    public TimerPromiseImpl(Component component, Object context) {
        this.component = component;
        this.context = context;
    }

    @Override
    public void setFailure(Exception exception) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise is already complete");
        } else {
            component.tell(new CancelResponse(this), component);
        }
    }

    @Override
    public void setSuccess(Void result) throws IllegalStateException {
        if (complete.getAndSet(true)) {
            throw new IllegalStateException("Promise is already complete");
        } else {
            component.tell(new PopResponse(this), component);
        }
    }

    @Override
    public boolean isComplete() {
        return complete.get();
    }

    public Object getContext() {
        return context;
    }
}
