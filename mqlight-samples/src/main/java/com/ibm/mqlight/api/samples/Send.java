/*
 * <copyright
 * notice="lm-source-program"
 * pids="5725-P60"
 * years="2015"
 * crc="3568777996" >
 * Licensed Materials - Property of IBM
 *
 * 5725-P60
 *
 * (C) Copyright IBM Corp. 2015
 *
 * US Government Users Restricted Rights - Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with
 * IBM Corp.
 * </copyright>
 */

package com.ibm.mqlight.api.samples;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.ClientOptions;
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
        /* TODO: not supported yet...
        out.println("  -c FILE, --trust-certificate=FILE\n" +
                    "                        use the certificate contained in FILE (in PEM format) to\n" +
                    "                        validate the identity of the server. The connection must\n" +
                    "                        be secured with SSL/TLS (e.g. the service URL must start\n" +
                    "                        with 'amqps://')");
        */
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
            opts = optsBuilder.build();
        }

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
                if (repeat-- == 0) {
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
        parser.expect("-h", "--help", Boolean.class, null)
              .expect("-s", "--service", String.class, "amqp://localhost")
        /*      .expect("-c", "--trust-certificate", String.class, null)  TODO: not implemented yet... */
              .expect("-t", "--topic", String.class, "public")
              .expect("-i", "--id", String.class, null)
              .expect(null, "--message-ttl", Integer.class, null)
              .expect("-d", "--delay", Double.class, 0.0)
              .expect("-r", "--repeat", Integer.class, 0)
              .expect(null, "--sequence", Boolean.class, null)
              .expect("-f", "--file", String.class, null);
        ArgumentParser.Results tmpArgs = null;;
        try {
            tmpArgs = parser.parse(cmdline);
        } catch(IllegalArgumentException e) {
            System.err.println(e.getMessage());
            showUsage();
            System.exit(0);
        }
        final ArgumentParser.Results args = tmpArgs;

        if (args.parsed.get("-h").equals(true)) {
            showUsage();
            System.exit(0);
        }

        ClientOptions clientOpts = null;
        if (args.parsed.containsKey("-i")) {
            clientOpts = ClientOptions.builder().setId((String)args.parsed.get("-i")).build();
        }

        NonBlockingClient.create((String)args.parsed.get("-s"), clientOpts, new NonBlockingClientAdapter<Void>() {
            @Override
            public void onStarted(NonBlockingClient client, Void context) {
                System.out.printf("Connected to %s using client-id %s\n", client.getService(), client.getId());
                String topic = (String)args.parsed.get("-t");
                SendOptionsBuilder optsBuilder = SendOptions.builder().setQos(QOS.AT_LEAST_ONCE);
                if (args.parsed.containsKey("--message-ttl")) {
                    optsBuilder.setTtl((Integer)args.parsed.get("--message-ttl") * 1000);
                }
                SendOptions opts = optsBuilder.build();

                if (args.parsed.containsKey("-f")) {
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
                if (throwable != null) {
                    System.err.println("*** error ***");
                    System.err.println(throwable.getMessage());
                }
                scheduledExecutor.shutdownNow();
                System.out.println("Exiting");
                System.exit(1);
            }
        }, null);
    }
}
