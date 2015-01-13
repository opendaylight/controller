/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.DefaultSshFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoConnectFuture;
import org.apache.sshd.common.io.IoConnector;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.IoServiceFactory;
import org.apache.sshd.common.io.IoServiceFactoryFactory;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.io.mina.MinaServiceFactory;
import org.apache.sshd.common.io.nio2.Nio2Connector;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty SSH handler class. Acts as interface between Netty and SSH library.
 */
public class ReversedAsyncSshHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ReversedAsyncSshHandler.class);
    public static final String SUBSYSTEM = "netconf";

    public static final SshClient DEFAULT_CLIENT = SshClient.setUpDefaultClient();

    public static final int SSH_DEFAULT_NIO_WORKERS = 8;

    static {
        // TODO make configurable, or somehow reuse netty threadpool
        DEFAULT_CLIENT.setNioWorkers(SSH_DEFAULT_NIO_WORKERS);
        DEFAULT_CLIENT.start();
    }

    private final AuthenticationHandler authenticationHandler;
    private final SshClient sshClient;

    private AsyncSshHandlerReader sshReadAsyncListener;
    private AsyncSshHandlerWriter sshWriteAsyncHandler;

    private ClientChannel channel;
    private ClientSession session;
    private ChannelPromise connectPromise;


    public static ReversedAsyncSshHandler createForNetconfSubsystem(final AuthenticationHandler authenticationHandler, final IoSession tcpSession) throws IOException {
        return new ReversedAsyncSshHandler(authenticationHandler, DEFAULT_CLIENT, tcpSession);
    }

    /**
     *
     * @param authenticationHandler
     * @param sshClient started SshClient
     * @param tcpSession
     * @throws java.io.IOException
     */
    public ReversedAsyncSshHandler(final AuthenticationHandler authenticationHandler, final SshClient sshClient, final IoSession tcpSession) throws IOException {
        this.authenticationHandler = Preconditions.checkNotNull(authenticationHandler);
        this.sshClient = Preconditions.checkNotNull(sshClient);
        // Start just in case
        sshClient.start();
        sshClient.setIoServiceFactoryFactory(new IoServiceFactoryFactory() {
            @Override
            public IoServiceFactory create(final FactoryManager manager) {
                return new MinaServiceFactory(manager) {
                    @Override
                    public IoConnector createConnector(final IoHandler handler) {
                        try {
                            return new Nio2Connector(manager, handler, AsynchronousChannelGroup.withThreadPool(MoreExecutors.sameThreadExecutor())) {
                                @Override
                                public IoConnectFuture connect(final SocketAddress address) {
                                    DefaultIoConnectFuture defaultIoConnectFuture = new DefaultIoConnectFuture(null);
                                    defaultIoConnectFuture.setSession(tcpSession);
                                    return defaultIoConnectFuture;
                                }
                            };
                        } catch (IOException e) {
                            // FIXME
                            e.printStackTrace();
                        }
                    }
                };
            }
        });
    }

    static class DefaultIoConnectFuture extends DefaultSshFuture<IoConnectFuture> implements IoConnectFuture {
        DefaultIoConnectFuture(Object lock) {
            super(lock);
        }
        public IoSession getSession() {
            Object v = getValue();
            return v instanceof IoSession ? (IoSession) v : null;
        }
        public Throwable getException() {
            Object v = getValue();
            return v instanceof Throwable ? (Throwable) v : null;
        }
        public boolean isConnected() {
            return getValue() instanceof IoSession;
        }
        public void setSession(IoSession session) {
            setValue(session);
        }
        public void setException(Throwable exception) {
            setValue(exception);
        }
    }

    private void startSsh(final ChannelHandlerContext ctx, final SocketAddress address) {
        LOG.debug("Starting SSH to {} on channel: {}", address, ctx.channel());

        final ConnectFuture sshConnectionFuture = sshClient.connect(authenticationHandler.getUsername(), address);
        sshConnectionFuture.addListener(new SshFutureListener<ConnectFuture>() {
            @Override
            public void operationComplete(final ConnectFuture future) {
                if (future.isConnected()) {
                    handleSshSessionCreated(future, ctx);
                } else {
                    handleSshSetupFailure(ctx, future.getException());
                }
            }
        });
    }

    private synchronized void handleSshSessionCreated(final ConnectFuture future, final ChannelHandlerContext ctx) {
        try {
            LOG.trace("SSH session created on channel: {}", ctx.channel());

            session = future.getSession();
            final AuthFuture authenticateFuture = authenticationHandler.authenticate(session);
            authenticateFuture.addListener(new SshFutureListener<AuthFuture>() {
                @Override
                public void operationComplete(final AuthFuture future) {
                    if (future.isSuccess()) {
                        handleSshAuthenticated(session, ctx);
                    } else {
                        // Exception does not have to be set in the future, add simple exception in such case
                        final Throwable exception = future.getException() == null ?
                                new IllegalStateException("Authentication failed") :
                                future.getException();
                        handleSshSetupFailure(ctx, exception);
                    }
                }
            });
        } catch (final IOException e) {
            handleSshSetupFailure(ctx, e);
        }
    }

    private synchronized void handleSshAuthenticated(final ClientSession session, final ChannelHandlerContext ctx) {
        try {
            LOG.debug("SSH session authenticated on channel: {}, server version: {}", ctx.channel(), session.getServerVersion());

            channel = session.createSubsystemChannel(SUBSYSTEM);
            channel.setStreaming(ClientChannel.Streaming.Async);
            channel.open().addListener(new SshFutureListener<OpenFuture>() {
                @Override
                public void operationComplete(final OpenFuture future) {
                    if(future.isOpened()) {
                        handleSshChanelOpened(ctx);
                    } else {
                        handleSshSetupFailure(ctx, future.getException());
                    }
                }
            });


        } catch (final IOException e) {
            handleSshSetupFailure(ctx, e);
        }
    }

    private synchronized void handleSshChanelOpened(final ChannelHandlerContext ctx) {
        LOG.trace("SSH subsystem channel opened successfully on channel: {}", ctx.channel());

        connectPromise.setSuccess();

        // TODO we should also read from error stream and at least log from that

        sshReadAsyncListener = new AsyncSshHandlerReader(new AutoCloseable() {
            @Override
            public void close() throws Exception {
                ReversedAsyncSshHandler.this.disconnect(ctx, ctx.newPromise());
            }
        }, new AsyncSshHandlerReader.ReadMsgHandler() {
            @Override
            public void onMessageRead(final ByteBuf msg) {
                ctx.fireChannelRead(msg);
            }
        }, channel.toString(), channel.getAsyncOut());

        // if readAsyncListener receives immediate close, it will close this handler and closing this handler sets channel variable to null
        if(channel != null) {
            sshWriteAsyncHandler = new AsyncSshHandlerWriter(channel.getAsyncIn());
            ctx.fireChannelActive();
        }
    }

    private synchronized void handleSshSetupFailure(final ChannelHandlerContext ctx, final Throwable e) {
        LOG.warn("Unable to setup SSH connection on channel: {}", ctx.channel(), e);
        disconnect(ctx, ctx.newPromise());

        // If the promise is not yet done, we have failed with initial connect and set connectPromise to failure
        if(!connectPromise.isDone()) {
            connectPromise.setFailure(e);
        }
    }

    @Override
    public synchronized void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
        sshWriteAsyncHandler.write(ctx, msg, promise);
    }

    @Override
    public synchronized void connect(final ChannelHandlerContext ctx, final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) throws Exception {
        LOG.debug("SSH session connecting on channel {}. promise: {} ", ctx.channel(), connectPromise);
        this.connectPromise = promise;
        startSsh(ctx, remoteAddress);
    }

    @Override
    public void close(final ChannelHandlerContext ctx, final ChannelPromise promise) throws Exception {
        disconnect(ctx, promise);
    }

    @Override
    public synchronized void disconnect(final ChannelHandlerContext ctx, final ChannelPromise promise) {
        LOG.trace("Closing SSH session on channel: {} with connect promise in state: {}", ctx.channel(), connectPromise);

        // If we have already succeeded and the session was dropped after, we need to fire inactive to notify reconnect logic
        if(connectPromise.isSuccess()) {
            ctx.fireChannelInactive();
        }

        if(sshWriteAsyncHandler != null) {
            sshWriteAsyncHandler.close();
        }

        if(sshReadAsyncListener != null) {
            sshReadAsyncListener.close();
        }

        if(session!= null && !session.isClosed() && !session.isClosing()) {
            session.close(false).addListener(new SshFutureListener<CloseFuture>() {
                @Override
                public void operationComplete(final CloseFuture future) {
                    if (future.isClosed() == false) {
                        session.close(true);
                    }
                    session = null;
                }
            });
        }

        // Super disconnect is necessary in this case since we are using NioSocketChannel and it needs to cleanup its resources
        // e.g. Socket that it tries to open in its constructor (https://bugs.opendaylight.org/show_bug.cgi?id=2430)
        // TODO better solution would be to implement custom ChannelFactory + Channel that will use mina SSH lib internally: port this to custom channel implementation
        try {
            // Disconnect has to be closed after inactive channel event was fired, because it interferes with it
            super.disconnect(ctx, ctx.newPromise());
        } catch (final Exception e) {
            LOG.warn("Unable to cleanup all resources for channel: {}. Ignoring.", ctx.channel(), e);
        }

        channel = null;
        promise.setSuccess();
        LOG.debug("SSH session closed on channel: {}", ctx.channel());
    }

}
