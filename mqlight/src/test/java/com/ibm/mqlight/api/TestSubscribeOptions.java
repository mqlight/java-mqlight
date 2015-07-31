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

import junit.framework.AssertionFailedError;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class TestSubscribeOptions {

    @Test
    public void creditValues() {
        SubscribeOptions.builder().setCredit(0);
        SubscribeOptions.builder().setCredit(Integer.MAX_VALUE);
        try {
            SubscribeOptions.builder().setCredit(-1);
            throw new AssertionFailedError("setCredit -1 should have been rejected");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void qosValues() {
        SubscribeOptions.builder().setQos(QOS.AT_LEAST_ONCE);
        SubscribeOptions.builder().setQos(QOS.AT_MOST_ONCE);
        try {
            SubscribeOptions.builder().setQos(null);
            throw new AssertionFailedError("setQos of null should have been rejected");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void shareValues() {
        SubscribeOptions.builder().setShare(null);
        SubscribeOptions.builder().setShare("");
        SubscribeOptions.builder().setShare("share");
        try {
            SubscribeOptions.builder().setShare(":");
            throw new AssertionFailedError("Colon should not be permitted in share name");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void ttlValues() {
        SubscribeOptions.builder().setTtl(0);
        SubscribeOptions.builder().setTtl(4294967295L);
        SubscribeOptions.builder().setTtl(4294967296L);
        SubscribeOptions.builder().setTtl(4294967295L*1000L);
        SubscribeOptions.builder().setTtl(365, TimeUnit.DAYS);
        SubscribeOptions.builder().setTtl(365*136, TimeUnit.DAYS);
        try {
            SubscribeOptions.builder().setTtl(-1);
            throw new AssertionFailedError("Should have failed on -1");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            SubscribeOptions.builder().setTtl(-1, TimeUnit.MILLISECONDS);
            throw new AssertionFailedError("Should have failed on -1 milliseconds");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            SubscribeOptions.builder().setTtl(-1, TimeUnit.SECONDS);
            throw new AssertionFailedError("Should have failed on -1 seconds");
        } catch(IllegalArgumentException e) {
            // Expected
        }        
        try {
            SubscribeOptions.builder().setTtl(-1, TimeUnit.MINUTES);
            throw new AssertionFailedError("Should have failed on -1 minutes");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            SubscribeOptions.builder().setTtl(4294967296L*1000L);
            throw new AssertionFailedError("Should have failed on 4294967296000 milliseconds");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            SubscribeOptions.builder().setTtl(365*137, TimeUnit.DAYS);
            throw new AssertionFailedError("Should have failed on 137 years");
        } catch (IllegalArgumentException e) {
        	// Expected
        }
    }
}
