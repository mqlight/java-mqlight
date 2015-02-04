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

package com.ibm.mqlight.api.timer;

import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.Promise;

/**
 * Plug point for timer implementations.  The implementation used for an
 * instance of the client can be specified using the
 * {@link NonBlockingClient#create(com.ibm.mqlight.api.endpoint.EndpointService, com.ibm.mqlight.api.callback.CallbackService, com.ibm.mqlight.api.network.NetworkService, TimerService, com.google.gson.GsonBuilder, com.ibm.mqlight.api.ClientOptions, com.ibm.mqlight.api.NonBlockingClientListener, Object)}
 * method.
 */
public interface TimerService {

    /**
     * Schedules a timer that will "pop" at some point in the future.  When the
     * timer "pops" the promise, specified as a parameter, must be completed successfully
     * by calling the {@link Promise#setSuccess(Object)} method.  Timers are "single shot"
     * as the promise can only be completed once.
     * <p>
     * The implementation cannot block the calling thread - and so must employ some scheme
     * that uses another thread to complete the promise.
     *
     * @param delay a delay in milliseconds
     * @param promise a promise object to be completed after the delay period
     */
    public void schedule(long delay, Promise<Void> promise);

    /**
     * Cancels a previously scheduled promise (e.g. one that has previously been passed to
     * the {@link TimerService#schedule(long, Promise)} method.  If the cancel operation is
     * successful then the promise's {@link Promise#setFailure(Exception)} method will be
     * invoked.
     * <p>
     * Once a promise has been scheduled, using the {@link TimerService#schedule(long, Promise)}
     * it will always be completed - even if it is cancelled as a result of this method
     * being invoked.
     * <p>
     * If this method is invoked on a promise which has already completed, it should have no
     * effect.
     *
     * @param promise the promise to cancel
     */
    public void cancel(Promise<Void> promise);
}
