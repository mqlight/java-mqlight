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

package com.ibm.mqlight.api;

import junit.framework.AssertionFailedError;

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
        try {
            SubscribeOptions.builder().setTtl(-1);
            throw new AssertionFailedError("Should have failed on -1");
        } catch(IllegalArgumentException e) {
            // Expected
        }
        try {
            SubscribeOptions.builder().setTtl(4294967296L);
            throw new AssertionFailedError("Should have failed on 4294967296L");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }
}
