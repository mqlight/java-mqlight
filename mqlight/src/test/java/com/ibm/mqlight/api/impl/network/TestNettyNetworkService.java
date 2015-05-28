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
package com.ibm.mqlight.api.impl.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.netty.buffer.ByteBuf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLServerSocketFactory;

import junit.framework.AssertionFailedError;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.endpoint.Endpoint;

public class TestNettyNetworkService {

    private static final int EVENT_WAIT_TIMEOUT_SECONDS = 10000;
    private static final int LISTENER_WAIT_TIMEOUT_SECONDS = 10000;
    private static final int NETWORK_WAIT_TIMEOUT_SECONDS = 10000;

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    private class LatchedLinkedList<T> extends LinkedList<T> {
        private static final long serialVersionUID = 8844358537632962333L;
        private final CountDownLatch latch;

        public LatchedLinkedList(int count) {
            latch = new CountDownLatch(count);
        }

        public void await(int timeout) throws InterruptedException {
            latch.await(timeout, TimeUnit.MILLISECONDS);
        }

        @Override
        public void addLast(T e) {
            super.addLast(e);
            latch.countDown();
        }
    }

    private class BaseListener implements Runnable {
        protected ServerSocket serverSocket;
        protected final int port;
        protected boolean stop = false;

        private final Thread thread;

        protected BaseListener(int port) throws IOException, InterruptedException {
            this.port = port;
            thread = new Thread(this);
            thread.setDaemon(true);
            synchronized(this) {
                thread.start();
                this.wait();
            }
        }

        protected boolean join(long timeout) throws InterruptedException {
            thread.join(timeout);
            return !thread.isAlive();
        }

        protected void stop() {
            if (serverSocket == null) {
                return;
            }

            try {
                stop = true;
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                synchronized(this) {
                    this.notifyAll();
                }
                Socket socket = serverSocket.accept();
                processSocket(socket);
                socket.close();
                serverSocket.close();
            } catch (SocketException e) {
                if (!stop) {
                    e.printStackTrace();
                }
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                synchronized(this) {
                    this.notifyAll();
                }
            }
        }

        protected void processSocket(Socket socket) throws IOException {
        }
    }

    private class BaseSslListener extends BaseListener {
        protected BaseSslListener(int port) throws IOException,
                InterruptedException {
            super(port);
        }

        @Override
        protected void processSocket(Socket socket) throws IOException {
            // XXX: should we unittest with a proper SSL handshake before
            // closing the socket?
            InputStream in = socket.getInputStream();
            while (in.read() != -1);
        }

        @Override
        public void run() {
            Socket socket = null;
            try {
                serverSocket = (SSLServerSocketFactory.getDefault())
                        .createServerSocket(port);
                synchronized(this) {
                    this.notifyAll();
                }
                socket = serverSocket.accept();
                processSocket(socket);
            } catch (SocketException e) {
                if (!stop) {
                    e.printStackTrace();
                }
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
              if (socket != null) try {
                socket.close();
              } catch (IOException e) {
              }
                synchronized(this) {
                  if (serverSocket != null) try {
                    serverSocket.close();
                  } catch (IOException e) {
                  }
                    this.notifyAll();
                }
            }
        }
    }

    private class ReceiveListener extends BaseListener {

        private final byte[] buffer = new byte[1024 * 1024];
        private int bytesRead;

        protected ReceiveListener(int port) throws IOException, InterruptedException {
            super(port);
        }

        @Override
        protected void processSocket(Socket socket) throws IOException {
            InputStream in = socket.getInputStream();
            int count = 0;
            while(true) {
                int n = in.read(buffer);
                if (n < 0) break;
                count += n;
            }
            synchronized(this) {
                bytesRead = count;
            }
        }

        protected synchronized int getBytesRead() {
            return bytesRead;
        }
    }

    private class SendListener extends BaseListener {

        private int bytesWritten;

        protected SendListener(int port) throws IOException, InterruptedException {
            super(port);
        }

        @Override
        protected void processSocket(Socket socket) throws IOException {
            OutputStream out = socket.getOutputStream();
            int count = 0;
            byte[] data = new byte[(1 << 24)];
            Arrays.fill(data, (byte)123);
            for (int i = 0; i < 25; ++i) {
                out.write(data, 0, 1 << i);
                count += 1 << i;
            }
            synchronized(this) {
                bytesWritten = count;
            }
        }

