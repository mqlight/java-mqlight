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

import java.io.File;

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

    private final String id;
    private final String user;
    private final String password;
    private final File certFile;
    private final boolean verifyName;

    private ClientOptions(String id, String user, String password, File certFile, boolean verifyName) {
        this.id = id;
        this.user = user;
        this.password = password;
        this.certFile = certFile;
        this.verifyName = verifyName;
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

    public File getCertificateFile() {
        return certFile;
    }

    public boolean getVerifyName() {
        return verifyName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" [id=")
          .append(id)
          .append(", user=")
          .append(user)
          .append(", password=")
          .append(password == null ? null : "******")
          .append(", certFile=")
          .append(certFile)
          .append(", verifyName=")
          .append(verifyName)
          .append("]");
        return sb.toString();
    }

    /**
     * @return a new instance of the <code>ClientOptionsBuilder<code> object.  This can be used to
     * build (immutable) <code>ClientOptions</code> objects.
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
        private File certFile = null;
        private boolean verifyName = true;

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
         * @return the same instance of <code>ClientOptionsBuilder</code> that this method was invoked on.
         */
        public ClientOptionsBuilder setId(String id) {
            if (id != null) {
                if (id.length() > 48) {
                    throw new IllegalArgumentException("Client identifier '" + id + "' is longer than the maximum ID length of 48.");
                } else if (id.length() < 1) {
                    throw new IllegalArgumentException("Client identifier must be a minimum ID length of 1.");
                }
                for (int i = 0; i < id.length(); ++i) {
                    if (!"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789%/._".contains(id.substring(i, i+1))) {
                        throw new IllegalArgumentException("Client identifier '" + id + "' contains invalid character: '" + id.substring(i, i+1) + "'");
                    }
                }
            }
            this.id = id;
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
         * Specifies a trust store for SSL/TLS certificates that the client will trust.
         * @param certificateFile a trust store that contains SSL/TLS certificates that
         *                        the client is to trust.  If this is not set (or is set to null)
         *                        then the client will use the set of trusted certificates
         *                        supplied with the JVM.
         * @return the same instance of <code>ClientOptionsBuilder</code> that this method was invoked on.
         */
        public ClientOptionsBuilder setSslTrustCertificate(File certificateFile) {
            this.certFile = certificateFile;
            return this;
        }

        /**
         * Determines whether the client validates that the CN name of the server's certificate
         * matches its DNS name.
         * @param verifyName should the client validate the server's CN name?  If this method is
         *                   not called, the default is to behave as if this method was called
         *                   with a value of <code>true</code>.
         * @return the same instance of <code>ClientOptionsBuilder</code> that this method was invoked on.
         */
        public ClientOptionsBuilder setSslVerifyName(boolean verifyName) {
            this.verifyName = verifyName;
            return this;
        }

        /**
         * @return an instance of the <code>ClientOptions</code> object, built using the various
         *         settings of this <code>ClientOptionsBuilder</code> class at the point this method
         *         is invoked.
         */
        public ClientOptions build() {
            return new ClientOptions(id, user, password, certFile, verifyName);
        }
    }
}
