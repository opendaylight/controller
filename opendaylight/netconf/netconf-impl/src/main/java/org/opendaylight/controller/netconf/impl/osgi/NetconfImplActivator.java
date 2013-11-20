/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.osgi;

import com.google.common.base.Optional;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionListenerFactory;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil.TLSConfiguration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;

public class NetconfImplActivator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(NetconfImplActivator.class);

    private Optional<InetSocketAddress> maybeTCPAddress;
    private Optional<TLSConfiguration> maybeTLSConfiguration;

    private NetconfOperationServiceFactoryTracker factoriesTracker;
    private DefaultCommitNotificationProducer commitNot;
    private NetconfServerDispatcher dispatch;
    private NioEventLoopGroup eventLoopGroup;
    private HashedWheelTimer timer;

    @Override
    public void start(final BundleContext context) throws Exception {
        maybeTCPAddress = NetconfConfigUtil.extractTCPNetconfAddress(context);
        maybeTLSConfiguration = NetconfConfigUtil.extractTLSConfiguration(context);
        if (maybeTCPAddress.isPresent() == false && maybeTLSConfiguration.isPresent() == false) {
            throw new IllegalStateException("TCP nor TLS is configured, netconf not available.");
        }
        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesTracker = new NetconfOperationServiceFactoryTracker(context, factoriesListener);
        factoriesTracker.open();

        SessionIdProvider idProvider = new SessionIdProvider();
        timer = new HashedWheelTimer();
        NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                timer, factoriesListener, idProvider);

        commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        NetconfServerSessionListenerFactory listenerFactory = new NetconfServerSessionListenerFactory(
                factoriesListener, commitNot, idProvider);

        eventLoopGroup = new NioEventLoopGroup();

        if (maybeTCPAddress.isPresent()) {
            Optional<SSLContext> maybeSSLContext = Optional.absent();
            InetSocketAddress address = maybeTCPAddress.get();
            NetconfServerDispatcher.ServerSslChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerSslChannelInitializer(
                    maybeSSLContext, serverNegotiatorFactory, listenerFactory);
            dispatch = new NetconfServerDispatcher(serverChannelInitializer, eventLoopGroup, eventLoopGroup);

            logger.info("Starting TCP netconf server at {}", address);
            dispatch.createServer(address);
        }
        if (maybeTLSConfiguration.isPresent()) {
            Optional<SSLContext> maybeSSLContext = Optional.of(maybeTLSConfiguration.get().getSslContext());
            InetSocketAddress address = maybeTLSConfiguration.get().getAddress();
            NetconfServerDispatcher.ServerSslChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerSslChannelInitializer(
                    maybeSSLContext, serverNegotiatorFactory, listenerFactory);
            dispatch = new NetconfServerDispatcher(serverChannelInitializer, eventLoopGroup, eventLoopGroup);

            logger.info("Starting TLS netconf server at {}", address);
            dispatch.createServer(address);
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        logger.info("Shutting down netconf because YangStoreService service was removed");

        commitNot.close();
        eventLoopGroup.shutdownGracefully();
        timer.stop();
    }
}
