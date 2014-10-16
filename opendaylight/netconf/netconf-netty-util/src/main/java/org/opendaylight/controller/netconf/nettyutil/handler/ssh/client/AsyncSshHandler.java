/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

/**
 * Netty SSH handler class. Acts as interface between Netty and SSH library.
 */
public class AsyncSshHandler extends ChannelOutboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AsyncSshHandler.class);
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


    public static AsyncSshHandler createForNetconfSubsystem(final AuthenticationHandler authenticationHandler) throws IOException {
        return new AsyncSshHandler(authenticationHandler, DEFAULT_CLIENT);
    }

    /**
     *
     * @param authenticationHandler
     * @param sshClient started SshClient
     * @throws IOException
     */
    public AsyncSshHandler(final AuthenticationHandler authenticationHandler, final SshClient sshClient) throws IOException {
        this.authenticationHandler = Preconditions.checkNotNull(authenticationHandler);
        this.sshClient = Preconditions.checkNotNull(sshClient);
        // Start just in case
        sshClient.start();
    }

    private void startSsh(final ChannelHandlerContext ctx, final SocketAddress address) {
        logger.debug("Starting SSH to {} on channel: {}", address, ctx.channel());

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
            logger.trace("SSH session created on channel: {}", ctx.channel());

            session = future.getSession();
            final AuthFuture authenticateFuture = authenticationHandler.authenticate(session);
            authenticateFuture.addListener(new SshFutureListener<AuthFuture>() {
                @Override
                public void operationComplete(final AuthFuture future) {
                    if (future.isSuccess()) {
                        handleSshAuthenticated(session, ctx);
                    } else {
                        handleSshSetupFailure(ctx, future.getException());
                    }
                }
            });
        } catch (final IOException e) {
            handleSshSetupFailure(ctx, e);
        }
    }

    private synchronized void handleSshAuthenticated(final ClientSession session, final ChannelHandlerContext ctx) {
        try {
            logger.debug("SSH session authenticated on channel: {}, server version: {}", ctx.channel(), session.getServerVersion());

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
        logger.trace("SSH subsystem channel opened successfully on channel: {}", ctx.channel());

        connectPromise.setSuccess();
        connectPromise = null;

        // TODO we should also read from error stream and at least log from that

        sshReadAsyncListener = new AsyncSshHandlerReader(new AutoCloseable() {
            @Override
            public void close() throws Exception {
                AsyncSshHandler.this.disconnect(ctx, ctx.newPromise());
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
        logger.warn("Unable to setup SSH connection on channel: {}", ctx.channel(), e);
        connectPromise.setFailure(e);
        connectPromise = null;
        throw new IllegalStateException("Unable to setup SSH connection on channel: " + ctx.channel(), e);
    }

    @Override
    public synchronized void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
        sshWriteAsyncHandler.write(ctx, msg, promise);
    }

    @Override
    public synchronized void connect(final ChannelHandlerContext ctx, final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) throws Exception {
        this.connectPromise = promise;
        startSsh(ctx, remoteAddress);
    }

    @Override
    public void close(final ChannelHandlerContext ctx, final ChannelPromise promise) throws Exception {
        disconnect(ctx, promise);
    }

    @Override
    public synchronized void disconnect(final ChannelHandlerContext ctx, final ChannelPromise promise) {
        if(sshReadAsyncListener != null) {
            sshReadAsyncListener.close();
        }

        if(sshWriteAsyncHandler != null) {
            sshWriteAsyncHandler.close();
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

        channel = null;
        promise.setSuccess();

        logger.debug("SSH session closed on channel: {}", ctx.channel());
        ctx.fireChannelInactive();
    }

}
