/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.ssh;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.sshd.common.io.IoInputStream;
import org.apache.sshd.common.io.IoOutputStream;
import org.apache.sshd.server.ExitCallback;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.client.AsyncSshHandlerReader;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.client.AsyncSshHandlerWriter;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty handler that reads SSH from remote client and writes to delegate server and reads from delegate server and writes to remote client
 */
final class SshProxyClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SshProxyClientHandler.class);

    private final IoInputStream in;
    private final IoOutputStream out;

    private AsyncSshHandlerReader asyncSshHandlerReader;
    private AsyncSshHandlerWriter asyncSshHandlerWriter;

    private final NetconfHelloMessageAdditionalHeader netconfHelloMessageAdditionalHeader;
    private final ExitCallback callback;

    public SshProxyClientHandler(final IoInputStream in, final IoOutputStream out,
                                 final NetconfHelloMessageAdditionalHeader netconfHelloMessageAdditionalHeader,
                                 final ExitCallback callback) {
        this.in = in;
        this.out = out;
        this.netconfHelloMessageAdditionalHeader = netconfHelloMessageAdditionalHeader;
        this.callback = callback;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        writeAdditionalHeader(ctx);

        asyncSshHandlerWriter = new AsyncSshHandlerWriter(out);
        asyncSshHandlerReader = new AsyncSshHandlerReader(new AutoCloseable() {
            @Override
            public void close() throws Exception {
                // Close both sessions (delegate server and remote client)
                ctx.fireChannelInactive();
                ctx.disconnect();
                ctx.close();
                asyncSshHandlerReader.close();
                asyncSshHandlerWriter.close();
            }
        }, new AsyncSshHandlerReader.ReadMsgHandler() {
            @Override
            public void onMessageRead(final ByteBuf msg) {
                if(LOG.isTraceEnabled()) {
                    LOG.trace("Forwarding message for client: {} on channel: {}, message: {}",
                            netconfHelloMessageAdditionalHeader.getAddress(), ctx.channel(), AsyncSshHandlerWriter.byteBufToString(msg));
                }
                // Just forward to delegate
                ctx.writeAndFlush(msg);
            }
        }, "ssh" + netconfHelloMessageAdditionalHeader.getAddress(), in);


        super.channelActive(ctx);
    }

    private void writeAdditionalHeader(final ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.copiedBuffer(netconfHelloMessageAdditionalHeader.toFormattedString().getBytes()));
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        asyncSshHandlerWriter.write(ctx, msg, ctx.newPromise());
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
        LOG.debug("Internal connection to netconf server was dropped for client: {} on channel: ",
                netconfHelloMessageAdditionalHeader.getAddress(), ctx.channel());
        callback.onExit(1, "Internal connection to netconf server was dropped for client: " +
                netconfHelloMessageAdditionalHeader.getAddress() + " on channel: " + ctx.channel());
        super.channelInactive(ctx);
    }


}
