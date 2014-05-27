/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.osgi;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil.InfixProp;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator for netconf SSH bundle which creates SSH bridge between netconf client and netconf server. Activator
 * starts SSH Server in its own thread. This thread is closed when activator calls stop() method. Server opens socket
 * and listens for client connections. Each client connection creation is handled in separate
 * {@link org.opendaylight.controller.netconf.ssh.threads.Handshaker} thread.
 * This thread creates two additional threads {@link org.opendaylight.controller.netconf.ssh.threads.IOThread}
 * forwarding data from/to client.IOThread closes servers session and server connection when it gets -1 on input stream.
 * {@link org.opendaylight.controller.netconf.ssh.threads.IOThread}'s run method waits for -1 on input stream to finish.
 * All threads are daemons.
 */
public class NetconfSSHActivator implements BundleActivator {
    private static final Logger logger = LoggerFactory.getLogger(NetconfSSHActivator.class);

    private NetconfSSHServer server;

    @Override
    public void start(final BundleContext bundleContext) throws IOException {
        server = startSSHServer(bundleContext);
    }

    @Override
    public void stop(BundleContext context) throws IOException {
        if (server != null) {
            server.close();
        }
    }

    private static NetconfSSHServer startSSHServer(BundleContext bundleContext) throws IOException {
        Optional<InetSocketAddress> maybeSshSocketAddress = NetconfConfigUtil.extractNetconfServerAddress(bundleContext,
                InfixProp.ssh);

        if (maybeSshSocketAddress.isPresent() == false) {
            logger.trace("SSH bridge not configured");
            return null;
        }
        InetSocketAddress sshSocketAddress = maybeSshSocketAddress.get();
        logger.trace("Starting netconf SSH  bridge at {}", sshSocketAddress);

        LocalAddress localAddress = NetconfConfigUtil.getNetconfLocalAddress();

        String path = FilenameUtils.separatorsToSystem(NetconfConfigUtil.getPrivateKeyPath(bundleContext));
        checkState(StringUtils.isNotBlank(path), "Path to ssh private key is blank. Reconfigure %s", NetconfConfigUtil.getPrivateKeyKey());
        String privateKeyPEMString = PEMGenerator.readOrGeneratePK(new File(path));

        final AuthProvider authProvider = new AuthProvider(privateKeyPEMString, bundleContext);
        EventLoopGroup bossGroup  = new NioEventLoopGroup();
        NetconfSSHServer server = NetconfSSHServer.start(sshSocketAddress.getPort(), localAddress, authProvider, bossGroup);

        final Thread serverThread = new Thread(server, "netconf SSH server thread");
        serverThread.setDaemon(true);
        serverThread.start();
        logger.trace("Netconf SSH  bridge up and running.");
        return server;
    }


}