        protected synchronized int getBytesWritten() {
            return bytesWritten;
        }
    }

    private class StubEndpoint implements Endpoint {
        private final String host;
        private final int port;
        private boolean useSsl = false;
        private File certChainFile = null;
        private boolean verifyName = false;
        private StubEndpoint(String host, int port) {
            this.host = host;
            this.port = port;
        }
        @Override public String getHost() { return host; }
        @Override public int getPort() { return port; }
        @Override public boolean useSsl() { return useSsl; }
        public void setUseSsl(final boolean useSsl) { this.useSsl = useSsl; }
        @Override public File getCertChainFile() { return certChainFile; }
        public void setCertChainFile(final File certChainFile) { this.certChainFile = certChainFile; }
        @Override public boolean getVerifyName() { return verifyName; }
        public void setVerifyName(final boolean verifyName) { this.verifyName = verifyName; }
        @Override public String getUser() { return null; }
        @Override public String getPassword() { return null; }
        @Override public int getIdleTimeout() { return 0; }
        @Override public URI getURI() { URI uri = null;
                                        try { uri = new URI((useSsl() ? "amqps://" : "amqp://") + host + ":" + port);
                                            } catch (URISyntaxException e) {}
                                        return uri; }
    }

    @Test
    public void connectRemoteClose() throws Exception {
        NettyNetworkService nn = new NettyNetworkService();
        BaseListener testListener = new BaseListener(34567);

        LatchedLinkedList<Event> channelEvents = new LatchedLinkedList<Event>(1);
        LatchedLinkedList<Event> connectEvents = new LatchedLinkedList<Event>(1);
        MockNetworkListener listener = new MockNetworkListener(channelEvents);
        MockNetworkConnectPromise promise = new MockNetworkConnectPromise(connectEvents);
        nn.connect(new StubEndpoint("localhost", 34567), listener, promise);

        connectEvents.await(EVENT_WAIT_TIMEOUT_SECONDS);
        channelEvents.await(EVENT_WAIT_TIMEOUT_SECONDS);

        assertTrue("Expected promise to be marked completed", promise.isComplete());
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        assertEquals("Wrong number of connect events seen: " + connectEvents.toString(), 1, connectEvents.size());
        assertEquals("Wrong number of channel events seen: " + channelEvents.toString(), 1, channelEvents.size());
        assertEquals("Expected first event to be a connect success", Event.Type.CONNECT_SUCCESS, connectEvents.get(0).type);
        assertEquals("Expected second event to be a close", Event.Type.CHANNEL_CLOSE, channelEvents.get(0).type);

        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
    }

    @Test
    public void connectRemoteCloseSsl() throws Exception {
        NettyNetworkService nn = new NettyNetworkService();
        BaseListener testListener = new BaseSslListener(34567);

        LatchedLinkedList<Event> channelEvents = new LatchedLinkedList<Event>(2);
        LatchedLinkedList<Event> connectEvents = new LatchedLinkedList<Event>(1);
        MockNetworkListener listener = new MockNetworkListener(channelEvents);
        MockNetworkConnectPromise promise = new MockNetworkConnectPromise(connectEvents);
        final StubEndpoint endpoint = new StubEndpoint("localhost", 34567);
        endpoint.setUseSsl(true);
        endpoint.setVerifyName(false);
        nn.connect(endpoint, listener, promise);

        connectEvents.await(EVENT_WAIT_TIMEOUT_SECONDS);
        channelEvents.await(EVENT_WAIT_TIMEOUT_SECONDS);

        assertTrue("Expected promise to be marked completed", promise.isComplete());
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        assertEquals("Wrong number of connect events seen: " + connectEvents.toString(), 1, connectEvents.size());
        assertEquals("Wrong number of channel events seen: " + channelEvents.toString(), 2, channelEvents.size());
        assertEquals("Expected connect event to be successful", Event.Type.CONNECT_SUCCESS, connectEvents.get(0).type);
        assertEquals("Expected first channel event to be a channel error", Event.Type.CHANNEL_ERROR, channelEvents.get(0).type);
        assertEquals("Expected second channel event to be a close", Event.Type.CHANNEL_CLOSE, channelEvents.get(1).type);

        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
    }

