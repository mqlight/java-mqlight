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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.ClientRuntimeException;
import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.endpoint.EndpointPromise;
import com.ibm.mqlight.api.impl.LogbackLogging;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;

public class BluemixEndpointService extends EndpointServiceImpl {
    private static final Logger logger = LoggerFactory.getLogger(BluemixEndpointService.class);

    static {
        LogbackLogging.setup();
    }

    private static final int THREAD_POOL_CORE_THREADS = Integer.getInteger("com.ibm.mqlight.BluemixEndpointService.coreThreads",5);
    private static final int THREAD_POOL_MAX_THREADS = Integer.getInteger("com.ibm.mqlight.BluemixEndpointService.maxThreads",5);
    private static final long THREAD_POOL_KEEP_ALIVE_SECONDS = Integer.getInteger("com.ibm.mqlight.BluemixEndpointService.keepAliveSeconds",5);
    static {
        logger.data("clinit>", new Object[] { "THREAD_POOL_CORE_THREADS: ", THREAD_POOL_CORE_THREADS });
        logger.data("clinit>", new Object[] { "THREAD_POOL_CORE_THREADS: ", THREAD_POOL_MAX_THREADS });
        logger.data("clinit>", new Object[] { "THREAD_POOL_CORE_THREADS: ", THREAD_POOL_KEEP_ALIVE_SECONDS });
        if (THREAD_POOL_CORE_THREADS <= 0 || THREAD_POOL_CORE_THREADS > THREAD_POOL_MAX_THREADS) {
            throw new ClientRuntimeException("Invalid value (" + THREAD_POOL_CORE_THREADS +
                    ") specified for System property com.ibm.mqlight.BluemixEndpointService.coreThreads (must be > 0 and < the value specified for "+
                    "System property com.ibm.mqlight.BluemixEndpointService.maxThreads, or 5 when com.ibm.mqlight.BluemixEndpointService.maxThreads is not defined.");
        }
        if (THREAD_POOL_KEEP_ALIVE_SECONDS < 0) {
            throw new ClientRuntimeException("Invalid value (" + THREAD_POOL_KEEP_ALIVE_SECONDS +
                    ")specified for System property com.ibm.mqlight.BluemixEndpointService.keepAliveSeconds (must be >= 0).");
        }
    }

    private static ThreadPoolExecutor executor;

    private static final Pattern defaultServiceLabelPattern = Pattern.compile("(mqlight.*)|(messagehub.*)|(user-provided)");
    private static final Pattern defaultServiceNamePattern = Pattern.compile(".*");

    private final Pattern serviceLabelPattern;
    private final Pattern serviceNamePattern;

    private static class State {
        String lookupUri;
        int retryCount;
        String user;
        String password;
        LinkedList<Endpoint> endpoints;
        int nextEndpointIndex;
    }
    private final State state = new State();

