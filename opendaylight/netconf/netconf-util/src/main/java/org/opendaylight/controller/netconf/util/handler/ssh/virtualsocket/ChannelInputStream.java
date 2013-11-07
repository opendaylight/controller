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
import io.netty.channel.ChannelInboundHandler;

import java.io.IOException;
import java.io.InputStream;

/**
 * Class provides {@link InputStream} functionality to users of virtual socket.
 */
public class ChannelInputStream extends InputStream implements ChannelInboundHandler {
    private final Object lock = new Object();
    private final ByteBuf bb = Unpooled.buffer();

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int bytesRead = 1;
        synchronized (lock) {
            int c = read();

            b[off] = (byte)c;

            if(this.bb.readableBytes() == 0) return bytesRead;

            int ltr = len-1;
            ltr = (ltr <= bb.readableBytes()) ? ltr : bb.readableBytes();

            bb.readBytes(b, 1, ltr);
            bytesRead += ltr;
        }
        return bytesRead;
    }

    @Override
    public int read() throws IOException {
        synchronized (lock) {
            while (this.bb.readableBytes() == 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return this.bb.readByte() & 0xFF;
        }
    }

    @Override
    public int available() throws IOException {
        synchronized (lock) {
            return this.bb.readableBytes();
        }
    }

    public void channelRegistered(ChannelHandlerContext ctx)
            throws Exception {
        ctx.fireChannelRegistered();
    }

    public void channelUnregistered(ChannelHandlerContext ctx)
            throws Exception {
        ctx.fireChannelUnregistered();
    }

    public void channelActive(ChannelHandlerContext ctx)
            throws Exception {
        ctx.fireChannelActive();
    }

    public void channelInactive(ChannelHandlerContext ctx)
            throws Exception {
        ctx.fireChannelInactive();
    }

    public void channelRead(ChannelHandlerContext ctx, Object o)
            throws Exception {
        synchronized(lock) {
            this.bb.discardReadBytes();
            this.bb.writeBytes((ByteBuf) o);
            lock.notifyAll();
        }
    }

    public void channelReadComplete(ChannelHandlerContext ctx)
            throws Exception {
        ctx.fireChannelReadComplete();
    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object o)
            throws Exception {
        ctx.fireUserEventTriggered(o);
    }

    public void channelWritabilityChanged(ChannelHandlerContext ctx)
            throws Exception {
        ctx.fireChannelWritabilityChanged();
    }

    public void handlerAdded(ChannelHandlerContext ctx)
            throws Exception {
    }

    public void handlerRemoved(ChannelHandlerContext ctx)
            throws Exception {
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable)
            throws Exception {
        ctx.fireExceptionCaught(throwable);
    }
}

