/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.ssh;

import com.google.common.collect.Lists;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.KeyPairProvider;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PasswordAuthenticator;

public class SshProxyServer implements AutoCloseable {
    public static final int NIO_WORKERS = 8;

    private final EventLoopGroup clientGroup = new NioEventLoopGroup();
    private final SshServer sshServer;

    public SshProxyServer(final InetSocketAddress bindingAddress, final LocalAddress localAddress, final PasswordAuthenticator authenticator, final KeyPairProvider keyPairProvider) throws IOException {
        final Bootstrap clientBootstrap = new Bootstrap();
        clientBootstrap.group(clientGroup).channel(LocalChannel.class);

        // Configure the server.
        this.sshServer = SshServer.setUpDefaultServer();
        sshServer.setHost(bindingAddress.getHostName());
        sshServer.setPort(bindingAddress.getPort());
        sshServer.setPasswordAuthenticator(authenticator);

        sshServer.setKeyPairProvider(keyPairProvider);
        sshServer.setNioWorkers(NIO_WORKERS);
        final RemoteNetconfCommand.NetconfCommandFactory netconfCommandFactory =
                new RemoteNetconfCommand.NetconfCommandFactory(clientBootstrap, localAddress);

        sshServer.setSubsystemFactories(Lists.<NamedFactory<Command>>newArrayList(netconfCommandFactory));

        sshServer.start();
    }

    @Override
    public void close() {
        try {
            sshServer.stop(true);
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while stopping sshServer", e);
        } finally {
            sshServer.close(true);
            clientGroup.shutdownGracefully();
        }
    }
}
