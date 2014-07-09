/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.framework;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;

/**
 * Channel inbound handler with noop implementation for all methods. Serves for overriding.
 */
public class NoopInboundChannelHandler implements ChannelInboundHandler {
    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {}

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {}

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {}

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {}

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {}

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {}

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {}

    @Override
    public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {}

    @Override
    public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {}

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {}

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {}
}
