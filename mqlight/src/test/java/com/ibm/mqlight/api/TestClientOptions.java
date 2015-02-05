package com.ibm.mqlight.api;

import junit.framework.AssertionFailedError;

import org.junit.Test;

public class TestClientOptions {

    @Test
    public void clientIdLength() {
        ClientOptions.builder().setId("012345678901234567890123456789012345678901234567").build();  // 48-char

        ClientOptions.builder().setId("0").build();

        try {
            ClientOptions.builder().setId("").build();
            throw new AssertionFailedError("Expected a zero-length ID to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }

        try {
            ClientOptions.builder().setId("0123456789012345678901234567890123456789012345678").build();
            throw new AssertionFailedError("Expected a 49 character length ID to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected
        }
    }
}
