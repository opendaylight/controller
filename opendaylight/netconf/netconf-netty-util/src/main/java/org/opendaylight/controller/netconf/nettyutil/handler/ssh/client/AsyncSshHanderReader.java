/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoInputStream;
import org.apache.sshd.common.io.IoReadFuture;
import org.apache.sshd.common.util.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener on async input stream from SSH session.
 * This listeners schedules reads in a loop until the session is closed or read fails.
 */
final class AsyncSshHanderReader implements SshFutureListener<IoReadFuture>, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncSshHandler.class);

    private static final int BUFFER_SIZE = 8192;

    private final ChannelOutboundHandler asyncSshHandler;
    private final ChannelHandlerContext ctx;

    private IoInputStream asyncOut;
    private Buffer buf;
    private IoReadFuture currentReadFuture;

    public AsyncSshHanderReader(final ChannelOutboundHandler asyncSshHandler, final ChannelHandlerContext ctx, final IoInputStream asyncOut) {
        this.asyncSshHandler = asyncSshHandler;
        this.ctx = ctx;
        this.asyncOut = asyncOut;
        buf = new Buffer(BUFFER_SIZE);
        asyncOut.read(buf).addListener(this);
    }

    @Override
    public synchronized void operationComplete(final IoReadFuture future) {
        if(future.getException() != null) {
            if(asyncOut.isClosed() || asyncOut.isClosing()) {
                // Ssh dropped
                LOGGER.debug("Ssh session dropped on channel: {}", ctx.channel(), future.getException());
            } else {
                LOGGER.warn("Exception while reading from SSH remote on channel {}", ctx.channel(), future.getException());
            }
            invokeDisconnect();
            return;
        }

        if (future.getRead() > 0) {
            ctx.fireChannelRead(Unpooled.wrappedBuffer(buf.array(), 0, future.getRead()));

            // Schedule next read
            buf = new Buffer(BUFFER_SIZE);
            currentReadFuture = asyncOut.read(buf);
            currentReadFuture.addListener(this);
        }
    }

    private void invokeDisconnect() {
        try {
            asyncSshHandler.disconnect(ctx, ctx.newPromise());
        } catch (final Exception e) {
            // This should not happen
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized void close() {
        // Remove self as listener on close to prevent reading from closed input
        if(currentReadFuture != null) {
            currentReadFuture.removeListener(this);
        }

        asyncOut = null;
    }
}
