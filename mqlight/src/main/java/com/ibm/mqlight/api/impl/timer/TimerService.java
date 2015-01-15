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

import com.ibm.mqlight.api.impl.Component;
import com.ibm.mqlight.api.impl.Message;

public class TimerService extends Component {
    
    private final ScheduledThreadPoolExecutor executor;
    
    private TimerService() {
        executor = new ScheduledThreadPoolExecutor(0);
        executor.setMaximumPoolSize(5); // TODO 5 == plucked from the air
        executor.setKeepAliveTime(500, TimeUnit.MILLISECONDS);
    }
    private static final TimerService instance = new TimerService();
    public static final TimerService getInstance() { return instance; }
    
    
    private final HashMap<ScheduleRequest, Timer> requestToTimer = new HashMap<>();
    
    private class Timer implements Runnable {
        private final ScheduleRequest request;
        private final HashMap<ScheduleRequest, Timer> requestToTimer;
        private ScheduledFuture<?> future;
        private Timer(ScheduleRequest request, HashMap<ScheduleRequest, Timer> requestToTimer) {
            this.request = request;
            this.requestToTimer = requestToTimer;
        }
        public void run() {
            synchronized(requestToTimer) {
                requestToTimer.remove(request);
            }
            request.getSender().tell(new PopResponse(request), instance);
        }
    }
    
    @Override
    protected void onReceive(Message message) {
        
        if (message instanceof ScheduleRequest) {
            ScheduleRequest sr = (ScheduleRequest)message;
            Timer timer = new Timer(sr, requestToTimer);
            synchronized(requestToTimer) {
                ScheduledFuture<?> sf = executor.schedule(timer, sr.delay, TimeUnit.MILLISECONDS);
                timer.future = sf;
                requestToTimer.put(sr, timer);
            }
        } else if (message instanceof CancelRequest) {
            CancelRequest cr = (CancelRequest)message;
            synchronized(requestToTimer) {
                Timer timer = requestToTimer.get(cr.request);
                if (timer != null) {
                    if (timer.future.cancel(false)) {
                        requestToTimer.remove(cr.request);
                        cr.getSender().tell(new CancelResponse(cr), this);
                    }
                }
            }
        }
        
    }
}
