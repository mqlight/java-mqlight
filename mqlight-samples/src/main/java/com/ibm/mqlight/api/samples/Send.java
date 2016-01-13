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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.ClientOptions;
import com.ibm.mqlight.api.ClientOptions.ClientOptionsBuilder;
import com.ibm.mqlight.api.CompletionListener;
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.NonBlockingClientAdapter;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.SendOptions;
import com.ibm.mqlight.api.SendOptions.SendOptionsBuilder;

public class Send {

    private static void showUsage() {
        PrintStream out = System.out;
        out.println("Usage: Send [options] <msg_1> ... <msg_n>");
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
        out.println("  -t TOPIC, --topic=TOPIC");
        out.println("                        send messages to topic TOPIC\n" +
                    "                        (default: public)");
        out.println("  -i ID, --id=ID        the ID to use when connecting to MQ Light\n" +
                    "                        (default: send_[0-9a-f]{7})");
        out.println("  --message-ttl=NUM     set message time-to-live to NUM seconds");
        out.println("  -d NUM, --delay=NUM   add NUM seconds delay between each request");
        out.println("  -r NUM, --repeat=NUM  send messages NUM times, default is 1, if\n" +
                    "                        NUM <= 0 then repeat forever");
        out.println("   --sequence           prefix a sequence number to the message\n" +
                    "                        payload (ignored for binary messages)");
        out.println("  -f FILE, --file=FILE  send FILE as binary data. Cannot be\n" +
                    "                        specified at the same time as <msg1>");
        out.println();
    }

    private static ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);

    protected static class SendRunnable implements Runnable, CompletionListener<String> {
        private final NonBlockingClient client;
        private final String topic;
        private final long delay;
        private final boolean sequence;
        private int sequenceNumber = 0;
        private int repeat;
        private final String[] messages;
        private int messageIndex = 0;
        private final SendOptions opts;

        protected SendRunnable(NonBlockingClient client, Map<String, Object> args, String[] messages) {
            this.client = client;
            topic = (String)args.get("-t");
            delay = Math.round(1000 * (Double)args.get("-d"));
            repeat = (Integer)args.get("-r");
            this.messages = messages;
            this.sequence = (Boolean)args.get("--sequence");
            SendOptionsBuilder optsBuilder = SendOptions.builder().setQos(QOS.AT_LEAST_ONCE);
            if (args.containsKey("--message-ttl")) {
                optsBuilder.setTtl((Integer)args.get("--message-ttl") * 1000);
            }
            optsBuilder.setRetainLink(repeat > 1 || messages.length > 1);
            opts = optsBuilder.build();
        }

        @Override
        public void run() {
            String msgBody = messages[messageIndex++];
            if (sequence) {
                msgBody = "" + ++sequenceNumber + ": " + msgBody;
            }
            client.send(topic, msgBody, null, opts, this, msgBody);
        }

        @Override
        public void onSuccess(NonBlockingClient client, String context) {
            System.out.println(context);
            boolean scheduleAgain = true;
            if (messageIndex == messages.length) {
                if (--repeat == 0) {
                    scheduleAgain = false;
                    client.stop(null,  null);
                } else {
                    messageIndex = 0;
                }
            }
            if (scheduleAgain) {
                scheduledExecutor.schedule(this, delay, TimeUnit.MILLISECONDS);
            }
        }

        @Override
        public void onError(NonBlockingClient client, String context, Exception exception) {
            System.err.println("Problem with send request: " + exception.getMessage());
            client.stop(null, null);
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
              .expect("-t", "--topic", String.class, "public")
              .expect("-i", "--id", String.class, null)
              .expect(null, "--message-ttl", Integer.class, null)
              .expect("-d", "--delay", Double.class, 0.0)
              .expect("-r", "--repeat", Integer.class, 1)
              .expect(null, "--sequence", null, null)
              .expect("-f", "--file", String.class, null);
        ArgumentParser.Results tmpArgs = null;
        try {
            tmpArgs = parser.parse(cmdline);
        } catch(IllegalArgumentException e) {
            System.err.println(e.getMessage());
            showUsage();
            System.exit(1);
        }
        final ArgumentParser.Results args = tmpArgs;

        if (args.parsed.get("-h").equals(true)) {
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

                if (args.parsed.containsKey("-f")) {
                    String topic = (String)args.parsed.get("-t");
                    SendOptionsBuilder optsBuilder = SendOptions.builder().setQos(QOS.AT_LEAST_ONCE);
                    if (args.parsed.containsKey("--message-ttl")) {
                        optsBuilder.setTtl((Integer)args.parsed.get("--message-ttl") * 1000);
                    }
                    optsBuilder.setRetainLink(false);
                    SendOptions opts = optsBuilder.build();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try (FileInputStream in = new FileInputStream((String)args.parsed.get("-f"))) {
                        byte[] buffer = new byte[1024 * 128];
                        while(true) {
                            int amount = in.read(buffer);
                            if (amount == -1) break;
                            out.write(buffer, 0, amount);
                        }
                        byte[] bytes = out.toByteArray();
                        client.send(topic, ByteBuffer.wrap(bytes), null, opts, new CompletionListener<byte[]>() {

                            @Override
                            public void onSuccess(NonBlockingClient client, byte[] context) {
                                System.out.print("data = { ");
                                int amount = Math.min(context.length, 16);
                                for (int i=0; i < amount; ++i) {
                                    if (i+1 == amount) {
                                        System.out.printf("%02x", context[i]);
                                    } else {
                                        System.out.printf("%02x, ", context[i]);
                                    }
                                }
                                System.out.println(amount < context.length ? ", ... }" : " }");
                                client.stop(null, null);
                            }

                            @Override
                            public void onError(NonBlockingClient client, byte[] context, Exception exception) {
                                System.err.println("Problem with send request: " + exception.getMessage());
                                client.stop(null, null);
                            }
                        }, bytes);
                    } catch(IOException ioException) {
                        System.out.println("Problem reading file ("+args.parsed.get("-f")+"): "+ioException.getMessage());
                        client.stop(null, null);
                    }
                } else {
                    String[] messages = args.unparsed;
                    if (messages.length == 0) {
                        messages = new String[] {"Hello World!"};
                    }
                    SendRunnable sendRunnable = new SendRunnable(client, args.parsed, messages);
                    scheduledExecutor.schedule(sendRunnable, 0, TimeUnit.SECONDS);
                }
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
