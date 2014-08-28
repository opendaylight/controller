/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.osgi;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.commons.io.FilenameUtils;
import org.opendaylight.controller.netconf.auth.AuthConstants;
import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil.InfixProp;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;

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
    private static AuthProviderTracker authProviderTracker;

    private NetconfSSHServer server;

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

    private static NetconfSSHServer startSSHServer(final BundleContext bundleContext) throws IOException {
        final Optional<InetSocketAddress> maybeSshSocketAddress = NetconfConfigUtil.extractNetconfServerAddress(bundleContext,
                InfixProp.ssh);

        if (maybeSshSocketAddress.isPresent() == false) {
            logger.trace("SSH bridge not configured");
            return null;
        }

        final InetSocketAddress sshSocketAddress = maybeSshSocketAddress.get();
        logger.trace("Starting netconf SSH bridge at {}", sshSocketAddress);

        final LocalAddress localAddress = NetconfConfigUtil.getNetconfLocalAddress();

        final String path = FilenameUtils.separatorsToSystem(NetconfConfigUtil.getPrivateKeyPath(bundleContext));
        checkState(!Strings.isNullOrEmpty(path), "Path to ssh private key is blank. Reconfigure %s", NetconfConfigUtil.getPrivateKeyKey());
        final String privateKeyPEMString = PEMGenerator.readOrGeneratePK(new File(path));

        final EventLoopGroup bossGroup  = new NioEventLoopGroup();
        final NetconfSSHServer server = NetconfSSHServer.start(sshSocketAddress.getPort(), localAddress, bossGroup, privateKeyPEMString.toCharArray());

        authProviderTracker = new AuthProviderTracker(bundleContext, server);

        return server;
    }

    private static Thread runNetconfSshThread(final NetconfSSHServer server) {
        final Thread serverThread = new Thread(server, "netconf SSH server thread");
        serverThread.setDaemon(true);
        serverThread.start();
        logger.trace("Netconf SSH  bridge up and running.");
        return serverThread;
    }

    private static class AuthProviderTracker implements ServiceTrackerCustomizer<AuthProvider, AuthProvider> {
        private final BundleContext bundleContext;
        private final NetconfSSHServer server;

        private Integer maxPreference;
        private Thread sshThread;
        private final ServiceTracker<AuthProvider, AuthProvider> listenerTracker;

        public AuthProviderTracker(final BundleContext bundleContext, final NetconfSSHServer server) {
            this.bundleContext = bundleContext;
            this.server = server;
            listenerTracker = new ServiceTracker<>(bundleContext, AuthProvider.class, this);
            listenerTracker.open();
        }

        @Override
        public AuthProvider addingService(final ServiceReference<AuthProvider> reference) {
            logger.trace("Service {} added", reference);
            final AuthProvider authService = bundleContext.getService(reference);
            final Integer newServicePreference = getPreference(reference);
            if(isBetter(newServicePreference)) {
                server.setAuthProvider(authService);
                if(sshThread == null) {
                    sshThread = runNetconfSshThread(server);
                }
            }
            return authService;
        }

        private Integer getPreference(final ServiceReference<AuthProvider> reference) {
            final Object preferenceProperty = reference.getProperty(AuthConstants.SERVICE_PREFERENCE_KEY);
            return preferenceProperty == null ? Integer.MIN_VALUE : Integer.valueOf(preferenceProperty.toString());
        }

        private boolean isBetter(final Integer newServicePreference) {
            Preconditions.checkNotNull(newServicePreference);
            if(maxPreference == null) {
                return true;
            }

            return newServicePreference > maxPreference;
        }

        @Override
        public void modifiedService(final ServiceReference<AuthProvider> reference, final AuthProvider service) {
            final AuthProvider authService = bundleContext.getService(reference);
            final Integer newServicePreference = getPreference(reference);
            if(isBetter(newServicePreference)) {
                logger.trace("Replacing modified service {} in netconf SSH.", reference);
                server.setAuthProvider(authService);
            }
        }

        @Override
        public void removedService(final ServiceReference<AuthProvider> reference, final AuthProvider service) {
            logger.trace("Removing service {} from netconf SSH. " +
                    "SSH won't authenticate users until AuthProvider service will be started.", reference);
            maxPreference = null;
            server.setAuthProvider(null);
        }

        public void stop() {
            listenerTracker.close();
            // sshThread should finish normally since sshServer.close stops processing
        }

    }
}
