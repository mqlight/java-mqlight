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

import java.net.URI;
import java.net.URISyntaxException;

import com.ibm.mqlight.api.endpoint.Endpoint;

class EndpointImpl implements Endpoint {

    private String host;
    private int port;
    private boolean useSsl;
    private String user;
    private String password;
    
    protected EndpointImpl(String uri, String user, String password) throws IllegalArgumentException {
        // TODO: crack the URI into a host/port.
        if (user == null && password != null) {
            throw new IllegalArgumentException("Can't have an empty user ID if you specify a password!");
        } else if (user != null && password == null) {
            throw new IllegalArgumentException("Can't have an empty password if you specify a user ID!");
        }
        port = 5672;
        useSsl = false;
        try {
            URI serviceUri = new URI(uri);
            if (serviceUri.getScheme() == null) {
                throw new IllegalArgumentException("No scheme in service URI");
            }
            String protocol = serviceUri.getScheme().toLowerCase();
            if ("amqps".equals(protocol)) {
                port = 5671;
                useSsl = true;
            } else if (!"amqp".equals(protocol)) {
                throw new IllegalArgumentException("Invalid protocol : " + protocol);
            }
            if (serviceUri.getHost() == null) {
                throw new IllegalArgumentException("No host in service URI");
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
                    throw new IllegalArgumentException("User/password information both specified and in service URI");
                }
                String[] userInfoSplit = userInfo.split(":");
                if (userInfoSplit.length == 1) {
                    throw new IllegalArgumentException("If user information is specified in the URI, a password must also be specified");
                } else if (userInfoSplit.length > 2) {
                    throw new IllegalArgumentException("Invalid user/password information in service URI");
                }
                this.user = userInfoSplit[0];
                if ("".equals(this.user)) {
                    throw new IllegalArgumentException("URI has a password but no user information");
                }
                this.password = userInfoSplit[1];
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("service URI not valid", e);
        }
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

}
