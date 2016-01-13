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
package com.ibm.mqlight.api.impl.endpoint;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import com.ibm.mqlight.api.ClientOptions.SSLOptions;
import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

class EndpointImpl implements Endpoint {
    private static final Logger logger = LoggerFactory.getLogger(EndpointImpl.class);

    /**
     * Property to allow the user to set a local idle timeout (in milliseconds)
     * which will request that the server sends data at least that often.
     * Defaults to 0, which disables the functionality.
     */
    private static final int DEFAULT_IDLE_TIMEOUT = Integer.getInteger("com.ibm.mqlight.api.idleTimeout", 0);

    private final URI uri;
    private String host;
    private int port;
    private boolean useSsl;
    private String user;
    private String password;
    private final int idleTimeout;
    private final SSLOptions sslOptions;

    protected EndpointImpl(final String uri, final String user,
            final String password) throws IllegalArgumentException {
        this(uri, user, password, null);
    }

    protected EndpointImpl(final String uri, final String user,
                           final String password, final SSLOptions sslOptions)
        throws IllegalArgumentException {
        final String methodName = "<init>";
        logger.entry(this, methodName, uri, user, "******", sslOptions);

        if (user == null && password != null) {
            final IllegalArgumentException exception = new IllegalArgumentException("Can't have an empty user ID if you specify a password!");
            logger.throwing(this, methodName, exception);
            throw exception;
        } else if (user != null && password == null) {
            final IllegalArgumentException exception = new IllegalArgumentException("Can't have an empty password if you specify a user ID!");
            logger.throwing(this, methodName, exception);
            throw exception;
        }
        port = 5672;
        useSsl = false;
        URI serviceUri;
        try {
            serviceUri = new URI(uri);
            if (serviceUri.getScheme() == null) {
                final IllegalArgumentException exception = new IllegalArgumentException("No scheme in service URI");
                logger.throwing(this, methodName, exception);
                throw exception;
            }
            String protocol = serviceUri.getScheme().toLowerCase();
            if ("amqps".equals(protocol)) {
                port = 5671;
                useSsl = true;
            } else if (!"amqp".equals(protocol)) {
                final IllegalArgumentException exception = new IllegalArgumentException("Invalid protocol : " + protocol);
                logger.throwing(this, methodName, exception);
                throw exception;
            }
            if (serviceUri.getHost() == null) {
                final IllegalArgumentException exception = new IllegalArgumentException("No host in service URI");
                logger.throwing(this, methodName, exception);
                throw exception;
            }
            host = serviceUri.getHost();
            if (serviceUri.getPort() > -1) {
                port = serviceUri.getPort();
            }

            String userInfo = serviceUri.getUserInfo();
            if (userInfo == null) {
                if (user != null) {
                    this.user = user;
                    this.password = password;
                }
            } else {
                if (user != null) {
                    final IllegalArgumentException exception = new IllegalArgumentException("User/password information both specified and in service URI");
                    logger.throwing(this, methodName, exception);
                    throw exception;
                }
                String[] userInfoSplit = userInfo.split(":");
                if (userInfoSplit.length == 1) {
                    final IllegalArgumentException exception = new IllegalArgumentException("If user information is specified in the URI, a password must also be specified");
                    logger.throwing(this, methodName, exception);
                    throw exception;
                } else if (userInfoSplit.length > 2) {
                    final IllegalArgumentException exception = new IllegalArgumentException("Invalid user/password information in service URI");
                    logger.throwing(this, methodName, exception);
                    throw exception;
                }
                this.user = userInfoSplit[0];
                if ("".equals(this.user)) {
                    throw new IllegalArgumentException("URI has a password but no user information");
                }
                this.password = userInfoSplit[1];
            }

            if (serviceUri.getPath() != null
                    && serviceUri.getPath().length() > 0
                    && !serviceUri.getPath().equals("/")) {
                final IllegalArgumentException exception =
                    new IllegalArgumentException("Unsupported URL '" + uri + "' paths (" + serviceUri.getPath() + ") " + "can't be part of a service URL");
                logger.throwing(this, methodName, exception);
                throw exception;
            }
        } catch (URISyntaxException e) {
            final IllegalArgumentException exception = new IllegalArgumentException("service URI not valid", e);
            logger.throwing(this, methodName, exception);
            throw exception;
        }

        // Validate the SSL options
        if (sslOptions != null) {
            validateFileOption("sslTrustCertificate", sslOptions.getTrustCertificateFile());
            validateFileOption("sslKeyStore", sslOptions.getKeyStoreFile());
            validateFileOption("sslClientCertificate", sslOptions.getClientCertificateFile());
            validateFileOption("sslClientKey", sslOptions.getClientKeyFile());

            if (sslOptions.getKeyStoreFile() != null
                    && (sslOptions.getTrustCertificateFile() != null || sslOptions.getClientCertificateFile() != null || sslOptions
                            .getClientKeyFile() != null)) {
                final IllegalArgumentException exception = new IllegalArgumentException(
                        "The SslKeyStore and other SSL options cannot be specified together");
                logger.throwing(this, methodName, exception);
                throw exception;
            }

            if ((sslOptions.getClientKeyFile() != null && sslOptions.getClientCertificateFile() == null)
                    || (sslOptions.getClientKeyFile() == null && sslOptions.getClientCertificateFile() != null)) {
                final IllegalArgumentException exception = new IllegalArgumentException(
                        "The SslClientCertificate and SslClientKey options must be specified together");
                logger.throwing(this, methodName, exception);
                throw exception;
            }
            
            this.sslOptions = sslOptions;
        } else {
            this.sslOptions = new SSLOptions(null, null, null, false, null, null, null);
        }
        
        this.idleTimeout = DEFAULT_IDLE_TIMEOUT;
        this.uri = serviceUri;
        
        logger.exit(this, methodName);
    }

    private void validateFileOption(String optionName, File file) {
        final String methodName = "validateFileOption";
        logger.entry(this, methodName, optionName, file);
        if (file != null) {
            if (!file.exists()) {
                final IllegalArgumentException exception = new IllegalArgumentException(
                        "The file specified for "+optionName+" '" + file.getPath() + "' does not exist");
                logger.throwing(this, methodName, exception);
                throw exception;
            }
            if (!file.isFile()) {
                final IllegalArgumentException exception = new IllegalArgumentException(
                        "The file specified for "+optionName+" '" + file.getPath() + "' is not a regular file");
                logger.throwing(this, methodName, exception);
                throw exception;
            }
        }
        
        logger.exit(this, methodName);
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public boolean useSsl() {
        return useSsl;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public int getIdleTimeout() {
        return idleTimeout;
    }

    @Override
    public URI getURI() {
      return uri;
    }

    @Override
    public SSLOptions getSSLOptions() {
      return sslOptions;
    }

}
