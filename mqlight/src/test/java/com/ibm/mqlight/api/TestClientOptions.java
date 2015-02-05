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

    @Test
    public void clientIdValidChars() {
        ClientOptions.builder().setId("abcdefghijklmnopqrstuvwxyz").build();
        ClientOptions.builder().setId("ABCDEFGHIJKLMNOPQRSTUVWXYZ").build();
        ClientOptions.builder().setId("0123456789").build();
        ClientOptions.builder().setId("%./_").build();
        try {
            ClientOptions.builder().setId("badid!").build();
            throw new AssertionFailedError("ID should have been rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }
    }
}
