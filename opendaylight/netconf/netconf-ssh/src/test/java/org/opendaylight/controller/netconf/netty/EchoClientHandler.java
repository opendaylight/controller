/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.netty;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler implementation for the echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public class EchoClientHandler extends ChannelInboundHandlerAdapter implements ChannelFutureListener {
    private static final Logger LOG = LoggerFactory.getLogger(EchoClientHandler.class);

    private ChannelHandlerContext ctx;
    private final StringBuilder fromServer = new StringBuilder();

    public static enum State {CONNECTING, CONNECTED, FAILED_TO_CONNECT, CONNECTION_CLOSED}


    private State state = State.CONNECTING;

    @Override
    public synchronized void channelActive(ChannelHandlerContext ctx) {
        checkState(this.ctx == null);
        LOG.info("channelActive");
        this.ctx = ctx;
        state = State.CONNECTED;
    }

    @Override
    public synchronized void channelInactive(ChannelHandlerContext ctx) throws Exception {
        state = State.CONNECTION_CLOSED;
    }

    @Override
    public synchronized void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf bb = (ByteBuf) msg;
        String string = bb.toString(Charsets.UTF_8);
        fromServer.append(string);
        LOG.info(">{}", string);
        bb.release();
    }

    @Override
    public synchronized void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        LOG.warn("Unexpected exception from downstream.", cause);
        checkState(this.ctx.equals(ctx));
        ctx.close();
        this.ctx = null;
    }

    public synchronized void write(String message) {
        ByteBuf byteBuf = Unpooled.copiedBuffer(message.getBytes());
        ctx.writeAndFlush(byteBuf);
    }

    public synchronized boolean isConnected() {
        return state == State.CONNECTED;
    }

    public synchronized String read() {
        return fromServer.toString();
    }

    @Override
    public synchronized void operationComplete(ChannelFuture future) throws Exception {
        checkState(state == State.CONNECTING);
        if (future.isSuccess()) {
            LOG.trace("Successfully connected, state will be switched in channelActive");
        } else {
            state = State.FAILED_TO_CONNECT;
        }
    }

    public State getState() {
        return state;
    }
}
