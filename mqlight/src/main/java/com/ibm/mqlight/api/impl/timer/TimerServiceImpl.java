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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;
import com.ibm.mqlight.api.timer.TimerService;

public class TimerServiceImpl implements TimerService {

    private static final Logger logger = LoggerFactory.getLogger(TimerServiceImpl.class);

    private static final int idleKeepAliveTimeMs = 500;
    private static final ScheduledThreadPoolExecutor executor;
    private int timerCount = 0;

    static class TimerServiceThreadFactory implements ThreadFactory {
      final ThreadFactory factory = Executors.defaultThreadFactory();
      @Override
      public Thread newThread(Runnable r) {
        final Thread t = factory.newThread(r);
        t.setName(TimerServiceImpl.class.getSimpleName() + "-" + t.getName());
        return t;
      }
    }

    static {
        executor = new ScheduledThreadPoolExecutor(0, new TimerServiceThreadFactory());
        executor.setKeepAliveTime(idleKeepAliveTimeMs, TimeUnit.MILLISECONDS);
        executor.setRemoveOnCancelPolicy(true);
    }

    // Using a default ConcurrentHashMap, with concurrency level 16, which should be sufficient for most applications
    private static final ConcurrentHashMap<Promise<Void>, Timer> promiseToTimer = new ConcurrentHashMap<>();

    private static class Timer implements Runnable {

        private static final Logger logger = LoggerFactory.getLogger(Timer.class);

        private final TimerServiceImpl service;
        private final Promise<Void> promise;
        private final ConcurrentHashMap<Promise<Void>, Timer> promiseToTimer;
        private ScheduledFuture<?> future;

        private Timer(TimerServiceImpl service,
                      Promise<Void> promise,
                      ConcurrentHashMap<Promise<Void>, Timer> promiseToTimer) {
            final String methodName = "<init>";
            logger.entry(this, methodName, service, promise, promiseToTimer);

            this.service = service;
            this.promise = promise;
            this.promiseToTimer = promiseToTimer;

            logger.exit(this, methodName);
        }
        @Override
        public void run() {
            final String methodName = "run";
            logger.entry(this, methodName);

            promiseToTimer.remove(promise);
            promise.setSuccess(null);
            service.decrementUsage();

            logger.exit(this, methodName);
        }
    }

    @Override
    public void schedule(long delay, Promise<Void> promise) {
        final String methodName = "schedule";
        logger.entry(this, methodName, delay, promise);

        final Timer timer = new Timer(this, promise, promiseToTimer);
        incrementUsage();
        timer.future = executor.schedule(timer, delay, TimeUnit.MILLISECONDS);
        promiseToTimer.put(promise, timer);

        logger.exit(this, methodName);
    }

    @Override
    public void cancel(Promise<Void> promise) {
        final String methodName = "cancel";
        logger.entry(this, methodName, promise);

        final Timer timer = promiseToTimer.get(promise);
        if (timer != null) {
            if (timer.future.cancel(false)) {
                promiseToTimer.remove(promise);
                promise.setFailure(null);
                decrementUsage();
            }
        }

        logger.exit(this, methodName);
    }

    private synchronized void decrementUsage() {
        if (--timerCount == 0) {
            executor.setKeepAliveTime(idleKeepAliveTimeMs, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized void incrementUsage() {
        if (++timerCount == 1) {
            executor.setKeepAliveTime(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
    }
}
