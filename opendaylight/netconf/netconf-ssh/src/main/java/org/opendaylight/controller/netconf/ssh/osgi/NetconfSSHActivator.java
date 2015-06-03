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
import io.netty.channel.nio.NioEventLoopGroup;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import org.apache.sshd.common.util.ThreadUtils;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.opendaylight.controller.netconf.ssh.SshProxyServer;
import org.opendaylight.controller.netconf.ssh.SshProxyServerConfigurationBuilder;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil.InfixProp;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfSSHActivator implements BundleActivator {
    private static final Logger LOG = LoggerFactory.getLogger(NetconfSSHActivator.class);

    private static final java.lang.String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 4096;
    public static final int POOL_SIZE = 8;
    private static final int DEFAULT_IDLE_TIMEOUT = Integer.MAX_VALUE;

    private static final String SYSTEM_SEPARATOR = File.separator;
    private static final String UNIX_SEPARATOR = "/";
    private static final String WINDOWS_SEPARATOR = "\\\\";

    private ScheduledExecutorService minaTimerExecutor;
    private NioEventLoopGroup clientGroup;
    private ExecutorService nioExecutor;
    private AuthProviderTracker authProviderTracker;

    private SshProxyServer server;

    @Override
    public void start(final BundleContext bundleContext) throws IOException {
        minaTimerExecutor = Executors.newScheduledThreadPool(POOL_SIZE, new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(r, "netconf-ssh-server-mina-timers");
            }
        });
        clientGroup = new NioEventLoopGroup();
        nioExecutor = ThreadUtils.newFixedThreadPool("netconf-ssh-server-nio-group", POOL_SIZE);
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

        if(nioExecutor!=null) {
            nioExecutor.shutdownNow();
        }

        if(clientGroup != null) {
            clientGroup.shutdownGracefully();
        }

        if(minaTimerExecutor != null) {
            minaTimerExecutor.shutdownNow();
        }
    }

    private SshProxyServer startSSHServer(final BundleContext bundleContext) throws IOException {
        final Optional<InetSocketAddress> maybeSshSocketAddress = NetconfConfigUtil.extractNetconfServerAddress(bundleContext, InfixProp.ssh);

        if (!maybeSshSocketAddress.isPresent()) {
            LOG.trace("SSH bridge not configured");
            return null;
        }

        final InetSocketAddress sshSocketAddress = maybeSshSocketAddress.get();
        LOG.trace("Starting netconf SSH bridge at {}", sshSocketAddress);

        final LocalAddress localAddress = NetconfConfigUtil.getNetconfLocalAddress();

        authProviderTracker = new AuthProviderTracker(bundleContext);

        final String path;
        if (WINDOWS_SEPARATOR.equals(SYSTEM_SEPARATOR)) {
            path = NetconfConfigUtil.getPrivateKeyPath(bundleContext).replaceAll(UNIX_SEPARATOR, Matcher.quoteReplacement(File.separator));
        } else {
            path = NetconfConfigUtil.getPrivateKeyPath(bundleContext).replaceAll(WINDOWS_SEPARATOR, Matcher.quoteReplacement(File.separator));
        }

        checkState(!Strings.isNullOrEmpty(path), "Path to ssh private key is blank. Reconfigure %s",
                NetconfConfigUtil.getPrivateKeyKey());

        final SshProxyServer sshProxyServer = new SshProxyServer(minaTimerExecutor, clientGroup, nioExecutor);
        sshProxyServer.bind(
                new SshProxyServerConfigurationBuilder()
                        .setBindingAddress(sshSocketAddress)
                        .setLocalAddress(localAddress)
                        .setAuthenticator(authProviderTracker)
                        .setKeyPairProvider(new PEMGeneratorHostKeyProvider(path, ALGORITHM, KEY_SIZE))
                        .setIdleTimeout(DEFAULT_IDLE_TIMEOUT)
                        .createSshProxyServerConfiguration());
        return sshProxyServer;
    }

}
