/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoInputStream;
import org.apache.sshd.common.io.IoOutputStream;
import org.apache.sshd.common.io.IoReadFuture;
import org.apache.sshd.common.util.Buffer;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty SSH handler class. Acts as interface between Netty and SSH library. All standard Netty message handling
 * stops at instance of this class. All downstream events are handed of to wrapped {@link SshClientAdapter};
 */
public class MinaSshHandler extends ChannelOutboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MinaSshHandler.class);
    private static final String SOCKET = "socket";

//    private final VirtualSocket virtualSocket = new VirtualSocket();

    private final AuthenticationHandler authenticationHandler;
    private final SshClient sshClient;
    private final AtomicBoolean sshInitialized = new AtomicBoolean(false);
    private IoOutputStream asyncIn;


    public static MinaSshHandler createForNetconfSubsystem(final AuthenticationHandler authenticationHandler) throws IOException {
        return new MinaSshHandler(authenticationHandler);
    }

    public MinaSshHandler(final AuthenticationHandler authenticationHandler) throws IOException {
        this.authenticationHandler = authenticationHandler;
        sshClient = SshClient.setUpDefaultClient();
    }

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) {


    }

    private void startSsh(final ChannelHandlerContext ctx, final SocketAddress address) {
        logger.debug("Starting SSH to {}", address);

        sshClient.start();
        final ConnectFuture sshConnectionFuture = sshClient.connect(authenticationHandler.getUsername(), address);
        sshConnectionFuture.addListener(new SshFutureListener<ConnectFuture>() {
            @Override
            public void operationComplete(final ConnectFuture future) {
                if (future.isConnected()) {
                    final ClientSession session = future.getSession();
                    try {
                        authenticationHandler.authenticate(session).addListener(new SshFutureListener<AuthFuture>() {
                            @Override
                            public void operationComplete(final AuthFuture future) {
                                if (future.isSuccess()) {
                                    try {
                                        final ClientChannel channel = session.createChannel(ClientChannel.CHANNEL_SUBSYSTEM, "netconf");
                                        channel.setStreaming(ClientChannel.Streaming.Async);

                                        channel.open().addListener(new SshFutureListener<OpenFuture>() {
                                            @Override
                                            public void operationComplete(final OpenFuture future) {
                                                if(future.isOpened()) {
                                                    sshInitialized.set(true);

                                                    final IoInputStream asyncOut = channel.getAsyncOut();
                                                    new MinaReadListener(ctx, asyncOut);
                                                    MinaSshHandler.this.asyncIn = channel.getAsyncIn();

                                                    while (postponed.peek() != null) {
                                                        PostponedMessage poll = postponed.poll();
                                                        write(poll.ctx, poll.msg, poll.promise);
                                                    }
                                                } else {
                                                    throw new IllegalStateException("Unable to setup SSH connection", future.getException());
                                                }
                                            }
                                        });


                                    } catch (final IOException e) {
                                        throw new IllegalStateException("Unable to setup SSH connection", e);
                                    }
                                } else {
                                    throw new IllegalStateException("Unable to setup SSH connection", future
                                            .getException());
                                }
                            }
                        });
                    } catch (final IOException e) {
                        throw new IllegalStateException("Unable to setup SSH connection", e);
                    }
                } else {
                    throw new IllegalStateException("Unable to setup SSH connection", future.getException());
                }
            }
        });
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) {
        if (ctx.channel().pipeline().get(SOCKET) != null) {
            ctx.channel().pipeline().remove(SOCKET);
        }
    }

    private final Queue<PostponedMessage> postponed = new LinkedList<>();

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
        if(sshInitialized.get()) {
            try {
                asyncIn.write(toBuffer(msg));
            } catch (final Exception e) {
                throw new IllegalStateException("Unexpected exception while writing message " + msg, e);
            }
        } else {
            postponed.add(new PostponedMessage(ctx, msg, promise));
        }
    }

    private Buffer toBuffer(final Object msg) {
        // TODO Buffer vs ByteBuf translate
        Preconditions.checkState(msg instanceof ByteBuf);
        ByteBuf msg1 = (ByteBuf) msg;
        byte[] temp = new byte[msg1.readableBytes()];
        msg1.readBytes(temp, 0, msg1.readableBytes());
        return new Buffer(temp);
    }

    @Override
    public void connect(final ChannelHandlerContext ctx, final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);

        promise.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture channelFuture) {
                    if (channelFuture.isSuccess()) {
                        startSsh(ctx, remoteAddress);
                    } else {
                        logger.debug("Failed to connect to remote host");
                    }
                }
            }
        );
    }

    @Override
    public void disconnect(final ChannelHandlerContext ctx, final ChannelPromise promise) throws Exception {
        // FIXME disconnect
    }

    private static class MinaReadListener implements SshFutureListener<IoReadFuture> {
        Buffer buf;
        private final ChannelHandlerContext ctx;
        private final IoInputStream asyncOut;

        public MinaReadListener(final ChannelHandlerContext ctx, final IoInputStream asyncOut) {
            this.ctx = ctx;
            this.asyncOut = asyncOut;
            buf = new Buffer(8192);
            asyncOut.read(buf).addListener(this);
        }

        @Override
        public void operationComplete(final IoReadFuture future) {
            // TODO check state
            if (future.getRead() > 0) {
                byte[] array = buf.array();
                // FIXME Ganymed sends some header over Hello message
                if(new String(array).startsWith("SSH-2.0-Ganymed_SSHD_null")) {
                    ctx.fireChannelRead(Unpooled.copiedBuffer(array, "SSH-2.0-Ganymed_SSHD_null".length(), future.getRead()));
                } else {
                    ctx.fireChannelRead(Unpooled.copiedBuffer(array, 0, future.getRead()));
                }
                buf = new Buffer(8192);
                asyncOut.read(buf).addListener(this);
            }
        }
    }

    private class PostponedMessage {
        private final ChannelHandlerContext ctx;
        private final Object msg;
        private final ChannelPromise promise;

        public PostponedMessage(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
            this.ctx = ctx;
            this.msg = msg;

            this.promise = promise;
        }
    }

}
