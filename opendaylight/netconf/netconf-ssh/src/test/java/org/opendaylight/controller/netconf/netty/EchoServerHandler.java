/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.netty;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler implementation for the echo server.
 */
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(EchoServerHandler.class);
    private String fromLastNewLine = "";
    private final Splitter splitter = Splitter.onPattern("\r?\n");
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.debug("sleep start");
        Thread.sleep(1000);
        LOG.debug("sleep done");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        String message = byteBuf.toString(Charsets.UTF_8);
        LOG.info("writing back '{}'", message);
        ctx.write(msg);
        fromLastNewLine += message;
        for (String line : splitter.split(fromLastNewLine)) {
            if ("quit".equals(line)) {
                LOG.info("closing server ctx");
                ctx.flush();
                ctx.close();
                break;
            }
            fromLastNewLine = line; // last line should be preserved
        }

        // do not release byteBuf as it is handled back
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        LOG.debug("flushing");
        ctx.flush();
    }
}
