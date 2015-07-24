/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.netty;

import com.google.common.base.Charsets;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyServerHandler.class);
    private final Bootstrap clientBootstrap;
    private final LocalAddress localAddress;


    private Channel clientChannel;

    public ProxyServerHandler(EventLoopGroup bossGroup, LocalAddress localAddress) {
        clientBootstrap = new Bootstrap();
        clientBootstrap.group(bossGroup).channel(LocalChannel.class);
        this.localAddress = localAddress;
    }

    @Override
    public void channelActive(ChannelHandlerContext remoteCtx) {
        final ProxyClientHandler clientHandler = new ProxyClientHandler(remoteCtx);
        clientBootstrap.handler(new ChannelInitializer<LocalChannel>() {
            @Override
            public void initChannel(LocalChannel ch) throws Exception {
                ch.pipeline().addLast(clientHandler);
            }
        });
        ChannelFuture clientChannelFuture = clientBootstrap.connect(localAddress).awaitUninterruptibly();
        clientChannel = clientChannelFuture.channel();
        clientChannel.writeAndFlush(Unpooled.copiedBuffer("connected\n".getBytes()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOG.info("channelInactive - closing client connection");
        clientChannel.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, final Object msg) {
        LOG.debug("Writing to client {}", msg);
        clientChannel.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        LOG.debug("flushing");
        clientChannel.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        LOG.warn("Unexpected exception from downstream.", cause);
        ctx.close();
    }
}

class ProxyClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyClientHandler.class);

    private final ChannelHandlerContext remoteCtx;


    public ProxyClientHandler(ChannelHandlerContext remoteCtx) {
        this.remoteCtx = remoteCtx;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOG.info("client active");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf bb = (ByteBuf) msg;
        LOG.info(">{}", bb.toString(Charsets.UTF_8));
        remoteCtx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        LOG.debug("Flushing server ctx");
        remoteCtx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        LOG.warn("Unexpected exception from downstream", cause);
        ctx.close();
    }

    // called both when local or remote connection dies
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOG.debug("channelInactive() called, closing remote client ctx");
        remoteCtx.close();
    }
}
