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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.NetworkException;
import com.ibm.mqlight.api.Promise;
import com.ibm.mqlight.api.endpoint.Endpoint;
import com.ibm.mqlight.api.impl.LogbackLogging;
import com.ibm.mqlight.api.logging.FFDCProbeId;
import com.ibm.mqlight.api.logging.Logger;
import com.ibm.mqlight.api.logging.LoggerFactory;
import com.ibm.mqlight.api.network.NetworkChannel;
import com.ibm.mqlight.api.network.NetworkListener;
import com.ibm.mqlight.api.network.NetworkService;

public class NettyNetworkService implements NetworkService {

    private static final Logger logger = LoggerFactory.getLogger(NettyNetworkService.class);

    static {
        LogbackLogging.setup();
    }

    private static Bootstrap insecureBootstrap;
    private static Bootstrap secureBootstrap;

    static class NettyInboundHandler extends ChannelInboundHandlerAdapter implements NetworkChannel {

        private static final Logger logger = LoggerFactory.getLogger(NettyInboundHandler.class);

        private final SocketChannel channel;
        private NetworkListener listener = null;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        protected NettyInboundHandler(SocketChannel channel) {
            final String methodName = "<init>";
            logger.entry(this, methodName, channel);

            this.channel = channel;

            logger.exit(this, methodName);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            final String methodName = "channelRead";
            logger.entry(this, methodName, ctx, msg);

            if (listener != null) listener.onRead(this, (ByteBuf)msg);

            logger.exit(this, methodName);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            final String methodName = "exceptionCaught";
            logger.entry(this, methodName, cause);
            try {
                ctx.close();
                Exception exception;
                if (cause instanceof Exception) {
                    exception = (Exception)cause;
                } else {
                    logger.ffdc(methodName, FFDCProbeId.PROBE_001, cause, this);
                    exception = new NetworkException("unexpected error", cause);
                }
                // if we have a nested chain of causes, walk it until we have at
                // most a single pair of Exception and cause
                while (exception.getCause() != null &&
                        exception.getCause() instanceof Exception) {
                    if (exception.getCause().getCause() == null) {
                        break;
                    }
                    exception = (Exception) exception.getCause();
                }

                // rewrap security-related exceptions
                final String condition = exception.getClass().getName();
                if (condition.contains("javax.net.ssl.") ||
                        condition.contains("java.security.") ||
                        condition.contains("com.ibm.jsse2.") ||
                        condition.contains("sun.security.")) {
                    exception = new com.ibm.mqlight.api.SecurityException(
                            exception.getMessage(), exception.getCause());
                }

                if (listener != null) {
                    listener.onError(this, exception);
                }
            } catch (Throwable t) {
                logger.throwing(this, methodName, t);
                throw t;
            }

            logger.exit(this, methodName);
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx)
                throws Exception {
            final String methodName = "channelWritabilityChanged";
            logger.entry(this, methodName, ctx);

            doWrite(null);

            logger.exit(this, methodName);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            final String methodName = "channelInactive";
            logger.entry(this, methodName, ctx);

            boolean alreadyClosed = closed.getAndSet(true);
            if (!alreadyClosed) {
                if (listener != null) {
                    listener.onClose(this);
                }
                decrementUseCount();
            }

            logger.exit(this, methodName);
        }

        protected void setListener(NetworkListener listener) {
            final String methodName = "setListener";
            logger.entry(this, methodName, listener);

            this.listener = listener;

            logger.exit(this, methodName);
        }

        @Override
        public void close(final Promise<Void> nwfuture) {
            final String methodName = "close";
            logger.entry(this, methodName, nwfuture);

            boolean alreadyClosed = closed.getAndSet(true);
            if (!alreadyClosed) {
                final ChannelFuture f = channel.disconnect();
                if (nwfuture != null) {
                    f.addListener(new GenericFutureListener<ChannelFuture>() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            nwfuture.setSuccess(null);
                            decrementUseCount();
                        }
                    });
                }
            } else if (nwfuture != null) {
                nwfuture.setSuccess(null);
            }

