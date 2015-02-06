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

import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.timer.TimerService;

// TODO: this could be re-written based on a ConcurrentHashMap to reduce the amount of lock
//       contention on the single 'promiseToTimer' HashMap used in this implementation.
public class TimerServiceImpl implements TimerService {

    private static final ScheduledThreadPoolExecutor executor;
    private static final ClientException failureException = new ClientException("Timer cancelled");

    static {
        executor = new ScheduledThreadPoolExecutor(1);
        executor.setKeepAliveTime(500, TimeUnit.MILLISECONDS);
        executor.allowCoreThreadTimeOut(true);
        executor.setRemoveOnCancelPolicy(true);
    }
    
    private static final HashMap<Promise<Void>, Timer> promiseToTimer = new HashMap<>();
    
    private static class Timer implements Runnable {
        private final Promise<Void> promise;
        private final HashMap<Promise<Void>, Timer> promiseToTimer;
        private ScheduledFuture<?> future;
        private Timer(Promise<Void> promise, HashMap<Promise<Void>, Timer> promiseToTimer) {
            this.promise = promise;
            this.promiseToTimer = promiseToTimer;
        }
        public void run() {
            synchronized(promiseToTimer) {
                promiseToTimer.remove(promise);
            }
            promise.setSuccess(null);
        }
    }
    
    @Override
    public void schedule(long delay, Promise<Void> promise) {
        Timer timer = new Timer(promise, promiseToTimer);
        synchronized(promiseToTimer) {
            ScheduledFuture<?> sf = executor.schedule(timer, delay, TimeUnit.MILLISECONDS);
            timer.future = sf;
            promiseToTimer.put(promise, timer);
        }
    }

    @Override
    public void cancel(Promise<Void> promise) {
        synchronized(promiseToTimer) {
            Timer timer = promiseToTimer.get(promise);
            if (timer != null) {
                if (timer.future.cancel(false)) {
                    promiseToTimer.remove(promise);
                    promise.setFailure(failureException);
                }
            }
        }
    }

}
