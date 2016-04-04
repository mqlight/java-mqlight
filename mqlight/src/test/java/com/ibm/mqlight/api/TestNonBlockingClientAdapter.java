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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;

import org.junit.Test;

import com.ibm.mqlight.api.impl.logging.logback.LogbackLoggingImpl;

public class TestNonBlockingClientAdapter {

    // Test that the default implementation of NonBlockingClientAdapter
    // logs a warning if either the onStopped or onRetrying methods care
    // called with a non-null exception argument.
    @Test
    public void defaultImplWarnsOnExceptions() {

        String expectedRetryingExceptionMessage = UUID.randomUUID().toString();
        String expectedStoppedExceptionMessage = UUID.randomUUID().toString();
        NonBlockingClientAdapter<Void> adapter =
                new NonBlockingClientAdapter<Void>() {
        };
        ClientException retryingException =
                new ClientException(expectedRetryingExceptionMessage);
        ClientException stoppedException =
                new ClientException(expectedStoppedExceptionMessage);

        LogbackLoggingImpl.stop();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream ps = new PrintStream(baos, true);
        final PrintStream savedOut = System.out;
        try {
            System.setOut(ps);
            LogbackLoggingImpl.setDefaultRequiredMQLightLogLevel("warn");
            LogbackLoggingImpl.setup();
            adapter.onRetrying(null, null, retryingException);
            adapter.onStopped(null, null, stoppedException);
            System.out.flush();
            String traceData = baos.toString();

            System.out.println(traceData);
            assertTrue("missing retry exception message",
                    traceData.contains(expectedRetryingExceptionMessage));
            assertTrue("missing stopped exception message",
                    traceData.contains(expectedStoppedExceptionMessage));
        } finally {
          LogbackLoggingImpl.stop();
          System.setOut(savedOut);
        }
    }
}