    @Test
    public void sslCertFiles() throws Exception {
        NettyNetworkService nn = new NettyNetworkService();
        MockNetworkListener listener = new MockNetworkListener(new LinkedList<Event>());
        final StubEndpoint endpoint = new StubEndpoint("localhost", 34567);
        endpoint.setUseSsl(true);
        endpoint.setVerifyName(true);

        // empty file
        {
            BaseListener testListener = new BaseSslListener(34567);
            endpoint.setCertChainFile(folder.newFile());
            LatchedLinkedList<Event> events = new LatchedLinkedList<Event>(1);
            MockNetworkConnectPromise promise = new MockNetworkConnectPromise(events);
            nn.connect(endpoint, listener, promise);
            events.await(EVENT_WAIT_TIMEOUT_SECONDS);
            assertTrue("Expected promise to be marked completed", promise.isComplete());
            assertEquals("Expected first event to be a connect failure", Event.Type.CONNECT_FAILURE, events.getLast().type);
            assertTrue("Expected a last event to have a SecurityException, but instead had: "+events.getLast().context, events.getLast().context instanceof SecurityException);
            assertTrue("Expected event to throw ClientException", (events.getLast().context instanceof Exception));
            assertTrue(((Exception) events.getLast().context).getCause().toString().startsWith("java.security.cert.CertificateException"));
            testListener.stop();
            assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        }

        // JKS file
        {
            BaseListener testListener = new BaseSslListener(34567);
            final File jksFile = folder.newFile();
            final KeyStore jks = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(new File(
                    System.getProperty("java.home") + "/lib/security/cacerts"));
                    FileOutputStream fos = new FileOutputStream(jksFile)) {
                jks.load(fis, null);
                jks.store(fos, new char[0]);
            }
            endpoint.setCertChainFile(jksFile);
            LatchedLinkedList<Event> events = new LatchedLinkedList<Event>(1);
            MockNetworkConnectPromise promise = new MockNetworkConnectPromise(events);
            nn.connect(endpoint, listener, promise);
            events.await(EVENT_WAIT_TIMEOUT_SECONDS);
            assertTrue("Expected promise to be marked completed", promise.isComplete());
            assertEquals("Expected next event to be a connect success", Event.Type.CONNECT_SUCCESS, events.getLast().type);
            assertNull("Expected event not to throw any Exception", (events.getLast().context));
            testListener.stop();
            assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        }

