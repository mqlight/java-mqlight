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
