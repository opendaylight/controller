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
import com.google.common.base.Strings;
import io.netty.channel.local.LocalAddress;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.commons.io.FilenameUtils;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.opendaylight.controller.netconf.ssh.SshProxyServer;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil.InfixProp;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfSSHActivator implements BundleActivator {
    private static final Logger logger = LoggerFactory.getLogger(NetconfSSHActivator.class);
    private static AuthProviderTracker authProviderTracker;

    private static final java.lang.String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 4096;

    private SshProxyServer server;

    @Override
    public void start(final BundleContext bundleContext) throws IOException {
        server = startSSHServer(bundleContext);
    }

    @Override
    public void stop(final BundleContext context) throws IOException {
        if (server != null) {
            server.close();
        }

        if(authProviderTracker != null) {
            authProviderTracker.stop();
        }
    }

    private static SshProxyServer startSSHServer(final BundleContext bundleContext) throws IOException {
        final Optional<InetSocketAddress> maybeSshSocketAddress = NetconfConfigUtil.extractNetconfServerAddress(bundleContext, InfixProp.ssh);

        if (maybeSshSocketAddress.isPresent() == false) {
            logger.trace("SSH bridge not configured");
            return null;
        }

        final InetSocketAddress sshSocketAddress = maybeSshSocketAddress.get();
        logger.trace("Starting netconf SSH bridge at {}", sshSocketAddress);

        final LocalAddress localAddress = NetconfConfigUtil.getNetconfLocalAddress();

        authProviderTracker = new AuthProviderTracker(bundleContext);

        final String path = FilenameUtils.separatorsToSystem(NetconfConfigUtil.getPrivateKeyPath(bundleContext));
        checkState(!Strings.isNullOrEmpty(path), "Path to ssh private key is blank. Reconfigure %s",
                NetconfConfigUtil.getPrivateKeyKey());

        return new SshProxyServer(sshSocketAddress, localAddress, authProviderTracker,
                new PEMGeneratorHostKeyProvider(path, ALGORITHM, KEY_SIZE));
    }

}