        // PEM file
        {
            BaseListener testListener = new BaseSslListener(34567);
            final String pem = "-----BEGIN CERTIFICATE-----\n" +
                    "MIIDdTCCAl2gAwIBAgIJANFFGK9I4T11MA0GCSqGSIb3DQEBCwUAMFExCzAJBgNV\n" +
                    "BAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX\n" +
                    "aWRnaXRzIFB0eSBMdGQxCjAIBgNVBAMMASowHhcNMTUwMjEyMTYwMDI3WhcNMTYw\n" +
                    "MjEyMTYwMDI3WjBRMQswCQYDVQQGEwJBVTETMBEGA1UECAwKU29tZS1TdGF0ZTEh\n" +
                    "MB8GA1UECgwYSW50ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMQowCAYDVQQDDAEqMIIB\n" +
                    "IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAv7Zxx52YpRNNYS2zrndAHqp3\n" +
                    "7SPfKO6V5b39fD9BY4sxE3EXbfqwhtBz8qdwwzZxQiJggasOoZvO6fVWqMLGGwD9\n" +
                    "E3spPHCnKGst5jYIXflhQGrmGcaLo3f4bl/PGFKjbxq5g9EAyPyR8UD6KKkJG5k2\n" +
                    "MbHtHc50IZ5Yqw1NdtArv9P+4BVmuizqHF624mbtvXm30Pvy0d7PHoQePVEyQdhT\n" +
                    "bFgONsn06YAGmbmOHroA9ZXd5mlUZR8WbP8CXy0H8AlHtyznZ17BRNvhq5LbuIQ/\n" +
                    "BCtIg2UYtJIO2bwjhW5C2d47LazUnYJn9k7h8EFBGAFUIENwagjyZRtGaP3pGwID\n" +
                    "AQABo1AwTjAdBgNVHQ4EFgQU66c1HFI3SoZgCEDSvFI3QhE44Y0wHwYDVR0jBBgw\n" +
                    "FoAU66c1HFI3SoZgCEDSvFI3QhE44Y0wDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0B\n" +
                    "AQsFAAOCAQEAoeRrYxU6fXzzhsNb8yK/DC7uM4zpEV39K9crfcs7KpshmaDqH9Zg\n" +
                    "pimds9b9Dz2yAMXcJM+2SSwjHZy4YDYAGkBkH/B6omm3CTPOpWF07zqruWyNgciA\n" +
                    "fA2Vpfgi5X8Ge6nI8JDwZ251OKjSKI5SdKK7DwOVC9XkQc/D1iqBR9yG7XkmheGL\n" +
                    "7GWcATLxSGvjTI0yNthlDBQcrWNVcyaATQBxqBGWwLE3cSpPmqHW8BZ1bFtMUhzh\n" +
                    "gYudp409BWrd/hFuj/6f/EJ78yC5BXZliwk6Rf6k8O9kwjWgxVvxZnB63pQ8YxeC\n" +
                    "iWuC764ip5/Snbsll8cbcMa48QvCdEv9AA==\n" +
                    "-----END CERTIFICATE-----\n";
            final File pemFile = folder.newFile();
            try (FileWriter writer = new FileWriter(pemFile)) {
                writer.write(pem);
                writer.flush();
                writer.close();
            }
            endpoint.setCertChainFile(pemFile);
            LatchedLinkedList<Event> events = new LatchedLinkedList<Event>(1);
            MockNetworkConnectPromise promise = new MockNetworkConnectPromise(events);
            nn.connect(endpoint, listener, promise);
            events.await(EVENT_WAIT_TIMEOUT_SECONDS);
            assertTrue("Expected promise to be marked completed", promise.isComplete());
            assertNull("Expected event not to throw any Exception", (events.getLast().context));
            assertEquals("Expected next event to be a connect success", Event.Type.CONNECT_SUCCESS, events.getLast().type);
            testListener.stop();
            assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        }

        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
    }

    @Test
    public void writeData() throws Exception {
        NettyNetworkService nn = new NettyNetworkService();
        ReceiveListener testListener = new ReceiveListener(34567);

        LinkedList<Event> events = new LinkedList<>();
        MockNetworkListener listener = new MockNetworkListener(events);
        MockNetworkConnectPromise promise = new MockNetworkConnectPromise(events);
        nn.connect(new StubEndpoint("localhost", 34567), listener, promise);

        for (int i = 0 ; i < 20; ++i) {
            if (promise.isComplete()) break;
            Thread.sleep(50);
        }
        assertTrue("Expected connect promise to be marked completed", promise.isComplete());
        assertNotNull("Expected connect promise to contain a channel, events are: "+promise.getEvents(), promise.getChannel());

        byte[] data = new byte[(1 << 24)];
        Arrays.fill(data, (byte)123);
        int expectedBytes = 0;
        @SuppressWarnings("unchecked")
        Promise<Boolean>[] promises = new Promise[25];
        for (int i = 0; i < 25; ++i) {
            ByteBuffer buffer = ByteBuffer.wrap(data, 0, 1 << i);
            promises[i] = new MockNetworkWritePromise();
            promise.getChannel().write(buffer, promises[i]);
            expectedBytes += (1 << i);
        }

        boolean allWritesComplete = false;
        for (int j = 0; j < 100; ++j) {
            allWritesComplete = true;
            for (int i = 0; i < promises.length; ++i) {
                if (!promises[i].isComplete()) {
                    allWritesComplete = false;
                    break;
                }
            }
            if (allWritesComplete) {
                break;
            }
            Thread.sleep(50);
        }

        if (!allWritesComplete) {
            StringBuilder sb = new StringBuilder("Expected all write promises to have been completed:\n");
            for (int i = 0; i < promises.length; ++i) {
                sb.append("Promise #" + i +" is " + (promises[i].isComplete() ? "completed" : "not completed!\n"));
            }
            throw new AssertionFailedError(sb.toString());
        }

        MockNetworkClosePromise closePromise = new MockNetworkClosePromise();
        promise.getChannel().close(closePromise);
        for (int i = 0 ; i < 20; ++i) {
            if (closePromise.isComplete()) break;
            Thread.sleep(50);
        }
        assertTrue("Expected close promise to be marked done", closePromise.isComplete());
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));

        assertEquals("Expected to have received same amount of data as was sent", expectedBytes, testListener.getBytesRead());

        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
    }

    @Test
    public void readData() throws IOException, InterruptedException {
        NettyNetworkService nn = new NettyNetworkService();
        SendListener testListener = new SendListener(34567);

        LatchedLinkedList<Event> channelEvents = new LatchedLinkedList<Event>(2);
        LatchedLinkedList<Event> connectEvents = new LatchedLinkedList<Event>(1);
        MockNetworkListener listener = new MockNetworkListener(channelEvents);
        MockNetworkConnectPromise connectPromise = new MockNetworkConnectPromise(connectEvents);
        nn.connect(new StubEndpoint("localhost", 34567), listener, connectPromise);

        connectEvents.await(EVENT_WAIT_TIMEOUT_SECONDS);

        assertTrue("Expected connect promise to be marked done", connectPromise.isComplete());
        assertNotNull("Expected connect promise to contain a channel", connectPromise.getChannel());
        assertEquals("Wrong number of connect events seen: " + connectEvents.toString(), 1, connectEvents.size());

        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));

        channelEvents.await(EVENT_WAIT_TIMEOUT_SECONDS);
        for (int i = 0; i < 20; ++i) {
            synchronized(channelEvents) {
                if (channelEvents.getLast().type == Event.Type.CHANNEL_CLOSE) {
                    break;
                }
            }
            Thread.sleep(50);
        }

        assertTrue("Expected at least three channel events", channelEvents.size() > 2);
        assertEquals("Expected first event to be a connect success", Event.Type.CONNECT_SUCCESS, connectEvents.removeFirst().type);
        assertEquals("Expected last event to be a channel close", Event.Type.CHANNEL_CLOSE, channelEvents.removeLast().type);

        int amount = 0;
        while(!channelEvents.isEmpty()) {
            Event event = channelEvents.removeFirst();
            assertEquals("Expected channel read event", Event.Type.CHANNEL_READ, event.type);
            amount += ((ByteBuf)event.context).readableBytes();
        }
        assertEquals("Didn't receive the same amount of data as was written", testListener.getBytesWritten(), amount);

        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
    }


    @Test
    public void connectLocalClose1() throws Exception {
        NettyNetworkService nn = new NettyNetworkService();
        ReceiveListener testListener = new ReceiveListener(34567);

        LinkedList<Event> events = new LinkedList<>();
        MockNetworkListener listener = new MockNetworkListener(events);
        MockNetworkConnectPromise promise = new MockNetworkConnectPromise(events);
        nn.connect(new StubEndpoint("localhost", 34567), listener, promise);

        for (int i = 0 ; i < 20; ++i) {
            if (promise.isComplete()) break;
            Thread.sleep(50);
        }
        assertTrue("Expected connect promise to be marked completed", promise.isComplete());
        assertNotNull("Expected connect promise to contain a channel", promise.getChannel());

        MockNetworkClosePromise closePromise1 = new MockNetworkClosePromise();
        MockNetworkClosePromise closePromise2 = new MockNetworkClosePromise();
        promise.getChannel().close(closePromise1);
        for (int i = 0 ; i < 20; ++i) {
            if (closePromise1.isComplete()) break;
            Thread.sleep(50);
        }
        promise.getChannel().close(closePromise2);
        assertTrue("Expected close promise1 to be marked done", closePromise1.isComplete());
        assertTrue("Expected close promise1 to be marked done", closePromise2.isComplete());
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));

        assertEquals("Expected to have received same amount of data as was sent", 0, testListener.getBytesRead());

        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
    }

    @Test
    public void connectLocalClose2() throws Exception {
        NettyNetworkService nn = new NettyNetworkService();
        ReceiveListener testListener = new ReceiveListener(34567);

        LinkedList<Event> events = new LinkedList<>();
        MockNetworkListener listener = new MockNetworkListener(events);
        MockNetworkConnectPromise promise = new MockNetworkConnectPromise(events);
        nn.connect(new StubEndpoint("localhost", 34567), listener, promise);

        for (int i = 0 ; i < 20; ++i) {
            if (promise.isComplete()) break;
            Thread.sleep(50);
        }
        assertTrue("Expected connect promise to be marked completed", promise.isComplete());
        assertNotNull("Expected connect promise to contain a channel", promise.getChannel());

        promise.getChannel().close(null);
        promise.getChannel().close(null);

        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        assertEquals("Expected to have received same amount of data as was sent", 0, testListener.getBytesRead());

        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
    }
}
