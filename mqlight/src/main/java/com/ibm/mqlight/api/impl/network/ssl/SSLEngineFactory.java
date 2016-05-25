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

package com.ibm.mqlight.api.impl.network.ssl;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.ibm.mqlight.api.ClientOptions.SSLOptions;
import com.ibm.mqlight.api.impl.LogbackLogging;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;
import com.ibm.mqlight.api.security.KeyStoreUtils;
import com.ibm.mqlight.api.security.PemFile;

/**
 * Factory to create a SSLEngine for specified SSL options.
 *
 */
public class SSLEngineFactory {

    private static final Logger logger = LoggerFactory.getLogger(SSLEngineFactory.class);

    static {
        LogbackLogging.setup();
    }

    /** Pattern of protocols to disable */
    final Pattern disabledProtocolPattern = Pattern.compile("(SSLv2|SSLv3).*");

    /** Pattern of cipher suites to disable */
    final Pattern disabledCipherPattern = Pattern.compile(".*_(NULL|EXPORT|DES|RC4|MD5|PSK|SRP|CAMELLIA)_.*");

    /**
     * @return A new {@link SSLEngineFactory}.
     */
    public static SSLEngineFactory newInstance() {
        return new SSLEngineFactory();
    }

    private SSLEngineFactory() {
    }

    /**
     * Creates a client mode {@link SSLEngine} for the specified SSL options.
     *
     * @param sslOptions
     *         an object encapsulating the SSL options to use when creating the
     *         SSLEngine.
     * @param host
     *         the host that the SSL engine will be used to connect to.
     * @param port
     *         the port that the SSL engine will be used to connect to.
     * @return An SSLEngine instance for the required SSL options.
     * @throws SSLException
     *         if the keystore, trust store, or client certificate cannot be
     *         found.
     * @throws NoSuchAlgorithmException
     *         if a TLSv1.2 {@link SSLContext} cannot be created.
     * @throws KeyManagementException
     *         if the initialization of the SSLContext fails.
     */
    public SSLEngine createClientSSLEngine(SSLOptions sslOptions, String host, int port) throws SSLException, NoSuchAlgorithmException, KeyManagementException {
        final String methodName = "createClientSSLEngine";
        logger.entry(this, methodName, sslOptions, host, port);

        KeyManagerFactory keyManagerFactory = null;
        TrustManagerFactory trustManagerFactory = null;

        // Setup the key and trust manager factories from the supplied options
        final File keyStoreFile = sslOptions.getKeyStoreFile();
        if (keyStoreFile != null) {
            try {
                final java.security.KeyStore keyStore = KeyStoreUtils.loadKeyStore(keyStoreFile, sslOptions.getKeyStoreFilePassphrase());

                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, sslOptions.getKeyStoreFilePassphrase().toCharArray());
                trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

            } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException | UnrecoverableKeyException e) {
                throw new SSLException("failed to load key store", e);
            }

        } else {
            // If a client certificate is available then setup a key manager factory for the client certificate and private key
            if (sslOptions.getClientCertificateFile() != null && sslOptions.getClientCertificateFile().exists()) {
                try {
                    final KeyStore keyStore = KeyStore.getInstance("JKS");
                    keyStore.load(null, null);

                    final char [] clientKeyPasswordChars;
                    if (sslOptions.getClientKeyFilePassphrase() == null) {
                        // No password provided, so generate one (not that secure, but does not need to be as never exposed externally)
                        clientKeyPasswordChars = Long.toHexString(new SecureRandom().nextLong()).toCharArray();
                    } else {
                        clientKeyPasswordChars = sslOptions.getClientKeyFilePassphrase().toCharArray();
                    }

                    // Get the client certificate chain
                    final PemFile certChainPemFile = new PemFile(sslOptions.getClientCertificateFile());
                    final List<Certificate> certChain = certChainPemFile.getCertificates();

                    // Add the private key, with the certificate chain, to the key store
                    KeyStoreUtils.addPrivateKey(keyStore, sslOptions.getClientKeyFile(), clientKeyPasswordChars, certChain);

                    // Set up key manager factory to use our key store
                    keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    keyManagerFactory.init(keyStore, clientKeyPasswordChars);

                } catch (Exception e) {
                    throw new SSLException("failed to load client certificate or private key", e);
                }
            }

            // If trust certificates ares available then setup a trust manager factory for them
            if (sslOptions.getTrustCertificateFile() != null && sslOptions.getTrustCertificateFile().exists()) {
                try {
                    // First attempt to load the trust certificates assuming the file is a key store
                    // TODO Not sure this is useful as a key store requires a password, which is not provided
                    KeyStore trustStore = null;
                    try {
                        trustStore = KeyStoreUtils.loadKeyStore(sslOptions.getTrustCertificateFile(), null);
                    } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException e) {
                        logger.data(this, methodName, e.toString());
                        trustStore = null;
                    }

                    // If the file was not a key store then assume a PEM format file
                    if (trustStore == null) {
                        trustStore = KeyStore.getInstance("JKS");
                        trustStore.load(null, null);

                        // Get the trust certificates
                        final PemFile certsPemFile = new PemFile(sslOptions.getTrustCertificateFile());
                        List<Certificate> certs = certsPemFile.getCertificates();
                        int index = 0;
                        for (Certificate cert : certs) {
                            trustStore.setCertificateEntry("cert"+(++index), cert);
                        }
                    }

                    // Set up trust manager factory to use our trust store.
                    trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init(trustStore);
                } catch (Exception e) {
                    throw new SSLException("failed to load trust certificates", e);
                }
            }
        }

        // Initialise the SSL context. If the trust manager is null then this falls back to loading default cacerts
        final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(),
                        trustManagerFactory == null ? null : trustManagerFactory.getTrustManagers(), null);

        // Setup the SSLEngine for client mode with the appropriate protocols and ciphers enabled
        final SSLEngine sslEngine = sslContext.createSSLEngine(host, port);
        sslEngine.setUseClientMode(true);

        final LinkedList<String> enabledProtocols = new LinkedList<String>() {
            private static final long serialVersionUID = 7838479468739671083L;
            {
                for (String protocol : sslEngine.getSupportedProtocols()) {
                    if (!disabledProtocolPattern.matcher(protocol).matches()) {
                        add(protocol);
                    }
                }
            }
        };
        sslEngine.setEnabledProtocols(enabledProtocols.toArray(new String[enabledProtocols.size()]));
        logger.data(this, methodName, "enabledProtocols", Arrays.toString(sslEngine.getEnabledProtocols()));

        final LinkedList<String> enabledCipherSuites = new LinkedList<String>() {
            private static final long serialVersionUID = 7838479468739671083L;
            {
                for (String cipher : sslEngine.getSupportedCipherSuites()) {
                    if (!disabledCipherPattern.matcher(cipher).matches()) {
                        add(cipher);
                    }
                }
            }
        };
        sslEngine.setEnabledCipherSuites(enabledCipherSuites.toArray(new String[enabledCipherSuites.size()]));
        logger.data(this, methodName, "enabledCipherSuites", Arrays.toString(sslEngine.getEnabledCipherSuites()));

        if (sslOptions.getVerifyName()) {
            SSLParameters sslParams = sslEngine.getSSLParameters();
            sslParams.setEndpointIdentificationAlgorithm("HTTPS");
            sslEngine.setSSLParameters(sslParams);
        }

        logger.exit(this, methodName, sslEngine);

        return sslEngine;
    }
}