            logger.exit(this, methodName);
        }

        private static class WriteRequest {
            protected final ByteBuf buffer;
            protected final Promise<Boolean> promise;
            protected WriteRequest(ByteBuf buffer, Promise<Boolean> promise) {
                this.buffer = buffer;
                this.promise = promise;
            }
        }

        @Override
        public void write(ByteBuf buffer, Promise<Boolean> promise) {
            final String methodName = "write";
            logger.entry(this, methodName, buffer, promise);

            doWrite(new WriteRequest(buffer, promise));

            logger.exit(this, methodName);
        }


        LinkedList<WriteRequest> pendingWrites = new LinkedList<>();
        boolean writeInProgress = false;

        private void doWrite(final WriteRequest writeRequest) {
            final String methodName = "doWrite";
            logger.entry(this, methodName, writeRequest);

            WriteRequest toProcess = null;
            synchronized(pendingWrites) {
                if (writeRequest != null) {
                    pendingWrites.addLast(writeRequest);
                }
                if (!writeInProgress && channel.isWritable() && !pendingWrites.isEmpty()) {
                    toProcess = pendingWrites.removeFirst();
                    writeInProgress = true;
                }
            }

            if (toProcess != null) {
                final Promise<Boolean> writeCompletePromise = toProcess.promise;
                logger.data(this, methodName, "writeAndFlush {}", toProcess);
                final ChannelFuture f = channel.pipeline().writeAndFlush(toProcess.buffer);
                f.addListener(new GenericFutureListener<ChannelFuture>() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        boolean havePendingWrites = false;
                        synchronized(pendingWrites) {
                            writeInProgress = false;
                            havePendingWrites = !pendingWrites.isEmpty();
                        }
                        logger.data(this, methodName, "doWrite (complete)");
                        writeCompletePromise.setSuccess(!havePendingWrites);
                        doWrite(null);
                    }
                });
            }

            logger.exit(this, methodName);
        }

        private Object context;

        @Override
        public synchronized void setContext(Object context) {
            this.context = context;
        }

        @Override
        public synchronized Object getContext() {
            return context;
        }
    }

    protected class ConnectListener implements GenericFutureListener<ChannelFuture> {

        private final Logger logger = LoggerFactory.getLogger(ConnectListener.class);

        private final Promise<NetworkChannel> promise;
        private final NetworkListener listener;
        protected ConnectListener(ChannelFuture cFuture, Promise<NetworkChannel> promise, NetworkListener listener) {
            final String methodName = "<init>";
            logger.entry(this, methodName, cFuture, promise, listener);

            this.promise = promise;
            this.listener = listener;

            logger.exit(this, methodName);
        }
        @Override
        public void operationComplete(ChannelFuture cFuture) throws Exception {
            final String methodName = "operationComplete";
            logger.entry(this, methodName, cFuture);

            if (cFuture.isSuccess()) {
                NettyInboundHandler handler = (NettyInboundHandler)cFuture.channel().pipeline().last();
                handler.setListener(listener);
                promise.setSuccess(handler);
            } else {
                ClientException cause = new NetworkException("Could not connect to server: " + cFuture.cause().getMessage(), cFuture.cause());
                promise.setFailure(cause);
                decrementUseCount();
            }

            logger.exit(this, methodName);
        }

    }

    @Override
    public void connect(Endpoint endpoint, NetworkListener listener, Promise<NetworkChannel> promise) {
        final String methodName = "connect";
        logger.entry(this, methodName, endpoint, listener, promise);

        SslContext sslCtx = null;
        try {
            if (endpoint.getCertChainFile() != null && endpoint.getCertChainFile().exists()) {
                try (FileInputStream fileInputStream =
                        new FileInputStream(endpoint.getCertChainFile())) {
                    KeyStore jks = KeyStore.getInstance("JKS");
                    jks.load(fileInputStream, null);
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory
                            .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    trustManagerFactory.init(jks);
                    sslCtx = SslContext.newClientContext();
                    if (sslCtx instanceof JdkSslContext) {
                        ((JdkSslContext) sslCtx).context().init(null,
                                trustManagerFactory.getTrustManagers(), null);
                    }
                } catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException e) {
                    logger.data(methodName, e.toString());
                }
            }
            // fallback to passing as .PEM file (or null, which loads default cacerts)
            if (sslCtx == null) {
                sslCtx = SslContext.newClientContext(endpoint.getCertChainFile());
            }

            SSLEngine sslEngine = sslCtx.newEngine(null, endpoint.getHost(), endpoint.getPort());
            sslEngine.setUseClientMode(true);
            if (endpoint.getVerifyName()) {
                SSLParameters sslParams = sslEngine.getSSLParameters();
                sslParams.setEndpointIdentificationAlgorithm("HTTPS");
                sslEngine.setSSLParameters(sslParams);
            }

            final Bootstrap bootstrap = getBootstrap(endpoint.useSsl(), sslEngine);
            final ChannelFuture f = bootstrap.connect(endpoint.getHost(), endpoint.getPort());
            f.addListener(new ConnectListener(f, promise, listener));

        } catch (SSLException e) {
            if (e.getCause() == null) {
                promise.setFailure(new SecurityException(e.getMessage(), e));
            } else {
                promise.setFailure(new SecurityException(e.getCause().getMessage(), e.getCause()));
            }
        }

        logger.exit(this, methodName);
    }

    private static int useCount = 0;

    /**
     * Request a {@link Bootstrap} for obtaining a {@link Channel} and track
     * that the workerGroup is being used.
     *
     * @param secure
     *            a {@code boolean} indicating whether or not a secure channel
     *            will be required
     * @param sslEngine
     *            an {@link SSLEngine} if one should be used to secure the channel
     * @return a netty {@link Bootstrap} object suitable for obtaining a
     *         {@link Channel} from
     */
    private static synchronized Bootstrap getBootstrap(final boolean secure,
            final SSLEngine sslEngine) {
        final String methodName = "getBootstrap";
        logger.entry(methodName, secure, sslEngine);

        ++useCount;
        if (useCount == 1) {
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            secureBootstrap = new Bootstrap();
            secureBootstrap.group(workerGroup);
            secureBootstrap.channel(NioSocketChannel.class);
            secureBootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            secureBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);
            secureBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addFirst(new SslHandler(sslEngine));
                    ch.pipeline().addLast(new NettyInboundHandler(ch));
                }
            });
            insecureBootstrap = secureBootstrap.clone();
            insecureBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new NettyInboundHandler(ch));
                }
            });
        }

        final Bootstrap result = (secure) ? secureBootstrap : insecureBootstrap;

        logger.exit(methodName, result);

        return result;
    }

    /**
     * Decrement the use count of the workerGroup and request a graceful
     * shutdown once it is no longer being used by anyone.
     */
    private static synchronized void decrementUseCount() {
        final String methodName = "decrementUseCount";
        logger.entry(methodName);

        --useCount;
        if (useCount <= 0) {
            /*
             * NB: workerGroup is shared between both secure and insecure, so
             * we only need to call shutdown via the group on one of them
             */
            if (secureBootstrap != null) {
                secureBootstrap.group().shutdownGracefully(0, 500, TimeUnit.MILLISECONDS);
            }
            secureBootstrap = null;
            insecureBootstrap = null;
            useCount = 0;
        }

        logger.exit(methodName);
    }
}
