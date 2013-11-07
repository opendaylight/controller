/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler.ssh.virtualsocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketAddress;

/**
 * Class provides {@link OutputStream) functionality to users of virtual socket.
 */
public class ChannelOutputStream extends OutputStream implements ChannelOutboundHandler {
    private final Object lock = new Object();
    private ByteBuf buff = Unpooled.buffer();
    private ChannelHandlerContext ctx;

    @Override
    public void flush() throws IOException {
        synchronized(lock) {
            ctx.writeAndFlush(buff).awaitUninterruptibly();
            buff = Unpooled.buffer();
        }
    }

    @Override
    public void write(int b) throws IOException {
        synchronized(lock) {
            buff.writeByte(b);
        }
    }

    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress,
                     ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }

    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                        SocketAddress localAddress, ChannelPromise promise)
            throws Exception {
        this.ctx = ctx;
        ctx.connect(remoteAddress, localAddress, promise);
    }

    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise)
            throws Exception {
        ctx.disconnect(promise);
    }

    public void close(ChannelHandlerContext ctx, ChannelPromise promise)
            throws Exception {
        ctx.close(promise);
    }

    public void deregister(ChannelHandlerContext ctx, ChannelPromise channelPromise)
            throws Exception {
        ctx.deregister(channelPromise);
    }

    public void read(ChannelHandlerContext ctx)
            throws Exception {
        ctx.read();
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
            throws Exception {
        // pass
    }

    public void flush(ChannelHandlerContext ctx)
            throws Exception {
        // pass
    }

    public void handlerAdded(ChannelHandlerContext ctx)
            throws Exception {
    }

    public void handlerRemoved(ChannelHandlerContext ctx)
            throws Exception {
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.fireExceptionCaught(cause);
    }
}
