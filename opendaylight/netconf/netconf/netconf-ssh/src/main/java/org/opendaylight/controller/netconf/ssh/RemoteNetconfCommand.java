/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.ssh;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.util.concurrent.GenericFutureListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.io.IoInputStream;
import org.apache.sshd.common.io.IoOutputStream;
import org.apache.sshd.server.AsyncCommand;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This command handles all netconf related rpc and forwards to delegate server.
 * Uses netty to make a local connection to delegate server.
 *
 * Command is Apache Mina SSH terminology for objects handling ssh data.
 */
public class RemoteNetconfCommand implements AsyncCommand, SessionAware {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteNetconfCommand.class);

    private final EventLoopGroup clientEventGroup;
    private final LocalAddress localAddress;

    private IoInputStream in;
    private IoOutputStream out;
    private ExitCallback callback;
    private NetconfHelloMessageAdditionalHeader netconfHelloMessageAdditionalHeader;

    private Channel clientChannel;
    private ChannelFuture clientChannelFuture;

    public RemoteNetconfCommand(final EventLoopGroup clientEventGroup, final LocalAddress localAddress) {
        this.clientEventGroup = clientEventGroup;
        this.localAddress = localAddress;
    }

    @Override
    public void setIoInputStream(final IoInputStream in) {
        this.in = in;
    }

    @Override
    public void setIoOutputStream(final IoOutputStream out) {
        this.out = out;
    }

    @Override
    public void setIoErrorStream(final IoOutputStream err) {
        // TODO do we want to use error stream in some way ?
    }

    @Override
    public void setInputStream(final InputStream in) {
        throw new UnsupportedOperationException("Synchronous IO is unsupported");
    }

    @Override
    public void setOutputStream(final OutputStream out) {
        throw new UnsupportedOperationException("Synchronous IO is unsupported");

    }

    @Override
    public void setErrorStream(final OutputStream err) {
        throw new UnsupportedOperationException("Synchronous IO is unsupported");

    }

    @Override
    public void setExitCallback(final ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(final Environment env) throws IOException {
        LOG.trace("Establishing internal connection to netconf server for client: {}", getClientAddress());

        final Bootstrap clientBootstrap = new Bootstrap();
        clientBootstrap.group(clientEventGroup).channel(LocalChannel.class);

        clientBootstrap
                .handler(new ChannelInitializer<LocalChannel>() {
                    @Override
                    public void initChannel(final LocalChannel ch) throws Exception {
                        ch.pipeline().addLast(new SshProxyClientHandler(in, out, netconfHelloMessageAdditionalHeader, callback));
                    }
                });
        clientChannelFuture = clientBootstrap.connect(localAddress);
        clientChannelFuture.addListener(new GenericFutureListener<ChannelFuture>() {

            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                if(future.isSuccess()) {
                    clientChannel = clientChannelFuture.channel();
                } else {
                    LOG.warn("Unable to establish internal connection to netconf server for client: {}", getClientAddress());
                    Preconditions.checkNotNull(callback, "Exit callback must be set");
                    callback.onExit(1, "Unable to establish internal connection to netconf server for client: "+ getClientAddress());
                }
            }
        });
    }

    @Override
    public void destroy() {
        LOG.trace("Releasing internal connection to netconf server for client: {} on channel: {}",
                getClientAddress(), clientChannel);

        clientChannelFuture.cancel(true);
        if(clientChannel != null) {
            clientChannel.close().addListener(new GenericFutureListener<ChannelFuture>() {

                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                    if (future.isSuccess() == false) {
                        LOG.warn("Unable to release internal connection to netconf server on channel: {}", clientChannel);
                    }
                }
            });
        }
    }

    private String getClientAddress() {
        return netconfHelloMessageAdditionalHeader.getAddress();
    }

    @Override
    public void setSession(final ServerSession session) {
        final SocketAddress remoteAddress = session.getIoSession().getRemoteAddress();
        String hostName = "";
        String port = "";
        if(remoteAddress instanceof InetSocketAddress) {
            hostName = ((InetSocketAddress) remoteAddress).getAddress().getHostAddress();
            port = Integer.toString(((InetSocketAddress) remoteAddress).getPort());
        }
        netconfHelloMessageAdditionalHeader = new NetconfHelloMessageAdditionalHeader(
                session.getUsername(), hostName, port, "ssh", "client");
    }

    public static class NetconfCommandFactory implements NamedFactory<Command> {

        public static final String NETCONF = "netconf";

        private final EventLoopGroup clientBootstrap;
        private final LocalAddress localAddress;

        public NetconfCommandFactory(final EventLoopGroup clientBootstrap, final LocalAddress localAddress) {

            this.clientBootstrap = clientBootstrap;
            this.localAddress = localAddress;
        }

        @Override
        public String getName() {
            return NETCONF;
        }

        @Override
        public RemoteNetconfCommand create() {
            return new RemoteNetconfCommand(clientBootstrap, localAddress);
        }
    }

}
