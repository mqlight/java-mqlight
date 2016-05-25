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

import java.io.File;

import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

/**
 * A set of options that can be used to configure the behaviour of the <code>NonBlockingClient</code>
 * class when it is created using the {code {@link NonBlockingClient#create(String, ClientOptions, NonBlockingClientListener, Object)})
 * method.  For example:
 * <pre>
 * ClientOptions opts = ClientOptions.builder().setId("client_1234").setCredentials("bob", "passw0rd".getBytes()).build();
 * NonBlockingClient client = NonBlockingClient.create("amqp://localhost", opts, listener, null);
 * </pre>
 */
public class ClientOptions {

    private static final Logger logger = LoggerFactory.getLogger(ClientOptions.class);

    private final String id;
    private final String user;
    private final String password;
    private final SSLOptions sslOptions;

    private ClientOptions(String id, String user, String password, SSLOptions sslOptions) {
        final String methodName = "<init>";
        logger.entry(this, methodName, id, user, "******", sslOptions);

        this.id = id;
        this.user = user;
        this.password = password;
        this.sslOptions = sslOptions;

        logger.exit(this, methodName);
    }

    public String getId() {
        return id;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public SSLOptions getSSLOptions() {
        return sslOptions;
    }

    @Override
    public String toString() {
        return super.toString()
                + " [id=" + id
                + ", user=" + user
                + ", password=" + (password == null ? null : "******")
                + ", sslOption=" + sslOptions.toString()
                + "]";
    }

    /**
     * A set of SSL/TLS specific client options.
     */
    public static class SSLOptions {
        private final File keyStoreFile;
        private final String keyStoreFilePassphrase;
        private final File trustCertFile;
        private final boolean verifyName;
        private final File clientCertFile;
        private final File clientKeyFile;
        private final String clientKeyFilePassphrase;

        public SSLOptions(File keyStoreFile, String keyStoreFilePassphrase, File certFile, boolean verifyName,
                File clientCertFile, File clientKeyFile, String clientKeyFilePassphrase) {
            this.keyStoreFile = keyStoreFile;
            this.keyStoreFilePassphrase = keyStoreFilePassphrase;
            this.trustCertFile = certFile;
            this.verifyName = verifyName;
            this.clientCertFile = clientCertFile;
            this.clientKeyFile = clientKeyFile;
            this.clientKeyFilePassphrase = clientKeyFilePassphrase;
        }

        public File getKeyStoreFile() {
            return keyStoreFile;
        }

        public String getKeyStoreFilePassphrase() {
            return keyStoreFilePassphrase;
        }

        public File getTrustCertificateFile() {
            return trustCertFile;
        }

        public boolean getVerifyName() {
            return verifyName;
        }

        public File getClientCertificateFile() {
            return clientCertFile;
        }

        public File getClientKeyFile() {
            return clientKeyFile;
        }

        public String getClientKeyFilePassphrase() {
            return clientKeyFilePassphrase;
        }

        @Override
        public String toString() {
            return super.toString()
                    + " [keyStoreFile=" + keyStoreFile
                    + ", keyStoreFilePassphrase=" + (keyStoreFilePassphrase == null ? null : "******")
                    + ", certFile=" + trustCertFile
                    + ", verifyName=" + verifyName
                    + ", clientCertFile=" + clientCertFile
                    + ", clientKeyFile=" + clientKeyFile
                    + ", clientKeyFilePassphrase=" + (clientKeyFilePassphrase == null ? null : "******") + "]";
        }

    }

    /**
     * @return a new instance of the {@link ClientOptionsBuilder} object.  This can be used to
     * build (immutable) {@link ClientOptions} objects.
     */
    public static ClientOptionsBuilder builder() {
        return new ClientOptionsBuilder();
    }

    /**
     * A builder for <code>ClientOptions</code> objects.
     */
    public static class ClientOptionsBuilder {

        private String id = null;
        private String user = null;
        private String password = null;
        private File keyStoreFile = null;
        private String keyStoreFilePassphrase = null;
        private File certFile = null;
        private boolean verifyName = true;
        private File clientCertFile = null;
        private File clientKeyFile = null;
        private String clientKeyFilePassphrase = null;

        private ClientOptionsBuilder() {}

        /**
         * Sets a client identifier, that will be associated with the <code>NonBlockingClient</code> object
         * returned by {@link NonBlockingClient#create(String, ClientOptions, NonBlockingClientListener, Object)}.
         *
         * @param id a unique identifier for this client. If this is not set then the default is the string "AUTO_"
         *           followed by a randomly chosen 7 digit hex value (with hex characters lowercase). A maximum of one
         *           instance of the client (as identified by the value of this parameter) can be connected the an
         *           MQ Light server at a given point in time.  If another instance of the same client connects, then
         *           the previously connected instance will be disconnected. This is reported, to the first client,
         *           as a ReplacedException, and the client transitioning into stopped state.
         *           When set, the id must be a minimum of 1 character and a maximum of 256 characters in length.
         *           The id can only contain alphanumeric characters, and any of the following characters:
         *           percent sign (%), slash (/), period (.), underscore (_).
         * @return the same instance of <code>ClientOptionsBuilder</code> that this method was invoked on.
         * @throws IllegalArgumentException if an invalid <code>id</code> value is specified.
         */
        public ClientOptionsBuilder setId(String id) throws IllegalArgumentException {
            final String methodName = "setId";
            logger.entry(this, methodName, id);

            if (id != null) {
                if (id.length() > 256) {
                  final IllegalArgumentException exception = new IllegalArgumentException("Client identifier '" + id + "' is longer than the maximum ID length of 256.");
                  logger.throwing(this,  methodName, exception);
                  throw exception;
                } else if (id.length() < 1) {
                  final IllegalArgumentException exception = new IllegalArgumentException("Client identifier must be a minimum ID length of 1.");
                  logger.throwing(this,  methodName, exception);
                  throw exception;
                }
                for (int i = 0; i < id.length(); ++i) {
                    if (!"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789%/._".contains(id.substring(i, i+1))) {
                      final IllegalArgumentException exception = new IllegalArgumentException("Client identifier '" + id + "' contains invalid character: '" + id.substring(i, i+1) + "'");
                      logger.throwing(this,  methodName, exception);
                      throw exception;
                    }
                }
            }
            this.id = id;

            logger.exit(this, methodName, this);

            return this;
        }

        /**
         * Sets the credentials, that will be associated with the <code>NonBlockingClient</code> object
         * returned by {@link NonBlockingClient#create(String, ClientOptions, NonBlockingClientListener, Object)}.
         * If these values are not set (or both values are set to <code>null</code> then the client will attempt
         * to use the SASL ANNONYMOUS mechanism when it connects to the MQ Light server.
         * @param user the user name that the client will identify itself using.
         * @param password the password that the client will use to authenticate itself.
         * @return the same instance of <code>ClientOptionsBuilder</code> that this method was invoked on.
         */
        public ClientOptionsBuilder setCredentials(String user, String password) {
            this.user = user;
            this.password = password;
            return this;
        }

        /**
         * Specifies a key store file containing the client's SSL/TLS
         * certificate, private key and the SSL/TLS certificates that the client
         * will trust. This can be a file in one of the following formats:
         * <ul>
         * <li>JKS - Java Key store</li>
         * <li>JCEKS - JCE key store.</li>
         * <li>PKCS12 - A PKCS#12 format key store.</li>
         * <li>CMS - Certificate Management Services key store. File extension .kdb</li>
         * </ul>
         *
         * @param keyStoreFile
         *            The key store file.
         * @return the same instance of <code>ClientOptionsBuilder</code> that
         *         this method was invoked on.
         */
        public ClientOptionsBuilder setSslKeyStore(File keyStoreFile) {
            final String methodName = "setSslKeyStore";
            logger.entry(this, methodName, keyStoreFile);

            if (this.certFile != null || this.clientCertFile != null || this.clientKeyFile != null || this.clientKeyFilePassphrase != null) {
                final IllegalArgumentException exception = new IllegalArgumentException("The SSL KeyStore and other SSL options cannot be specified together");
                logger.throwing(this,  methodName, exception);
                throw exception;
            }
            this.keyStoreFile = keyStoreFile;

            logger.exit(this, methodName, this);

            return this;
        }

        /**
         * Specifies the passphrase used to access the key store file.
         *
         * @param passphrase
         *            The passphrase used to access the key store file.
         * @return the same instance of <code>ClientOptionsBuilder</code> that
         *         this method was invoked on.
         */
        public ClientOptionsBuilder setSslKeyStorePassphrase(String passphrase) {
            final String methodName = "setSslKeyStorePassphrase";
            logger.entry(this, methodName, passphrase);

            if (this.certFile != null || this.clientCertFile != null || this.clientKeyFile != null || this.clientKeyFilePassphrase != null) {
                final IllegalArgumentException exception = new IllegalArgumentException("The SSL KeyStore and other SSL options cannot be specified together");
                logger.throwing(this,  methodName, exception);
                throw exception;
            }
            this.keyStoreFilePassphrase = passphrase;

            logger.exit(this, methodName, this);

            return this;
        }

        /**
         * Specifies a X.509 certificate chain file for SSL/TLS certificates
         * that the client will trust. This can either be a file in PEM format
         * or a Java KeyStore (JKS) file.
         *
         * @param certificateFile
         *            a trust store that contains SSL/TLS certificates that the
         *            client is to trust. If this is not set (or is set to null)
         *            then the client will use the set of trusted certificates
         *            supplied with the JVM.
         * @return the same instance of <code>ClientOptionsBuilder</code> that
         *         this method was invoked on.
         * @throws IllegalArgumentException
         *             if this option is specified when a key store option has
         *             already been specified.
         */
        public ClientOptionsBuilder setSslTrustCertificate(File certificateFile) {
            final String methodName = "setSslTrustCertificate";
            logger.entry(this, methodName, certificateFile);

            if (this.keyStoreFile != null || this.keyStoreFilePassphrase != null) {
                final IllegalArgumentException exception = new IllegalArgumentException("The SSL TrustCertificate and KeyStore options cannot be specified together");
                logger.throwing(this,  methodName, exception);
                throw exception;
            }
            this.certFile = certificateFile;

            logger.exit(this, methodName, this);

            return this;
        }

        /**
         * Determines whether the client validates that the CN name of the
         * server's certificate matches its DNS name.
         *
         * @param verifyName
         *            should the client validate the server's CN name? If this
         *            method is not called, the default is to behave as if this
         *            method was called with a value of <code>true</code>.
         * @return the same instance of <code>ClientOptionsBuilder</code> that
         *         this method was invoked on.
         */
        public ClientOptionsBuilder setSslVerifyName(boolean verifyName) {
            this.verifyName = verifyName;
            return this;
        }

        /**
         * Specifies a X.509 certificate file for the client's SSL/TLS
         * certificate. This file is expected to be in PEM format.
         *
         * @param certificateFile
         *            a PEM format file that contains the client's SSL/TLS
         *            certificate.
         * @return the same instance of <code>ClientOptionsBuilder</code> that
         *         this method was invoked on.
         */
        public ClientOptionsBuilder setSslClientCertificate(File certificateFile) {
            final String methodName = "setSslClientCertificate";
            logger.entry(this, methodName, certificateFile);

            if (this.keyStoreFile != null || this.keyStoreFilePassphrase != null) {
                final IllegalArgumentException exception = new IllegalArgumentException("The SSL ClientCertificateFile and KeyStore options cannot be specified together");
                logger.throwing(this,  methodName, exception);
                throw exception;
            }
            this.clientCertFile = certificateFile;

            logger.exit(this, methodName, this);

            return this;
        }

        /**
         * Specifies a X.509 certificate containing the private key for the
         * client. This file is expected to be in PEM format.
         *
         * @param keyFile
         *            a PEM format file that contains the SSL/TLS private key
         *            for the client.
         * @return the same instance of <code>ClientOptionsBuilder</code> that
         *         this method was invoked on.
         */
        public ClientOptionsBuilder setSslClientKey(File keyFile) {
            final String methodName = "setSslClientKey";
            logger.entry(this, methodName, keyFile);

            if (this.keyStoreFile != null || this.keyStoreFilePassphrase != null) {
                final IllegalArgumentException exception = new IllegalArgumentException("The SSL ClientKeyFile and KeyStore options cannot be specified together");
                logger.throwing(this,  methodName, exception);
                throw exception;
            }
            this.clientKeyFile = keyFile;

            logger.exit(this, methodName, this);

            return this;
        }

        /**
         * Specifies the passphrase used to decrypt the client private key, when
         * it is stored in an encrypted format.
         *
         * @param passphrase
         *            The passphrase used to decrypt the client private key.
         * @return the same instance of <code>ClientOptionsBuilder</code> that
         *         this method was invoked on.
         */
        public ClientOptionsBuilder setSslClientKeyPassphrase(String passphrase) {
            final String methodName = "setSslClientKeyPassphrase";
            logger.entry(this, methodName, passphrase);

            if (this.keyStoreFile != null || this.keyStoreFilePassphrase != null) {
                final IllegalArgumentException exception = new IllegalArgumentException("The SSL ClientKeyPassphrase and KeyStore options cannot be specified together");
                logger.throwing(this,  methodName, exception);
                throw exception;
            }
            this.clientKeyFilePassphrase = passphrase;

            logger.exit(this, methodName, this);

            return this;
        }

        /**
         * @return an instance of the <code>ClientOptions</code> object, built using the various
         *         settings of this <code>ClientOptionsBuilder</code> class at the point this method
         *         is invoked.
         */
        public ClientOptions build() {
            final SSLOptions sslOptions = new SSLOptions(keyStoreFile, keyStoreFilePassphrase, certFile, verifyName, clientCertFile, clientKeyFile, clientKeyFilePassphrase);
            return new ClientOptions(id, user, password, sslOptions);
        }
    }
}
