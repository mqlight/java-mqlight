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

import static org.junit.Assert.*;
import org.junit.Test;

public class TestSendOptions {

    @Test
    public void defaults() {
        SendOptions opts = SendOptions.builder().build();
        assertEquals(QOS.AT_MOST_ONCE, opts.getQos());
        assertEquals(0, opts.getTtl());
    }

    @Test
    public void getSet() {
        SendOptions opts = SendOptions.builder().setQos(QOS.AT_LEAST_ONCE).setTtl(8).build();
        assertEquals(QOS.AT_LEAST_ONCE, opts.getQos());
        assertEquals(8, opts.getTtl());
    }

    @Test
    public void qosValues() {
        SendOptions.builder().setQos(QOS.AT_LEAST_ONCE);
        SendOptions.builder().setQos(QOS.AT_MOST_ONCE);
        try {
            SendOptions.builder().setQos(null);
            throw new AssertionFailedError("null should have been rejected");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void ttlValues() {
        SendOptions.builder().setTtl(1);
        SendOptions.builder().setTtl(4294967295L);

        try {
            SendOptions.builder().setTtl(0);
            throw new AssertionFailedError("0 should have been rejected");
        } catch(IllegalArgumentException e) {
            // Expected
        }

        try {
            SendOptions.builder().setTtl(4294967296L);
            throw new AssertionFailedError("4294967296L should have been rejected");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void retainLinkValues() {
        SendOptions defaultRetainLinkOpts = SendOptions.builder().build();
        assertEquals(true, defaultRetainLinkOpts.getRetainLink());

        SendOptions explicitRetainLinkFalseOpts = SendOptions.builder().setRetainLink(false).build();
        assertEquals(false, explicitRetainLinkFalseOpts.getRetainLink());

        SendOptions explicitRetainLinkTrueOpts = SendOptions.builder().setRetainLink(true).build();
        assertEquals(true, explicitRetainLinkTrueOpts.getRetainLink());
    }
}

    @Test
    public void retainLinkValues() {
        SendOptions defaultRetainLinkOpts = SendOptions.builder().build();
        assertEquals(true, defaultRetainLinkOpts.getRetainLink());

        SendOptions explicitRetainLinkFalseOpts = SendOptions.builder().setRetainLink(false).build();
        assertEquals(false, explicitRetainLinkFalseOpts.getRetainLink());

        SendOptions explicitRetainLinkTrueOpts = SendOptions.builder().setRetainLink(true).build();
        assertEquals(true, explicitRetainLinkTrueOpts.getRetainLink());
    }
