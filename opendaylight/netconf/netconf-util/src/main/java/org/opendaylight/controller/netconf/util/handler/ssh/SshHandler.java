/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler.ssh;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.io.IOException;
import java.net.SocketAddress;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.handler.ssh.client.Invoker;
import org.opendaylight.controller.netconf.util.handler.ssh.client.SshClient;
import org.opendaylight.controller.netconf.util.handler.ssh.client.SshClientAdapter;
import org.opendaylight.controller.netconf.util.handler.ssh.virtualsocket.VirtualSocket;

/**
 * Netty SSH handler class. Acts as interface between Netty and SSH library. All standard Netty message handling
 * stops at instance of this class. All downstream events are handed of to wrapped {@link org.opendaylight.controller.netconf.util.handler.ssh.client.SshClientAdapter};
 */
public class SshHandler extends ChannelOutboundHandlerAdapter {
    private final VirtualSocket virtualSocket = new VirtualSocket();
    private final SshClientAdapter sshClientAdapter;

    public SshHandler(AuthenticationHandler authenticationHandler, Invoker invoker) throws IOException {
        SshClient sshClient = new SshClient(virtualSocket, authenticationHandler);
        this.sshClientAdapter = new SshClientAdapter(sshClient, invoker);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx){
        if (ctx.channel().pipeline().get("socket") == null) {
            ctx.channel().pipeline().addFirst("socket", virtualSocket);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().pipeline().get("socket") != null) {
            ctx.channel().pipeline().remove("socket");
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        this.sshClientAdapter.write((String) msg);
    }

    @Override
    public void connect(final ChannelHandlerContext ctx,
                        SocketAddress remoteAddress,
                        SocketAddress localAddress,
                        ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);

        promise.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                sshClientAdapter.start(ctx);
            }}
        );
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        sshClientAdapter.stop(promise);
    }
}
