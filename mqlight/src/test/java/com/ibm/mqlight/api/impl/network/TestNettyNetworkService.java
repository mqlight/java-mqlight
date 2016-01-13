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
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLServerSocketFactory;

import junit.framework.AssertionFailedError;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ibm.mqlight.api.ClientOptions.SSLOptions;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.security.PemFile;

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
        private File keyStoreFile = null;
        private String keyStorePassphrase = null;
        private File certChainFile = null;
        private File clientCertFile = null;
        private File clientKeyFile = null;
        private String clientKeyePassphrase = null;
        private boolean verifyName = false;
        private StubEndpoint(String host, int port) {
            this.host = host;
            this.port = port;
        }
        @Override public String getHost() { return host; }
        @Override public int getPort() { return port; }
        @Override public boolean useSsl() { return useSsl; }
        public void setUseSsl(final boolean useSsl) { this.useSsl = useSsl; }
        public void setCertChainFile(final File certChainFile) { this.certChainFile = certChainFile; }
        public void setVerifyName(final boolean verifyName) { this.verifyName = verifyName; }
        @Override public String getUser() { return null; }
        @Override public String getPassword() { return null; }
        @Override public int getIdleTimeout() { return 0; }
        @Override public URI getURI() { URI uri = null;
                                        try { uri = new URI((useSsl() ? "amqps://" : "amqp://") + host + ":" + port);
                                            } catch (URISyntaxException e) {}
                                        return uri; }
        @Override
        public SSLOptions getSSLOptions() {
            return new SSLOptions(keyStoreFile, keyStorePassphrase, certChainFile, verifyName, clientCertFile, clientKeyFile, clientKeyePassphrase);
        }
        public void setKeyStore(File keyStoreFile, String passphrase) {
            this.keyStoreFile = keyStoreFile;
            this.keyStorePassphrase = passphrase;
            
        }
        public void setClientCert(File clientCertfile) {
            this.clientCertFile = clientCertfile;
        }
        public void setClientKey(File clientKeyFile, String passphrase) {
            this.clientKeyFile = clientKeyFile;
            this.clientKeyePassphrase = passphrase;
        }
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

    private final String cacertsPEM = "-----BEGIN CERTIFICATE-----\n" +
            "MIIF1zCCA7+gAwIBAgIJAPeXpRj1jgibMA0GCSqGSIb3DQEBCwUAMIGBMQswCQYD\n" +
            "VQQGEwJVUzERMA8GA1UECAwITmV3IFlvcmsxDzANBgNVBAcMBkFybW9uazE0MDIG\n" +
            "A1UECgwrSW50ZXJuYXRpb25hbCBCdXNpbmVzcyBNYWNoaW5lcyBDb3Jwb3JhdGlv\n" +
            "bjEYMBYGA1UEAwwPTVEgVGVzdCBSb290IENBMB4XDTE2MDEwNzEwMjAyMloXDTIx\n" +
            "MDEwNjEwMjAyMlowgYExCzAJBgNVBAYTAlVTMREwDwYDVQQIDAhOZXcgWW9yazEP\n" +
            "MA0GA1UEBwwGQXJtb25rMTQwMgYDVQQKDCtJbnRlcm5hdGlvbmFsIEJ1c2luZXNz\n" +
            "IE1hY2hpbmVzIENvcnBvcmF0aW9uMRgwFgYDVQQDDA9NUSBUZXN0IFJvb3QgQ0Ew\n" +
            "ggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCvzeWyncNlAcdqOqkPCNeR\n" +
            "o81XswKpGGGCTszvemzGHdRjWeXIfWT/gvjFHJUnnt7cgnQpcDnxdBWoUDU3Hwm2\n" +
            "BHuVNLyYJrq9muBI+X7RhPshgkYeEAAZRshz7T5F72vnsnJiFrXWFiOfZ3Sp9Cyb\n" +
            "Ep/5A7IuVCb0/YwLkDy+kN8qfj1oX2oRazQ/CId9mmb2nT28ObBz6h5sRGXm0D6l\n" +
            "drfUSDVrFvltG6BBkjR/Tz0g+mtJhVZYqd24Yzl4TmL883dvFBWec3Gv3peUtm8g\n" +
            "F5Dgh7r0t1mqllkErf3bpedKPui4TxGOgT1oJYjxXBpXKUAppH0tUxZCjY/E99rO\n" +
            "q21Nl9TuBnaRfHblbKOcwMolJuYuqrKWRY6bqCiDoQ4vGP/k96FtsSPZ6mCpFnXK\n" +
            "+r+YCtIQef7O9uScOff9m+tUFaducvEHY2RLdN7gMvr7KWfPdSm6Fyw3R3D7qaOJ\n" +
            "Ism25QjdhyAt5v8o1B/mHfyJdYk+vg0FIMblTTRM1F+9pbwV7h8U75EdBelPrOeG\n" +
            "d7fsMxXURwWm4xVvdp6pStkA6v2fGwV0o+ZBoiiOWk6CGy3JX7bto6iaAiQ0tyui\n" +
            "+lUlVZjS475fKPsm1xnAMNLR++y2fq0AiwjYkfItbB/C4uLlFzaye/ZIM1sxySp5\n" +
            "N6CBDAw7PpIOASNjHo/GawIDAQABo1AwTjAdBgNVHQ4EFgQU0j80+QdMRhZNkiZo\n" +
            "24t5M5PxJVEwHwYDVR0jBBgwFoAU0j80+QdMRhZNkiZo24t5M5PxJVEwDAYDVR0T\n" +
            "BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAgEAX/x5G40GWA41CHXA6Zo4UWj+Ap2u\n" +
            "B3E2ZVktk0KKvX9rrXkdcUbqf28Bj23B3iNGZp0Tris8WoBYQ9lhN9ZoD8jvPkr2\n" +
            "q5LAAQ9EnlBQPkpMS8PB+9trHcGvZnN6L13Ftmh1325LPodLLmXzc0mzT6f9kdhI\n" +
            "NgN5KeQ7b9T6AnEosNAR6RBbJJ87RbWNFvb5Xk41Lth0/tfjGOlIJxUZ7bqfCF0p\n" +
            "wsIwSuK2EAKvdWfJd0uln1JBXE8STcr8ft+ts3Dpft2Y+ZR9VEPqrWka25a5+g43\n" +
            "OAP0ez3sMLo93BEouA24c6PumEriIeHaggKEZWCGUsU7RSOpU0/UAXf7RzW0c3yK\n" +
            "//7TzxX6gPcpENquCG2zsna07SOWKigrLGDp5wkP20VhvbM/22JZpkCwlixqew1E\n" +
            "B0QQmXqkGFzLrtIU4YjnqQ+FFIiV5zCzcayEHzYEx9iERKqvo+hyj9U1oACCUZ7Z\n" +
            "AAu/Uy7KVSzjIVrl6dBmPRN4UqoLmQU5Xx9XR4b5MKc8tb9J5e3R586ajePKcAKD\n" +
            "83QNoArQcL6xK6wsI9tLaiPQEyLGBgpe1+SR76oJ3aYFsPKfT/gDFDF3Uz4KNtay\n" +
            "F4QHlw0tTSgpUNKgo7RwNazC+RmQd9UT12CidnueSbVmvnYVRv/BdwlK/5uh8K3Y\n" +
            "HwbpyR7idzmVNfA=\n" +
            "-----END CERTIFICATE-----";

    private final String clientCertPEM = "-----BEGIN CERTIFICATE-----\n" +
            "MIIESzCCAjMCAQEwDQYJKoZIhvcNAQELBQAwgYExCzAJBgNVBAYTAlVTMREwDwYD\n" +
            "VQQIDAhOZXcgWW9yazEPMA0GA1UEBwwGQXJtb25rMTQwMgYDVQQKDCtJbnRlcm5h\n" +
            "dGlvbmFsIEJ1c2luZXNzIE1hY2hpbmVzIENvcnBvcmF0aW9uMRgwFgYDVQQDDA9N\n" +
            "USBUZXN0IFJvb3QgQ0EwHhcNMTYwMTA3MTAyMTAxWhcNMTgwMTA2MTAyMTAxWjBV\n" +
            "MQswCQYDVQQGEwJHQjESMBAGA1UECAwJSGFtcHNoaXJlMRAwDgYDVQQHDAdIdXJz\n" +
            "bGV5MQwwCgYDVQQKDANJQk0xEjAQBgNVBAMMCWxvY2FsaG9zdDCCASIwDQYJKoZI\n" +
            "hvcNAQEBBQADggEPADCCAQoCggEBAM7ge71fSL6CoHceEi3gNgU2NcOV4pVjp49b\n" +
            "iNL6bYO7XogHzN9P8N7x/ktEYO5lcIaBivWIrrAGe72EyIFQy3g14PSiWVhtDKhx\n" +
            "gEUFQr5y+caot1BM2WXpCOG90qsbIEDCPu5j1ySoWRE/JNH5QHJnxBfmzQgN3xWx\n" +
            "5/U73rmj8h8t02ZvSLPWffRlKipzMNzlN58x9Db7LmgQJElyZqGUu1pdMdpEhy2T\n" +
            "EOlqnzv7+dY6afk72AAPFbRkKGjudjuZYTvMjYkyNVN8gYlzDFKQNl4QrvNh34ak\n" +
            "tv1Ikps9cs/kH2wjV7wJelxxO9RT5RCilyVIuK8f0CfYoFW1cWECAwEAATANBgkq\n" +
            "hkiG9w0BAQsFAAOCAgEApK8w7aiPweKgKHSWactLu4BKIBU/O5xlCdOtVGktcJ85\n" +
            "lyBEM0xriTCpGPRLQBMFSim8yfyBQsoSRqoQkod67QHecpB/Ymxgk9tHZz4o2wTH\n" +
            "TEyROEG/ZyTuNrBKtLHwv0N9HVUBB45HfQSln4R3KVjRi1vXoERBX4cF7NhytM2u\n" +
            "G7BOG9Tr8ga0u/PqF38PYeq4UMleU+DmZ4fPFxCHikAuj/WoUz3og+XFzVHgvoHM\n" +
            "hMfm/rKNF2JSiC9kjZExwiaWeQe/CQXA+WkWoLO+roNK6BCJmj3y1ylG6pNy8DR0\n" +
            "+5uUzwFcnMJkT3N2U/8YNO5M6Sf1ULa9FIYYcgyWAweNI/XZlNLf5PaSbcrjCzIW\n" +
            "YQ1Y0nQ8n9PF3it/YI8IfVtrhm4RH3vYgeSzJy2iK7E3aC/D4CCkARvqAJg4Lz3F\n" +
            "AmR4H6ai9GJlNT5nn+mQT7BM/3meSVZylmZvrZeFD4dSbTn8G45Uhy/RmzKRxLBh\n" +
            "ow/Yj6O6ctp6zn4yk+adTuvXGH6iLYhJl4DYVi8+HZEGeT7E3QxYv7kjJqDPiQxS\n" +
            "lOnZq2rzTUM9wCxTKVpHq1XylKcs39SAvFLXRy5GSX+WF4DqccbpSbvnW2xTId/a\n" +
            "lKu8yjDY+/iH06ISme9GcmfXdZcXQbQaRzSYeWq6+z1q/KO1Nbdol0wRkdIilRM=\n" +
            "-----END CERTIFICATE-----";
    
    private final String clientKeyPEM = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDO4Hu9X0i+gqB3\n" +
            "HhIt4DYFNjXDleKVY6ePW4jS+m2Du16IB8zfT/De8f5LRGDuZXCGgYr1iK6wBnu9\n" +
            "hMiBUMt4NeD0ollYbQyocYBFBUK+cvnGqLdQTNll6QjhvdKrGyBAwj7uY9ckqFkR\n" +
            "PyTR+UByZ8QX5s0IDd8Vsef1O965o/IfLdNmb0iz1n30ZSoqczDc5TefMfQ2+y5o\n" +
            "ECRJcmahlLtaXTHaRIctkxDpap87+/nWOmn5O9gADxW0ZCho7nY7mWE7zI2JMjVT\n" +
            "fIGJcwxSkDZeEK7zYd+GpLb9SJKbPXLP5B9sI1e8CXpccTvUU+UQopclSLivH9An\n" +
            "2KBVtXFhAgMBAAECggEAHceeyGHg/Nuc8ci4YTY3UIS/NIps9YhE5JQOnCBEh4P/\n" +
            "VaGZ0kOVruIy8u7C7U5Y5mPbdwDX9KraQSvOGR9iSsmmulBsgBgijFTaXBUnyHaQ\n" +
            "khJTYRHO8aacCrLikpLzD59mo7Znj/VT7PGkAT3aEx3w0mf8973XcS4M7/ZQV3SJ\n" +
            "nMUQLkxCN1XK4ns9yNxN6H8mYp4gn9VuB1P9igF0UomuSKYGrXTw6VAesDlKqdQ1\n" +
            "s08yCRt9hEQSO1/dRmYvq+pFq6RrN8KjXIOiGrUKg1O1JEsVGVT6aDs9R6i+oLoG\n" +
            "BgfdOOk5GlkxmqgO0eZngtDsPMD3QEwgBkFMiynw4QKBgQD+eua0D51Pb7wSjJfq\n" +
            "T4fUanFVu8Hd9tJw36dOJgAyzV2tzbSRCQPZxRtPXCi/K7vRwN12FNcnf6cMlgkQ\n" +
            "aVx5JpZBvb3GZAc7P0mzX4hPJbBpBb7yRsPvmvggO5h3a4PPRFqrpf1E6Z/iaJ16\n" +
            "mGSk0VcdXqm96dUNMvF3t65pXQKBgQDQHMwP/2BecI0yyuYLXx9+1WpqR5wRUB0T\n" +
            "XshUzxSu6uaLqwrg30PekkiKIYdDyNwJtE0DuzbCM2fp34Gp0ceNjQI2LirQJkHZ\n" +
            "YEKwr1cWK8hl9QzUDd6wxQrRZmbwptWDoby0f8znlDV+cuL3Eqeg0WH4SstfrpGv\n" +
            "oCYXnh9z1QKBgQD0utj3PTT52eiypgbKzWVBQIRyALj2b5H9/vh0zVLPiHSY2wTV\n" +
            "nifX3Bjhfy2oe7SKicHw9yXa1IagMgHRiKHn2NYTrxe8nSHfNoP4Pt1l0EcRGPeD\n" +
            "I872tL/+r5F29yyxvXi9LkqdZVffcuBPsBLJ9pCirBDtlNzRbraNfVX7+QKBgHbH\n" +
            "iNy0mC8utBhCX+wrnZFJg5QnTPdAr2en2FU3YAm5vl4HAI16QIVfHpHgMxDIKnZL\n" +
            "dw1jJAzRRETisWHYfrnWumVsEjl9LGZCH64yVVUtJhKzO2Aojmp7/AGqHaTKw+B+\n" +
            "RnMK4ktmduW18r6r4grSlsUdA1iYDUSc9kDSmgcFAoGBAJHtnSr4IJ0GDNShlocx\n" +
            "s03WPhYCzjd98srDyNvALvrypccY3Hzkh+YvXnztWpa3F9A5DC+LQWwBtdgzwqM1\n" +
            "kFifJQXthyFjNjn0YmgcmS0epUCPP6nfs0t9uyQ5DngbeS/V6sN+PrGQFpWZJ4NO\n" +
            "FclmlCoKNJfExI6CJwluNjEy\n" +
            "-----END PRIVATE KEY-----";
    
    private final String encryptedClientKeyPEM = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n" +
            "MIIFDjBABgkqhkiG9w0BBQ0wMzAbBgkqhkiG9w0BBQwwDgQIqIH+1ODU2HACAggA\n" +
            "MBQGCCqGSIb3DQMHBAgoYExX3n/dYwSCBMjEb2XiCtEQshGWLG6H2pqHTI1a3tY6\n" +
            "Eis+SpTnghEZPrO3hjCqqvq26tnpxV1wTSueprFHKqBZLKr3vcn/OFK0fkamK1Z+\n" +
            "BKis4KIL2Ibc7nh1N915n6IOQugqda8Uvt5MTNNzprGU+h/vdDh4i3czBR8UK5iH\n" +
            "H0WU7d9Eu9iNxVA9yzmhpDM+b5NkDAF/P6MaWowU1wSbpykqexxhVdIHH/ub+d4P\n" +
            "fk9CBiacyAuCDS7bumei2g+MI5y2uanymKJukUOr0ixBzwh3KESOU9bIIeyVu/EA\n" +
            "3rTheCoHp64i4GaSjFftlCk2aYUltWMTJysb0VoXSlfaP+pUkFBLRjDanTQaOzFf\n" +
            "XPh9d/hHyzwoI2GTpX01pY5OTDzzmmLLUHKo7a49axVL31I0R9wfGXAS5kBplPwM\n" +
            "Mpy/TzVQrWTfJpVLNnaviX17LKmp3/xdpzIcHbP4BAPreGBA5hg3AmGUQWFbhH4R\n" +
            "4O6ugGON0bSiEhQPwpZHTMnKPysE1+2X/b6YmwyL5F6sf73isfKeJhRvYfSXKChB\n" +
            "RDemliX5GuulI0yyIEGS4I2ovyhEDEUF7qg6GgmAQrb7ggq+P1TA3zup1Wc5F5pf\n" +
            "4RODWXW5znx34VNP7grsfLv24z70BaBuBoAvJtpZ8Ql6CexN7KlM8IdnEIgIoP8T\n" +
            "4a6cyYW/dedUOxob8f4dGZVeTLOqU+MPiLOebVDbBUF1BVQbJhcPANUmrWrxwGff\n" +
            "DS1K26ZaujwPxMaOkM3rGpESDHn7Qpr0EDPMvsqWy8G6lZgn9OhtypLAu3EtvLRY\n" +
            "RznisyzWzaATYpyDrwfpZwLrK6URkGBRJH4tudl5D9SjQhJE4KkHYelWRWpP9Rxk\n" +
            "mXvu0CXDAZr65lreExRVXYrI8WQWwXc/wkd6Y0xkRcUVrL3LLs50dT97PkF1ghgO\n" +
            "+JKVhdXAiKjKiUS30VaP+CPv4bBZwZkp1/Ue20qu94zxErGUGVoxBbvLoQ7NHezR\n" +
            "EnqR2MJDqTYOMM0vKAZ8OAteGx05byR7Tw+lm2RjGtrkEpEPBoqJ3x1f3DSTXXtu\n" +
            "uyMMgp7DbF4VnbkMWBWXoNmt7lDND7eHfz8Dqa14ZmsgSYbMEzNFs8Rr0fH3iroS\n" +
            "FWP0Vk5D9145qzBC9uZmjoFhwpJdPjUQeNFl7FfvMQ0cpjz5Lxo6VMpcB6qYwzSZ\n" +
            "eobJ+sSMdHU0wES2SQAeolpAGHwIE09A+ovOCkGirtjUHl/1nttjRuZ1lNbj08e4\n" +
            "xMUTUgNmJgtQ92N2TveImc/LONqQboUB5jQCyHhNzVVQYjBUl1wu0W8LHHrEDFUg\n" +
            "Lro/lP1ZNJ5NRi5IHxOujRL4ewusNXDsO/ZieLsIdWZAVJdAhSCKLPWHRIk6kClP\n" +
            "cqniTiNO/f/Sg8c3xPbFxMHQDM901haC5GncJyQG9TIk9ocTxlacxPX24ZY0rQTq\n" +
            "jhZKMAe9x6HtiEx8B5I9eI4mbcd3G28Kv0/ISkRt9gxsUaz046awbzZ9OUQounqZ\n" +
            "id2d4m6lrQ3RwzBnAcTx3lQZxpe7N1T2xZpwCdX8YfjeMZRn8TCLcfPT/IxCadT5\n" +
            "A1Pyo75PCfUoxrlxmpoeJdB1U8oCbsed7jtOQ4PJvPgqg4WRgDJ/FZy2OK2EwDu6\n" +
            "hFs=\n" +
            "-----END ENCRYPTED PRIVATE KEY-----";
    private final String encryptedClientKeyPEMPassphrase = "123456";
    
    private void createKeyStore(File keyStoreFile, String password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyException, InvalidKeySpecException {
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
                
        final File certsFile = folder.newFile();
        try (FileWriter writer = new FileWriter(certsFile)) {
            writer.write(cacertsPEM);
            writer.flush();
            writer.close();
        }
        final File clientKeyfile = folder.newFile();
        try (FileWriter writer = new FileWriter(clientKeyfile)) {
            writer.write(clientKeyPEM);
            writer.flush();
            writer.close();
        }
        final File clientCertfile = folder.newFile();
        try (FileWriter writer = new FileWriter(clientCertfile)) {
            writer.write(clientCertPEM);
            writer.flush();
            writer.close();
        }
        final PemFile certChainPemFile = new PemFile(clientCertfile);
        final List<Certificate> certChain = (List<Certificate>) certChainPemFile.getCertificates();

        final PemFile clientKeyPemfile = new PemFile(clientKeyfile);
        final byte[] privateKeyBytes = clientKeyPemfile.getPrivateKeyBytes();
        final KeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        final PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        keyStore.setKeyEntry("key", privateKey, password.toCharArray(), certChain.toArray(new Certificate[certChain.size()]));

        final PemFile certsPemFile = new PemFile(certsFile);
        List<Certificate> certs = certsPemFile.getCertificates();
        int index = 0;
        for (Certificate cert : certs) keyStore.setCertificateEntry("cert"+(++index), cert);
        
        try (FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
            keyStore.store(fos, password.toCharArray());
            fos.close();
        }
    }
    
    @Test
    public void sslKeyStore() throws Exception {
        NettyNetworkService nn = new NettyNetworkService();
        MockNetworkListener listener = new MockNetworkListener(new LinkedList<Event>());
        final StubEndpoint endpoint = new StubEndpoint("localhost", 34567);
        endpoint.setUseSsl(true);
        endpoint.setVerifyName(true);

        BaseListener testListener = new BaseSslListener(34567);
        final File jksFile = folder.newFile();
        createKeyStore(jksFile, "password");
        endpoint.setKeyStore(jksFile, "password");
        LatchedLinkedList<Event> events = new LatchedLinkedList<Event>(1);
        MockNetworkConnectPromise promise = new MockNetworkConnectPromise(events);
        nn.connect(endpoint, listener, promise);
        events.await(EVENT_WAIT_TIMEOUT_SECONDS);
        assertTrue("Expected promise to be marked completed", promise.isComplete());
        assertEquals("Expected next event to be a connect success", Event.Type.CONNECT_SUCCESS, events.getLast().type);
        assertNull("Expected event not to throw any Exception", (events.getLast().context));
        testListener.stop();
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));

        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
    }

    @Test
    public void badSslKeyStore() throws Exception {
        NettyNetworkService nn = new NettyNetworkService();
        MockNetworkListener listener = new MockNetworkListener(new LinkedList<Event>());
        final StubEndpoint endpoint = new StubEndpoint("localhost", 34567);
        endpoint.setUseSsl(true);
        endpoint.setVerifyName(true);

        BaseListener testListener = new BaseSslListener(34567);
        final File jksFile = folder.newFile();
        createKeyStore(jksFile, "password");
        
        final File badKeyStore = folder.newFile();
        try (FileWriter writer = new FileWriter(badKeyStore)) {
            writer.write(cacertsPEM);
            writer.flush();
            writer.close();
        }
        // bad key store format
        endpoint.setKeyStore(badKeyStore, "password");
        LatchedLinkedList<Event> events = new LatchedLinkedList<Event>(1);
        MockNetworkConnectPromise promise = new MockNetworkConnectPromise(events);
        nn.connect(endpoint, listener, promise);
        events.await(EVENT_WAIT_TIMEOUT_SECONDS);
        assertTrue("Expected promise to be marked completed", promise.isComplete());
        assertEquals("Expected next event to be a connect failure", Event.Type.CONNECT_FAILURE, events.getLast().type);
        assertEquals("Expected event to throw Exception", SecurityException.class, (events.getLast().context).getClass());
        testListener.stop();
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
        
        // wrong key store password
        endpoint.setKeyStore(jksFile, "wrong");
        events = new LatchedLinkedList<Event>(1);
        promise = new MockNetworkConnectPromise(events);
        nn.connect(endpoint, listener, promise);
        events.await(EVENT_WAIT_TIMEOUT_SECONDS);
        assertTrue("Expected promise to be marked completed", promise.isComplete());
        assertEquals("Expected next event to be a connect failure", Event.Type.CONNECT_FAILURE, events.getLast().type);
        assertEquals("Expected event to throw Exception", SecurityException.class, (events.getLast().context).getClass());
        testListener.stop();
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
        
    }

    @Test
    public void sslCLientAuth() throws Exception {
        final StubEndpoint endpoint = new StubEndpoint("localhost", 34567);
        endpoint.setUseSsl(true);
        endpoint.setVerifyName(true);

        final File certsFile = folder.newFile();
        try (FileWriter writer = new FileWriter(certsFile)) {
            writer.write(cacertsPEM);
            writer.flush();
            writer.close();
        }
        final File clientKeyfile = folder.newFile();
        try (FileWriter writer = new FileWriter(clientKeyfile)) {
            writer.write(clientKeyPEM);
            writer.flush();
            writer.close();
        }
        final File encryptedClientKeyfile = folder.newFile();
        try (FileWriter writer = new FileWriter(encryptedClientKeyfile)) {
            writer.write(encryptedClientKeyPEM);
            writer.flush();
            writer.close();
        }
        final File clientCertfile = folder.newFile();
        try (FileWriter writer = new FileWriter(clientCertfile)) {
            writer.write(clientCertPEM);
            writer.flush();
            writer.close();
        }

        // using a client key with password
        NettyNetworkService nn = new NettyNetworkService();
        MockNetworkListener listener = new MockNetworkListener(new LinkedList<Event>());
        BaseListener testListener = new BaseSslListener(34567);
        
        endpoint.setClientCert(clientCertfile);
        endpoint.setClientKey(clientKeyfile, "1234");
        endpoint.setCertChainFile(certsFile);
        LatchedLinkedList<Event> events = new LatchedLinkedList<Event>(1);
        MockNetworkConnectPromise promise = new MockNetworkConnectPromise(events);
        nn.connect(endpoint, listener, promise);
        events.await(EVENT_WAIT_TIMEOUT_SECONDS);
        assertTrue("Expected promise to be marked completed", promise.isComplete());
        assertNull("Expected event not to throw any Exception", (events.getLast().context));
        assertEquals("Expected next event to be a connect success", Event.Type.CONNECT_SUCCESS, events.getLast().type);
        testListener.stop();
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
        
        // using a client key with no password
        nn = new NettyNetworkService();
        listener = new MockNetworkListener(new LinkedList<Event>());
        testListener = new BaseSslListener(34567);
        
        endpoint.setClientCert(clientCertfile);
        endpoint.setClientKey(clientKeyfile, null);
        endpoint.setCertChainFile(certsFile);
        events = new LatchedLinkedList<Event>(1);
        promise = new MockNetworkConnectPromise(events);
        nn.connect(endpoint, listener, promise);
        events.await(EVENT_WAIT_TIMEOUT_SECONDS);
        assertTrue("Expected promise to be marked completed", promise.isComplete());
        assertNull("Expected event not to throw any Exception", (events.getLast().context));
        assertEquals("Expected next event to be a connect success", Event.Type.CONNECT_SUCCESS, events.getLast().type);
        testListener.stop();
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
        
        // using an encrypted client key
        nn = new NettyNetworkService();
        listener = new MockNetworkListener(new LinkedList<Event>());
        testListener = new BaseSslListener(34567);
        
        endpoint.setClientCert(clientCertfile);
        endpoint.setClientKey(encryptedClientKeyfile, encryptedClientKeyPEMPassphrase);
        endpoint.setCertChainFile(certsFile);
        events = new LatchedLinkedList<Event>(1);
        promise = new MockNetworkConnectPromise(events);
        nn.connect(endpoint, listener, promise);
        events.await(EVENT_WAIT_TIMEOUT_SECONDS);
        assertTrue("Expected promise to be marked completed", promise.isComplete());
        assertNull("Expected event not to throw any Exception", (events.getLast().context));
        assertEquals("Expected next event to be a connect success", Event.Type.CONNECT_SUCCESS, events.getLast().type);
        testListener.stop();
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
    }
    
    @Test
    public void badSslCLientAuth() throws Exception {
        final NettyNetworkService nn = new NettyNetworkService();
        final MockNetworkListener listener = new MockNetworkListener(new LinkedList<Event>());
        final StubEndpoint endpoint = new StubEndpoint("localhost", 34567);
        endpoint.setUseSsl(true);
        endpoint.setVerifyName(true);

        final File certsFile = folder.newFile();
        try (FileWriter writer = new FileWriter(certsFile)) {
            writer.write(cacertsPEM);
            writer.flush();
            writer.close();
        }
        final File clientKeyfile = folder.newFile();
        try (FileWriter writer = new FileWriter(clientKeyfile)) {
            writer.write(clientKeyPEM);
            writer.flush();
            writer.close();
        }
        final File encryptedClientKeyfile = folder.newFile();
        try (FileWriter writer = new FileWriter(encryptedClientKeyfile)) {
            writer.write(encryptedClientKeyPEM);
            writer.flush();
            writer.close();
        }
        final File clientCertfile = folder.newFile();
        try (FileWriter writer = new FileWriter(clientCertfile)) {
            writer.write(clientCertPEM);
            writer.flush();
            writer.close();
        }

        BaseListener testListener = new BaseSslListener(34567);

        // Invalid client key file
        endpoint.setClientCert(clientCertfile);
        endpoint.setClientKey(clientCertfile, "abc");
        endpoint.setCertChainFile(certsFile);
        LatchedLinkedList<Event> events = new LatchedLinkedList<Event>(1);
        MockNetworkConnectPromise promise = new MockNetworkConnectPromise(events);
        nn.connect(endpoint, listener, promise);
        events.await(EVENT_WAIT_TIMEOUT_SECONDS);
        assertTrue("Expected promise to be marked completed", promise.isComplete());
        assertEquals("Expected next event to be a connect failure", Event.Type.CONNECT_FAILURE, events.getLast().type);
        assertEquals("Expected event to throw Exception", SecurityException.class, (events.getLast().context).getClass());
        testListener.stop();
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));

        // Invalid client cert file
        endpoint.setClientCert(clientKeyfile);
        endpoint.setClientKey(clientKeyfile, null);
        endpoint.setCertChainFile(certsFile);
        events = new LatchedLinkedList<Event>(1);
        promise = new MockNetworkConnectPromise(events);
        nn.connect(endpoint, listener, promise);
        events.await(EVENT_WAIT_TIMEOUT_SECONDS);
        assertTrue("Expected promise to be marked completed", promise.isComplete());
        assertEquals("Expected next event to be a connect failure", Event.Type.CONNECT_FAILURE, events.getLast().type);
        assertEquals("Expected event to throw Exception", SecurityException.class, (events.getLast().context).getClass());
        testListener.stop();
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
        assertTrue("Expected network service to end!", nn.awaitTermination(NETWORK_WAIT_TIMEOUT_SECONDS));
        
        // wrong key password
        endpoint.setClientCert(clientCertfile);
        endpoint.setClientKey(encryptedClientKeyfile, "wrong");
        endpoint.setCertChainFile(certsFile);
        events = new LatchedLinkedList<Event>(1);
        promise = new MockNetworkConnectPromise(events);
        nn.connect(endpoint, listener, promise);
        events.await(EVENT_WAIT_TIMEOUT_SECONDS);
        assertTrue("Expected promise to be marked completed", promise.isComplete());
        assertEquals("Expected next event to be a connect failure", Event.Type.CONNECT_FAILURE, events.getLast().type);
        ((Exception)events.getLast().context).printStackTrace();
        assertEquals("Expected event to throw Exception", SecurityException.class, (events.getLast().context).getClass());
        testListener.stop();
        assertTrue("Expected listener to end!", testListener.join(LISTENER_WAIT_TIMEOUT_SECONDS));
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
