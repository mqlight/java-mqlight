/*
 *   <copyright 
 *   notice="oco-source" 
 *   pids="5725-P60" 
 *   years="2015" 
 *   crc="1438874957" > 
 *   IBM Confidential 
 *    
 *   OCO Source Materials 
 *    
 *   5724-H72
 *    
 *   (C) Copyright IBM Corp. 2015
 *    
 *   The source code for the program is not published 
 *   or otherwise divested of its trade secrets, 
 *   irrespective of what has been deposited with the 
 *   U.S. Copyright Office. 
 *   </copyright> 
 */

package com.ibm.mqlight.api.impl.callback;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.callback.CallbackService;

public class ThreadPoolCallbackService implements CallbackService {

    private static class WorkList implements Runnable {
        private final ThreadPoolExecutor executor;
        private boolean running = false;
        private final LinkedList<Runnable> runnables = new LinkedList<>();
        private final LinkedList<Promise<Void>> promises = new LinkedList<>();
        
        private WorkList(ThreadPoolExecutor executor) {
            this.executor = executor;
        }
        
        @Override
        public void run() {
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
        }
        
        public synchronized void put(Runnable runnable, Promise<Void> promise) {
            runnables.addLast(runnable);
            promises.addLast(promise);
            if (!running) {
                running = true;
                executor.submit(this);
            }
        }
    }
    
    private final int poolSize;
    private final ThreadPoolExecutor executor;
    private final WorkList workLists[];
    
    public ThreadPoolCallbackService(int poolSize) {
        this.poolSize = poolSize;
        executor = new ThreadPoolExecutor(0, poolSize, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        workLists = new WorkList[poolSize];
        for (int i = 0; i < poolSize; ++i) workLists[i] = new WorkList(executor);
    }

    @Override
    public void run(Runnable runnable, Object orderingCtx, Promise<Void> promise) {
        workLists[Math.abs(orderingCtx.hashCode()) % poolSize].put(runnable, promise);
    }

}