    private static class BluemixThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger number = new AtomicInteger();
        protected BluemixThreadFactory() {
            SecurityManager sm = System.getSecurityManager();
            group = sm == null ? Thread.currentThread().getThreadGroup() : sm.getThreadGroup();
        }
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(group, runnable, "bluemix-endpoint-" + number.getAndIncrement());
        }
    }

    public BluemixEndpointService(Pattern label, Pattern name) {
        serviceLabelPattern = (label == null ? defaultServiceLabelPattern : label);
        serviceNamePattern = (name == null ? defaultServiceNamePattern : name);
    }

    protected String getVcapServices() {
        return System.getenv("VCAP_SERVICES");
    }

    protected String getConnectionUriKey() {
        return (System.getenv("MQLIGHT_JAVA_BLUEMIX_DISABLE_TLS") == null) ? "connectionLookupURI" : "nonTLSConnectionLookupURI";
    }

    protected String hitUri(String httpUri) throws IOException {
        final String methodName = "hitUri";
        logger.entry(this, methodName, httpUri);

        URL url = new URL(httpUri);
        InputStream in = url.openStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte buffer[] = new byte[1024];
        while(true) {
            int amount = in.read(buffer);
            if (amount < 0) break;
            out.write(buffer, 0, amount);
        }

        final String result = out.toString("UTF-8");

        logger.exit(this, methodName, result);

        return result;
    }

    protected void doHttpLookup(final String httpUri, final EndpointPromise future) {
        final String methodName = "doHttpLookup";
        logger.entry(this, methodName, httpUri, future);

        synchronized(this) {
            if (executor == null) {
                executor = new ThreadPoolExecutor(THREAD_POOL_CORE_THREADS, THREAD_POOL_MAX_THREADS, THREAD_POOL_KEEP_ALIVE_SECONDS,
                                                  TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new BluemixThreadFactory());
            }
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final String methodName = "run";
                logger.entry(this, methodName);

                try {
                    String serviceJson = hitUri(httpUri);

                    JsonParser parser = new JsonParser();
                    JsonObject root = parser.parse(serviceJson).getAsJsonObject();
                    JsonArray services = root.get("service").getAsJsonArray();
                    Endpoint endpoint = null;
                    synchronized(this) {
                        state.endpoints = new LinkedList<>();
                        for (JsonElement serviceElement : services) {
                            String uri = serviceElement.getAsString();
                            state.endpoints.add(new EndpointImpl(uri, state.user, state.password));
                        }

                        if (state.endpoints.isEmpty()) {
                            state.endpoints = null;
                            state.nextEndpointIndex = 0;
                        } else {
                            endpoint = state.endpoints.get(0);
                            state.nextEndpointIndex = 1;
                        }
                    }

                    if (endpoint == null) {
                        doRetry(future);
                    } else {
                        future.setSuccess(endpoint);
                    }
                } catch(IOException ioException) {
                    logger.data(this, methodName, "will retry due to java.io.IOException exception", ioException.getLocalizedMessage());
                    // Retry later...
                    doRetry(future);
                } catch(JsonParseException parseException) {
                    final ClientException exception = new ClientException("Could not parse the JSON returned by the IBM MQ Light Bluemix lookup service.  See linked exception for more information", parseException);
                    logger.data(this, methodName, exception);
                    future.setFailure(exception);
                } catch(IllegalArgumentException iae) {
                    final ClientException exception = new ClientException("Endpoint information returned by IBM MQ Light Bluemix lookup service was not valid.  See linked exception for more information", iae);
                    logger.data(this, methodName, exception);
                    future.setFailure(exception);
                }

                logger.exit(this, methodName);
            }
        });

        logger.exit(this, methodName);
    }

    protected void doRetry(EndpointPromise future) {
        final String methodName = "doRetry";
        logger.entry(this, methodName, future);

        int retry;
        synchronized(state) {
            retry = state.retryCount;
            ++state.retryCount;
        }
        future.setWait(calculateDelay(retry));

        logger.exit(this, methodName);
    }

    private void parseVCAPJson() throws JsonParseException {
        String vcapServices = getVcapServices();
        if (vcapServices != null) {
            JsonParser parser = new JsonParser();
            JsonObject root = parser.parse(vcapServices).getAsJsonObject();
            Set<JsonObject> serviceObjects = new HashSet<>();

            // Iterate over all of the services in VCAP_SERVICES
            for (Map.Entry<String, JsonElement> rootObjectProperty : root.entrySet()) {
                JsonArray arrayOfServices = rootObjectProperty.getValue().getAsJsonArray();
                for (JsonElement serviceElement : arrayOfServices) {
                    JsonObject service = serviceElement.getAsJsonObject();

                    // For each service that has name that matches 'serviceNamePattern' and a label
                    // that matches 'serviceLabelPattern'...
                    if (service.has("label") &&
                            serviceLabelPattern.matcher(service.get("label").getAsString()).matches() &&
                            service.has("name") &&
                            serviceNamePattern.matcher(service.get("name").getAsString()).matches()) {

                        // Skip over any services that are labelled 'user-provided - but don't
                        // contain the required attributes of an MQ Light service
                        if ("user-provided".equals(service.get("label").getAsString())) {
                            JsonObject credentials = service.get("credentials").getAsJsonObject();
                            if (!credentials.has("username") || !credentials.has("connectionLookupURI") || !credentials.has("password")) {
                                continue;
                            }
                        }

                        serviceObjects.add(service);
                    }
                }
            }

            if (serviceObjects.size() > 1) {
                // It is an error if more than one service matches
                StringBuilder errorMsg =
                        new StringBuilder("Multiple services were found in VCAP_SERVICES. ");
                errorMsg.append("Use the serviceLabel and serviceName methods of BluemixNonBlockingClientBuilder to disambiguate. ");
                errorMsg.append("Candidate services are: [");
                Iterator<JsonObject> serviceIterator = serviceObjects.iterator();
                while(serviceIterator.hasNext()) {
                    JsonObject service = serviceIterator.next();
                    errorMsg.append("label: ").append(service.get("label").getAsString()).append(",");
                    errorMsg.append("name: ").append(service.get("name").getAsString());
                    if (serviceIterator.hasNext()) {
                        errorMsg.append(", ");
                    }
                }
                errorMsg.append("]");
                throw new JsonParseException(errorMsg.toString());
            } else if (serviceObjects.size() == 1) {
                JsonObject service = serviceObjects.iterator().next();
                JsonObject credentials = service.get("credentials").getAsJsonObject();
                state.user = credentials.get("username").getAsString();
                state.lookupUri = credentials.get("connectionLookupURI").getAsString();
                state.password = credentials.get("password").getAsString();
            }

        }
    }

    @Override
    public void lookup(EndpointPromise future) {
        final String methodName = "lookup";
        logger.entry(this, methodName, future);

        try {
            String lookupUri;
            Endpoint endpoint = null;
            boolean retry = false;

            synchronized(state) {
                if (state.lookupUri == null) {
                    parseVCAPJson();
                }

                lookupUri = state.lookupUri;
                if (state.lookupUri != null) {
                    if (state.endpoints == null) {
                        doHttpLookup(state.lookupUri, future);
                    } else if (state.nextEndpointIndex >= state.endpoints.size()) {
                        state.endpoints = null;
                        retry = true;
                    } else {
                        endpoint = state.endpoints.get(state.nextEndpointIndex++);
                    }
                }
            }

            if (lookupUri == null) {
                final ClientException exception =
                  new ClientException("Could not locate a valid IBM Bluemix VCAP_SERVICES environment variable. Check 'service' parameter to NonBlockingClient.create(...) method.");
                logger.data(this, methodName, exception);
                future.setFailure(exception);
            } else if (retry) {
                doRetry(future);
            } else if (endpoint != null) {
                future.setSuccess(endpoint);
            }
        } catch(JsonParseException e) {
            // Can't parse VCAP_SERVICES values
            final ClientException exception =
              new ClientException("Could not parse the JSON present in the IBM Bluemix VCAP_SERVICES environment variable.  See linked exception for more information", e);
            logger.data(this, methodName, exception);
            future.setFailure(exception);
        }

        logger.exit(this, methodName);
    }

    @Override
    public void onSuccess(Endpoint endpoint) {
        final String methodName = "onSuccess";
        logger.entry(this, methodName, endpoint);

        synchronized(state) {
            int index = -1;
            if (state.endpoints != null) {
                index = state.endpoints.indexOf(endpoint);
            }

            if (index == 0) {
                state.nextEndpointIndex = 0;
            } else if (index > 0) {
                // Shuffle to front
                state.endpoints.remove(index);
                state.endpoints.addFirst(endpoint);
                state.nextEndpointIndex = 0;
            }
        }

        logger.exit(this, methodName);
    }

}
