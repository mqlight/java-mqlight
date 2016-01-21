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

import static org.junit.Assert.assertEquals;

import java.io.File;

import junit.framework.AssertionFailedError;

import org.junit.Test;

import com.ibm.mqlight.api.ClientOptions.ClientOptionsBuilder;
import com.ibm.mqlight.api.ClientOptions.SSLOptions;

public class TestSSLClientOptions {

    @Test
    public void keyStoreOptions() {
        final ClientOptionsBuilder builder = ClientOptions.builder().setSslKeyStore(new File("keystore"));
        try {
            builder.setSslClientCertificate(new File("clientCert"));
            throw new AssertionFailedError("Expected SslClientCertificate option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }
        try {
            builder.setSslClientKey(new File("clientKey"));
            throw new AssertionFailedError("Expected SslClientKey option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }
        try {
            builder.setSslClientKeyPassphrase("passphrase");
            throw new AssertionFailedError("Expected SslClientKeyPassphrase option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }
        try {
            builder.setSslTrustCertificate(new File("trustCert"));
            throw new AssertionFailedError("Expected SslTrustCertificate option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }
        builder.setSslKeyStorePassphrase("passphrase");
        builder.setSslVerifyName(false);
        final ClientOptions opts = builder.build();
        SSLOptions sslOpts = opts.getSSLOptions();
        assertEquals("keystore", sslOpts.getKeyStoreFile().getPath());
        assertEquals("passphrase", sslOpts.getKeyStoreFilePassphrase());
        assertEquals(null, sslOpts.getClientCertificateFile());
        assertEquals(null, sslOpts.getClientKeyFile());
        assertEquals(null, sslOpts.getClientKeyFilePassphrase());
        assertEquals(null, sslOpts.getTrustCertificateFile());
        assertEquals(false, sslOpts.getVerifyName());
    }

    @Test
    public void trustCertificateOption() {
        final ClientOptionsBuilder builder = ClientOptions.builder().setSslTrustCertificate(new File("trustCert"));
        try {
            builder.setSslKeyStore(new File("keystore"));
            throw new AssertionFailedError("Expected SslKeyStore option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }
        try {
            builder.setSslKeyStorePassphrase("passphrase");
            throw new AssertionFailedError("Expected SslKeyStorePassphrase option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }

        final ClientOptions opts = builder.build();
        SSLOptions sslOpts = opts.getSSLOptions();
        assertEquals(null, sslOpts.getKeyStoreFile());
        assertEquals(null, sslOpts.getKeyStoreFilePassphrase());
        assertEquals(null, sslOpts.getClientCertificateFile());
        assertEquals(null, sslOpts.getClientKeyFile());
        assertEquals(null, sslOpts.getClientKeyFilePassphrase());
        assertEquals("trustCert", sslOpts.getTrustCertificateFile().getPath());
        assertEquals(true, sslOpts.getVerifyName());
    }

    @Test
    public void clientCertificateOption() {
        final ClientOptionsBuilder builder = ClientOptions.builder().setSslClientCertificate(new File("clientCert"));
        try {
            builder.setSslKeyStore(new File("keystore"));
            throw new AssertionFailedError("Expected SslKeyStore option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }
        try {
            builder.setSslKeyStorePassphrase("passphrase");
            throw new AssertionFailedError("Expected SslKeyStorePassphrase option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }

        final ClientOptions opts = builder.build();
        SSLOptions sslOpts = opts.getSSLOptions();
        assertEquals(null, sslOpts.getKeyStoreFile());
        assertEquals(null, sslOpts.getKeyStoreFilePassphrase());
        assertEquals("clientCert", sslOpts.getClientCertificateFile().getPath());
        assertEquals(null, sslOpts.getClientKeyFile());
        assertEquals(null, sslOpts.getClientKeyFilePassphrase());
        assertEquals(null, sslOpts.getTrustCertificateFile());
        assertEquals(true, sslOpts.getVerifyName());
    }

    @Test
    public void clientKeyOption() {
        final ClientOptionsBuilder builder = ClientOptions.builder().setSslClientKey(new File("clientKey"));
        try {
            builder.setSslKeyStore(new File("keystore"));
            throw new AssertionFailedError("Expected SslKeyStore option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }
        try {
            builder.setSslKeyStorePassphrase("passphrase");
            throw new AssertionFailedError("Expected SslKeyStorePassphrase option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }

        final ClientOptions opts = builder.build();
        SSLOptions sslOpts = opts.getSSLOptions();
        assertEquals(null, sslOpts.getKeyStoreFile());
        assertEquals(null, sslOpts.getKeyStoreFilePassphrase());
        assertEquals(null, sslOpts.getClientCertificateFile());
        assertEquals("clientKey", sslOpts.getClientKeyFile().getPath());
        assertEquals(null, sslOpts.getClientKeyFilePassphrase());
        assertEquals(null, sslOpts.getTrustCertificateFile());
        assertEquals(true, sslOpts.getVerifyName());
    }

    @Test
    public void clientKeyPassphraseOption() {
        final ClientOptionsBuilder builder = ClientOptions.builder().setSslClientKeyPassphrase("wibble");
        try {
            builder.setSslKeyStore(new File("keystore"));
            throw new AssertionFailedError("Expected SslKeyStore option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }
        try {
            builder.setSslKeyStorePassphrase("passphrase");
            throw new AssertionFailedError("Expected SslKeyStorePassphrase option to be rejected");
        } catch(IllegalArgumentException e) {
            // Expected.
        }

        final ClientOptions opts = builder.build();
        SSLOptions sslOpts = opts.getSSLOptions();
        assertEquals(null, sslOpts.getKeyStoreFile());
        assertEquals(null, sslOpts.getKeyStoreFilePassphrase());
        assertEquals(null, sslOpts.getClientCertificateFile());
        assertEquals(null, sslOpts.getClientKeyFile());
        assertEquals("wibble", sslOpts.getClientKeyFilePassphrase());
        assertEquals(null, sslOpts.getTrustCertificateFile());
        assertEquals(true, sslOpts.getVerifyName());
    }
}
