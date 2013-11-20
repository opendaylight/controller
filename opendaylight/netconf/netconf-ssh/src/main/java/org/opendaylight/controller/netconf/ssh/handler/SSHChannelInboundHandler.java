/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.handler;

import ch.ethz.ssh2.ServerSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class SSHChannelInboundHandler extends SimpleChannelInboundHandler {

    private ServerSession serverSession;

    public SSHChannelInboundHandler(ServerSession serverSession) {
        this.serverSession = serverSession;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        this.serverSession.getStdin().write( ((ByteBuf)msg).readBytes(((ByteBuf)msg).readableBytes()).array());
    }
}
