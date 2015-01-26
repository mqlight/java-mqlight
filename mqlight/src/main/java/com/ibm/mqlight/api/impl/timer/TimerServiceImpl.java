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
        executor.setMaximumPoolSize(5); // TODO 5 == plucked from the air
        executor.setKeepAliveTime(600, TimeUnit.MILLISECONDS);
        executor.allowCoreThreadTimeOut(true);
        executor.setRemoveOnCancelPolicy(true);
    }
    
    private static final HashMap<Promise<Void>, Timer> promiseToTimer = new HashMap<>();
    
    private class Timer implements Runnable {
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
