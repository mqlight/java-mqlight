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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.mqlight.api.BytesDelivery;
import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.ClientOptions;
import com.ibm.mqlight.api.ClientOptions.ClientOptionsBuilder;
import com.ibm.mqlight.api.CompletionListener;
import com.ibm.mqlight.api.Delivery;
import com.ibm.mqlight.api.DestinationListener;
import com.ibm.mqlight.api.MalformedDelivery;
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.NonBlockingClientAdapter;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.StringDelivery;
import com.ibm.mqlight.api.SubscribeOptions;
import com.ibm.mqlight.api.SubscribeOptions.SubscribeOptionsBuilder;
import com.ibm.mqlight.api.samples.ArgumentParser.Results;

public class Receive {

    private static ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);

    private static void showUsage() {
        PrintStream out = System.out;
        out.println("Usage: Receive [options]");
        out.println();
        out.println("Options:");
        out.println("  -h, --help            show this help message and exit");
        out.println("  -s URL, --service=URL service to connect to, for example:\n" +
                    "                        amqp://user:password@host:5672 or\n" +
                    "                        amqps://host:5671 to use SSL/TLS\n" +
                    "                        (default: amqp://localhost)");
        out.println("  -k FILE, --keystore=FILE\n" +
                    "                        use key store contained in FILE (in PKCS#12 format) to\n" +
                    "                        supply the client certificate, private key and trust\n" +
                    "                        certificates.\n" +
                    "                        The Connection must be secured with SSL/TLS (e.g. the\n" +
                    "                        service URL must start with 'amqps://').\n" +
                    "                        Option is mutually exclusive with the client-key,\n" +
                    "                        client-certificate and trust-certifcate options");
        out.println("  -p PASSPHRASE, --keystore-passphrase=PASSPHRASE\n" +
                    "                        use PASSPHRASE to access the keystore");
        out.println("  --client-certificate=FILE\n" +
                    "                        use the certificate contained in FILE (in PEM format) to\n" +
                    "                        supply the identity of the client. The connection must\n" +
                    "                        be secured with SSL/TLS");
        out.println("  --client-key=FILE     use the private key contained in FILE (in PEM format)\n" +
                    "                        for encrypting the specified client certificate");
        out.println("  --client-key-passphrase=PASSPHRASE\n" +
                    "                        use PASSPHRASE to access the client private key");
        out.println("  -c FILE, --trust-certificate=FILE\n" +
                    "                        use the certificate contained in FILE (in PEM format) to\n" +
                    "                        validate the identity of the server. The connection must\n" +
                    "                        be secured with SSL/TLS");
        out.println("  --verify-name=TRUE|FALSE\n" +
                    "                        specify whether or not to additionally check the\n" +
                    "                        server's common name in the specified trust certificate\n" +
                    "                        matches the actual server's DNS name\n" +
                    "                        (default: TRUE)");
        out.println("  -t TOPICPATTERN, --topic-pattern=TOPICPATTERN\n" +
                    "                        subscribe to receive messages matching TOPICPATTERN");
        out.println("                        (default: public)");
        out.println("  -i ID, --id=ID        the ID to use when connecting to MQ Light\n" +
                    "                        (default: recv_[0-9a-f]{7})");
        out.println("  --destination-ttl=NUM set destination time-to-live to NUM seconds");
        out.println("  -n NAME, --share-name NAME");
        out.println("                        optionally, subscribe to a shared destination using\n" +
                    "                        NAME as the share name.");
        out.println("  -f FILE, --file=FILE  write the payload of the next message received to\n" +
                    "                        FILE (overwriting previous file contents) then end.\n" +
                    "                        (default is to print messages to stdout)");
        out.println("  -d NUM, --delay=NUM   delay for NUM seconds each time a message is received.");
        out.println("  --verbose             print additional information about each message\n" +
                    "                        received.");
        out.println();
    }

    protected static class Listener implements DestinationListener<Void> {
        private final String filename;
        private final boolean verbose;
        private final long delayMillis;
        int count = 0;

        protected Listener(String filename, boolean verbose, long delayMillis) {
            this.filename = filename;
            this.verbose = verbose;
            this.delayMillis = delayMillis;
        }

        @Override
        public void onMessage(NonBlockingClient client, Void context, final Delivery delivery) {
            ++count;
            if (delivery instanceof MalformedDelivery) {
                System.err.printf("*** received malformed message (%d)\n", count);
            } else if (verbose) {
                System.out.printf("# received message (%d)\n", count);
            }
            if (filename == null) {
                if (delivery instanceof StringDelivery) {
                    System.out.println(((StringDelivery)delivery).getData());
                } else {
                    ByteBuffer buffer = ((BytesDelivery)delivery).getData();
                    byte[] data = new byte[buffer.remaining() > 16 ? buffer.remaining() : 16];
                    buffer.get(data);
                    System.out.print("data = { ");
                    int amount = Math.min(data.length, 16);
                    for (int i=0; i < amount; ++i) {
                        if (i+1 == amount) {
                            System.out.printf("%02x", data[i]);
                        } else {
                            System.out.printf("%02x, ", data[i]);
                        }
                    }
                    System.out.println(amount < data.length ? ", ... }" : " }");
                }
                if (verbose) {
                    StringBuilder sb = new StringBuilder("[ properties=");
                    sb.append(delivery.getProperties())
                      .append(", qos=").append(delivery.getQOS())
                      .append(", share=").append(delivery.getShare())
                      .append(", topic=").append(delivery.getTopic())
                      .append(", topic pattern=").append(delivery.getTopicPattern())
                      .append(", ttl=").append(delivery.getTtl())
                      .append(" ]");
                    System.out.println(sb.toString());
                }
                if (delayMillis > 0) {
                    scheduledExecutor.schedule(new Runnable() {
                        @Override
                        public void run() {
                            delivery.confirm();
                        }

                    }, delayMillis, TimeUnit.MILLISECONDS);
                }
            } else {

                try (FileOutputStream out = new FileOutputStream(filename)) {
                    ByteBuffer data;
                    if (delivery instanceof BytesDelivery) {
                        data = ((BytesDelivery)delivery).getData();
                    } else {
                        String strData = ((StringDelivery)delivery).getData();
                        data = ByteBuffer.wrap(strData.getBytes());
                    }
                    while(data.remaining() > 0) {
                        out.getChannel().write(data);
                    }
                } catch(IOException ioException) {
                    System.err.printf("Problem writing for file (%s): %s\n", filename, ioException.getMessage());
                    client.stop(null, null);
                }
            }
        }

        @Override
        public void onMalformed(NonBlockingClient client, Void context, MalformedDelivery delivery) {
            onMessage(client, context, delivery);
        }

        @Override
        public void onUnsubscribed(NonBlockingClient client, Void context, String topicPattern, String share, Exception error) {
        }
    }

    public static void main(String[] cmdline) {
        scheduledExecutor.setRemoveOnCancelPolicy(true);

        ArgumentParser parser = new ArgumentParser();
        parser.expect("-h", "--help", null, null)
              .expect("-s", "--service", String.class, "amqp://localhost")
              .expect("-k", "--keystore", String.class, null)
              .expect("-p", "--keystore-passphrase", String.class, null)
              .expect("-c", "--trust-certificate", String.class, null)
              .expect(null, "--client-certificate", String.class, null)
              .expect(null, "--client-key", String.class, null)
              .expect(null, "--client-key-passphrase", String.class, null)
              .expect(null, "--verify-name", Boolean.class, true)
              .expect("-t", "--topic-pattern", String.class, "public")
              .expect("-i", "--id", String.class, null)
              .expect(null, "--destination-ttl", Double.class, 0.0)
              .expect("-n", "--share-name", String.class, null)
              .expect("-f", "--file", String.class, null)
              .expect("-d", "--delay", Double.class, 0.0)
              .expect(null, "--verbose", null, false);

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

        ClientOptionsBuilder builder = ClientOptions.builder();
        if (args.parsed.containsKey("-i")) {
            builder.setId((String)args.parsed.get("-i"));
        }
        if (args.parsed.containsKey("-k")) {
            builder.setSslKeyStore(new File((String) args.parsed.get("-k")));
        }
        if (args.parsed.containsKey("-p")) {
            builder.setSslKeyStorePassphase((String)args.parsed.get("-p"));
        }
        if (args.parsed.containsKey("-c")
                && args.parsed.get("-c") instanceof String) {
            builder.setSslTrustCertificate(
                    new File((String) args.parsed.get("-c")));
        }
        if (args.parsed.containsKey("--verify-name")) {
            builder.setSslVerifyName((Boolean)args.parsed.get("--verify-name"));
        }
        if (args.parsed.containsKey("--client-certificate")) {
            builder.setSslClientCertificate(new File((String) args.parsed.get("--client-certificate")));
        }
        if (args.parsed.containsKey("--client-key")) {
            builder.setSslClientKey(new File((String) args.parsed.get("--client-key")));
        }
        if (args.parsed.containsKey("--client-key-passphrase")) {
            builder.setSslClientKeyPassphase((String)args.parsed.get("--client-key-passphrase"));
        }
        ClientOptions clientOpts = builder.build();

        NonBlockingClient.create((String)args.parsed.get("-s"), clientOpts, new NonBlockingClientAdapter<Void>() {
            @Override
            public void onStarted(NonBlockingClient client, Void context) {
                System.out.printf("Connected to %s using client-id %s\n", client.getService(), client.getId());

                long delayMillis = 0;
                SubscribeOptionsBuilder subOptBuilder = SubscribeOptions.builder();
                delayMillis = Math.round((Double)args.parsed.get("-d") * 1000);
                if (delayMillis > 0) {
                    subOptBuilder.setAutoConfirm(false)
                                 .setCredit(1);
                }
                subOptBuilder.setQos(QOS.AT_LEAST_ONCE);
                if (args.parsed.containsKey("--destination-ttl")) {
                    long ttl = Math.round((Double)args.parsed.get("--destination-ttl") * 1000);
                    subOptBuilder.setTtl(ttl);
                }
                if (args.parsed.containsKey("-n")) {
                    subOptBuilder.setShare((String)args.parsed.get("-n"));
                }

                Listener listener = new Listener((String)args.parsed.get("-f"), (Boolean)args.parsed.get("--verbose"), delayMillis);
                client.subscribe((String)args.parsed.get("-t"), subOptBuilder.build(), listener, new CompletionListener<Void>() {
                    @Override
                    public void onSuccess(NonBlockingClient client, Void context) {
                        if (args.parsed.containsKey("-n")) {
                            System.out.printf("Subscribed to share: %s, pattern: %s\n", args.parsed.get("-n"), args.parsed.get("-t"));
                        } else {
                            System.out.printf("Subscribed to pattern: %s\n", args.parsed.get("-t"));
                        }
                    }

                    @Override
                    public void onError(NonBlockingClient client, Void context, Exception exception) {
                        System.err.println("Problem with subscribe request: " + exception.getMessage());
                        client.stop(null, null);
                    }
                }, null);
            }

            @Override
            public void onRetrying(NonBlockingClient client, Void context,
                    ClientException throwable) {
                System.err.println("*** error ***");
                if (throwable != null) System.err.println(throwable.getMessage());
                  client.stop(null, null);
            }

            @Override
            public void onStopped(NonBlockingClient client, Void context,
                    ClientException throwable) {
                final int exitCode;
                if (throwable != null) {
                    System.err.println("*** error ***");
                    System.err.println(throwable.getMessage());
                    if (throwable.getCause() != null) {
                        System.err.println(throwable.getCause().toString());
                    }
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
