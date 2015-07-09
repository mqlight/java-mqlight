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
package com.ibm.mqlight.api.samples;

import java.io.File;
import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.ClientOptions;
import com.ibm.mqlight.api.ClientOptions.ClientOptionsBuilder;
import com.ibm.mqlight.api.CompletionListener;
import com.ibm.mqlight.api.DestinationAdapter;
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.NonBlockingClientAdapter;
import com.ibm.mqlight.api.SubscribeOptions;
import com.ibm.mqlight.api.samples.ArgumentParser.Results;

/**
 * Drives a low level of workload through MQ Light to demonstrate features of the MQ Light user interface.
 */
public class UiWorkout {

    private static ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    private static AtomicInteger messageCount = new AtomicInteger(0);
    private static Random random = new Random();

    private static final String[] loremIpsum =
            ("Lorem ipsum dolor sit amet, consectetur adipisicing elit, " +
             "sed do eiusmod tempor incididunt ut labore et dolore " +
             "magna aliqua. Ut enim ad minim veniam, quis nostrud " +
             "exercitation ullamco laboris nisi ut aliquip ex ea " +
             "commodo consequat. Duis aute irure dolor in reprehenderit " +
             "in voluptate velit esse cillum dolore eu fugiat nulla " +
             "pariatur. Excepteur sint occaecat cupidatat non proident, " +
             "sunt in culpa qui officia deserunt mollit anim id est " +
             "laborum.").split(" ");

    private static final String[][] destinations = new String[][] {
        {"shared1", "share1"} ,
        {"shared/shared2", "share2"},
        {"private1", null},
        {"private/private2", null},
        {"private/private3", null},
        {"private4", null}
    };

    private static void showUsage() {
        PrintStream out = System.out;
        out.println("Usage: UiWorkout [options]");
        out.println();
        out.println("Options:");
        out.println("  -h, --help            show this help message and exit");
        out.println("  -s URL, --service=URL service to connect to, for example:\n" +
                    "                        amqp://user:password@host:5672 or\n" +
                    "                        amqps://host:5671 to use SSL/TLS\n" +
                    "                        (default: amqp://localhost)");
        out.println("  -c FILE, --trust-certificate=FILE\n" +
                    "                        use the certificate contained in FILE (in PEM format) to\n" +
                    "                        validate the identity of the server. The connection must\n" +
                    "                        be secured with SSL/TLS (e.g. the service URL must start\n" +
                    "                        with 'amqps://')");
    }

    private static String createClientId() {
        String i = Integer.toHexString(random.nextInt());
        while(i.length() < 8) i = "0" + i;
        return "CLIENT_" + i.substring(0, 7);
    }

    public static void main(String[] cmdline) {
        scheduledExecutor.setRemoveOnCancelPolicy(true);

        ArgumentParser parser = new ArgumentParser();
        parser.expect("-h", "--help", Boolean.class, null)
              .expect("-s", "--service", String.class, System.getenv("VCAP_SERVICES") == null ? "amqp://localhost" : null)
              .expect("-c", "--trust-certificate", String.class, null);

        Results tmpArgs = null;
        try {
            tmpArgs = parser.parse(cmdline);
        } catch(IllegalArgumentException e) {
            System.err.println(e.getMessage());
            showUsage();
            System.exit(1);
        }
        final Results args = tmpArgs;

        if (args.parsed.get("-h").equals(true) || args.unparsed.length != 0) {
            showUsage();
            System.exit(0);
        }

        for (final String[] dest : destinations) {
            ClientOptionsBuilder optBuilder = ClientOptions.builder();
            optBuilder.setId(createClientId());
            if (args.parsed.containsKey("-c") && args.parsed.get("-c") instanceof String) {
              optBuilder.setSslTrustCertificate(new File((String) args.parsed.get("-c")));
            }
            NonBlockingClient.create((String)args.parsed.get("-s"), optBuilder.build(), new NonBlockingClientAdapter<Void>() {

                @Override
                public void onStarted(NonBlockingClient client, Void context) {
                    System.out.printf("Connected to %s using id %s\n", client.getService(), client.getId());
                    SubscribeOptions subOpts;
                    if (dest[1] == null) {
                        subOpts = SubscribeOptions.builder().build();
                    } else {
                        subOpts = SubscribeOptions.builder().setShare(dest[1]).build();
                    }
                    client.subscribe(dest[0], subOpts, new DestinationAdapter<Void>() {}, new CompletionListener<Void>() {
                        @Override
                        public void onSuccess(final NonBlockingClient client, Void context) {
                            if (dest[1] == null) {
                                System.out.printf("Receiving messages from topic pattern %s\n", dest[0]);
                            } else {
                                System.out.printf("Receiving messages from topic pattern %s and share %s\n", dest[0], dest[1]);
                            }

                            scheduledExecutor.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    Random rand = new Random();
                                    try {

                                    int start = Math.abs(rand.nextInt() % (loremIpsum.length - 15));
                                    int end = start + 5 + Math.abs(rand.nextInt() % 10);
                                    String message = "";
                                    for (int i = start; i < end; ++i) {
                                        message += loremIpsum[i];
                                        if (i+1 < end) message += " ";
                                    }
                                    client.send(destinations[Math.abs(rand.nextInt() % destinations.length)][0], message, null);
                                    } catch(Exception e) {
                                        e.printStackTrace();
                                    }
                                    int c = messageCount.getAndIncrement();
                                    if (c == 0) {
                                        System.out.println("Sending messages");
                                    } else if ((c+1) % 10 == 0) {
                                        System.out.printf("Sent %d messages\n", c+1);
                                    }
                                    scheduledExecutor.schedule(this, Math.abs(rand.nextInt() % 20000), TimeUnit.MILLISECONDS);
                                }

                            }, Math.round(20000 * Math.random()), TimeUnit.MILLISECONDS);
                        }
                        @Override
                        public void onError(NonBlockingClient client, Void context, Exception exception) {
                            System.err.printf("Problem with subscribe request: %s\n", exception.getMessage());
                            client.stop(null, null);
                        }
                    }, null);
                }

                @Override
                public void onRetrying(NonBlockingClient client, Void context, ClientException throwable) {
                    System.err.println("*** error ***");
                    if (throwable != null) System.err.println(throwable.getMessage());
                    client.stop(null, null);
                }

                @Override
                public void onStopped(NonBlockingClient client, Void context, ClientException throwable) {
                    final int exitCode;
                    if (throwable != null) {
                        System.err.println("*** error ***");
                        System.err.println(throwable.getMessage());
                        exitCode = 1;
                    } else {
                        exitCode = 0;
                    }
                    scheduledExecutor.shutdownNow();
                    System.out.println("Exiting");
                    System.exit(exitCode);
                }

            }, null);
        }
    }
}
