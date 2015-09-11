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
package com.ibm.mqlight.api.impl.callback;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.callback.CallbackService;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

public class ThreadPoolCallbackService implements CallbackService {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolCallbackService.class);

    private static class WorkList implements Runnable {

        private static final Logger logger = LoggerFactory.getLogger(WorkList.class);

        private final ThreadPoolExecutor executor;
        private boolean running = false;
        private final LinkedList<Runnable> runnables = new LinkedList<>();
        private final LinkedList<Promise<Void>> promises = new LinkedList<>();

        private WorkList(ThreadPoolExecutor executor) {
            final String methodName = "<init>";
            logger.entry(this, methodName, executor);

            this.executor = executor;

            logger.exit(this, methodName);
        }

        @Override
        public void run() {
            final String methodName = "run";
            logger.entry(this, methodName);

            while(true) {
                Runnable runnable;
                Promise<Void> promise;
                synchronized(this) {
                    if (runnables.isEmpty()) {
                        running = false;
                        break;
                    } else {
                        runnable = runnables.removeFirst();
                        promise = promises.removeFirst();
                    }
                }
                try {
                    runnable.run();
                    promise.setSuccess(null);
                } catch(Exception e) {
                    promise.setFailure(e);
                }
            }

            logger.exit(this, methodName);
        }

        public synchronized void put(Runnable runnable, Promise<Void> promise) {
            final String methodName = "put";
            logger.entry(this, methodName, runnable, promise);

            runnables.addLast(runnable);
            promises.addLast(promise);
            if (!running) {
                running = true;
                executor.submit(this);
            }

            logger.exit(this, methodName);
        }
    }

    private final int poolSize;
    private final WorkList workLists[];

    public ThreadPoolCallbackService(int poolSize) {
        final String methodName = "<init>";
        logger.entry(this, methodName, poolSize);

        this.poolSize = poolSize;
        workLists = new WorkList[poolSize];

        ThreadPoolExecutor executor = new ThreadPoolExecutor(0, poolSize, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        for (int i = 0; i < poolSize; ++i) workLists[i] = new WorkList(executor);

        logger.exit(this, methodName);
    }

    @Override
    public void run(Runnable runnable, Object orderingCtx, Promise<Void> promise) {
       final String methodName = "run";
        logger.entry(this, methodName, runnable, orderingCtx, promise);

        int hash = orderingCtx.hashCode();
        if (hash == Integer.MIN_VALUE) hash = Integer.MIN_VALUE + 1;    // Avoid possibility that Math.abs(Integer.MIN_VALUE) == Integer.MIN_VALUE
        workLists[Math.abs(hash) % poolSize].put(runnable, promise);

        logger.exit(this, methodName);
    }

}
