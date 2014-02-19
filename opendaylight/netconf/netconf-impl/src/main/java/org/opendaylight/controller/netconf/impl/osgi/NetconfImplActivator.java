/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.osgi;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionListenerFactory;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

public class NetconfImplActivator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(NetconfImplActivator.class);

    private NetconfOperationServiceFactoryTracker factoriesTracker;
    private DefaultCommitNotificationProducer commitNot;
    private NetconfServerDispatcher dispatch;
    private NioEventLoopGroup eventLoopGroup;
    private HashedWheelTimer timer;
    private ServiceRegistration<NetconfMonitoringService> regMonitoring;

    @Override
    public void start(final BundleContext context) throws Exception {
        InetSocketAddress address = NetconfConfigUtil.extractTCPNetconfAddress(context,
                "TCP is not configured, netconf not available.", false);

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        startOperationServiceFactoryTracker(context, factoriesListener);

        SessionIdProvider idProvider = new SessionIdProvider();
        timer = new HashedWheelTimer();
        long connectionTimeoutMillis = NetconfConfigUtil.extractTimeoutMillis(context);
        NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                timer, factoriesListener, idProvider, connectionTimeoutMillis);

        commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        NetconfMonitoringServiceImpl monitoringService = startMonitoringService(context, factoriesListener);

        NetconfServerSessionListenerFactory listenerFactory = new NetconfServerSessionListenerFactory(
                factoriesListener, commitNot, idProvider, monitoringService);

        eventLoopGroup = new NioEventLoopGroup();

        NetconfServerDispatcher.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerChannelInitializer(
                serverNegotiatorFactory, listenerFactory);
        dispatch = new NetconfServerDispatcher(serverChannelInitializer, eventLoopGroup, eventLoopGroup);

        logger.info("Starting TCP netconf server at {}", address);
        dispatch.createServer(address);

    }

    private void startOperationServiceFactoryTracker(BundleContext context, NetconfOperationServiceFactoryListenerImpl factoriesListener) {
        factoriesTracker = new NetconfOperationServiceFactoryTracker(context, factoriesListener);
        factoriesTracker.open();
    }

    private NetconfMonitoringServiceImpl startMonitoringService(BundleContext context, NetconfOperationServiceFactoryListenerImpl factoriesListener) {
        NetconfMonitoringServiceImpl netconfMonitoringServiceImpl = new NetconfMonitoringServiceImpl(factoriesListener);
        Dictionary<String, ?> dic = new Hashtable<>();
        regMonitoring = context.registerService(NetconfMonitoringService.class, netconfMonitoringServiceImpl, dic);

        return netconfMonitoringServiceImpl;
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        logger.info("Shutting down netconf because YangStoreService service was removed");

        commitNot.close();
        eventLoopGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
        timer.stop();

        regMonitoring.unregister();
        factoriesTracker.close();
    }
}
