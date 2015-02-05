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
}
