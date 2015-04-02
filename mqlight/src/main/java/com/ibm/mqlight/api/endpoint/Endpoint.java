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
package com.ibm.mqlight.api.endpoint;

import java.io.File;

/**
 * Bundles information that the client uses when establishing connections
 * to the system hosting the MQ Light server.
 */
public interface Endpoint {

    /**
     * @return the host name of the system to connect to.
     */
    String getHost();

    /**
     * @return the port number to connect to.
     */
    int getPort();

    /**
     * @return indicates whether an SSL/TLS protected connection should
     *         be used.
     */
    boolean useSsl();

    /**
     * @return an (optional) X.509 certificate chain file to use for the SSL/TLS
     *         protected connection
     */
    File getCertChainFile();

    /**
     * @return a {@code boolean} indicating whether the client validates that
     *         the CN name of the server's certificate matches its DNS name.
     */
    boolean getVerifyName();

    /**
     * @return the user name to use as part of a SASL PLAIN flow used to
     *         authenticate the client.  If this value is <code>null</code>
     *         then the SASL ANONYMOUS mechanism will be used instead.
     */
    String getUser();

    /**
     * @return the password to use as part of a SASL PLAIN flow used to
     *         authenticate the client.
     */
    String getPassword();

    /**
     * @return the maximum idle period between activity (frames) on the
     *         connection that the client desires from this endpoint.
     */
    int getIdleTimeout();
}
