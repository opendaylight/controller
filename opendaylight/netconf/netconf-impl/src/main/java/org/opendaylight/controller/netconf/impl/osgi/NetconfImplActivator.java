/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.impl.osgi;

import com.google.common.base.Preconditions;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcherImpl;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactoryListener;
import org.opendaylight.controller.netconf.notifications.BaseNotificationPublisherRegistration;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationCollector;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfImplActivator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfImplActivator.class);

    private NetconfOperationServiceFactoryTracker factoriesTracker;
    private NioEventLoopGroup eventLoopGroup;
    private HashedWheelTimer timer;
    private ServiceRegistration<NetconfMonitoringService> regMonitoring;

    private BaseNotificationPublisherRegistration listenerReg;

    @Override
    public void start(final BundleContext context)  {
        try {
            AggregatedNetconfOperationServiceFactory factoriesListener = new AggregatedNetconfOperationServiceFactory();
            startOperationServiceFactoryTracker(context, factoriesListener);

            SessionIdProvider idProvider = new SessionIdProvider();
            timer = new HashedWheelTimer();
            long connectionTimeoutMillis = NetconfConfigUtil.extractTimeoutMillis(context);

            final NetconfMonitoringServiceImpl monitoringService = startMonitoringService(context, factoriesListener);

            NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                    timer, factoriesListener, idProvider, connectionTimeoutMillis, monitoringService);

            eventLoopGroup = new NioEventLoopGroup();

            NetconfServerDispatcherImpl.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcherImpl.ServerChannelInitializer(
                    serverNegotiatorFactory);
            NetconfServerDispatcherImpl dispatch = new NetconfServerDispatcherImpl(serverChannelInitializer, eventLoopGroup, eventLoopGroup);

            LocalAddress address = NetconfConfigUtil.getNetconfLocalAddress();
            LOG.trace("Starting local netconf server at {}", address);
            dispatch.createLocalServer(address);

            final ServiceTracker<NetconfNotificationCollector, NetconfNotificationCollector> notificationServiceTracker =
                    new ServiceTracker<>(context, NetconfNotificationCollector.class, new ServiceTrackerCustomizer<NetconfNotificationCollector, NetconfNotificationCollector>() {
                        @Override
                        public NetconfNotificationCollector addingService(ServiceReference<NetconfNotificationCollector> reference) {
                            Preconditions.checkState(listenerReg != null, "Notification collector service was already added");
                            listenerReg = context.getService(reference).registerBaseNotificationPublisher();
                            monitoringService.setNotificationPublisher(listenerReg);
                            return null;
                        }

                        @Override
                        public void modifiedService(ServiceReference<NetconfNotificationCollector> reference, NetconfNotificationCollector service) {

                        }

                        @Override
                        public void removedService(ServiceReference<NetconfNotificationCollector> reference, NetconfNotificationCollector service) {
                            listenerReg.close();
                            listenerReg = null;
                            monitoringService.setNotificationPublisher(listenerReg);
                        }
                    });
            notificationServiceTracker.open();
        } catch (Exception e) {
            LOG.warn("Unable to start NetconfImplActivator", e);
        }
    }

    private void startOperationServiceFactoryTracker(BundleContext context, NetconfOperationServiceFactoryListener factoriesListener) {
        factoriesTracker = new NetconfOperationServiceFactoryTracker(context, factoriesListener);
        factoriesTracker.open();
    }

    private NetconfMonitoringServiceImpl startMonitoringService(BundleContext context, AggregatedNetconfOperationServiceFactory factoriesListener) {
        NetconfMonitoringServiceImpl netconfMonitoringServiceImpl = new NetconfMonitoringServiceImpl(factoriesListener);
        Dictionary<String, ?> dic = new Hashtable<>();
        regMonitoring = context.registerService(NetconfMonitoringService.class, netconfMonitoringServiceImpl, dic);

        return netconfMonitoringServiceImpl;
    }

    @Override
    public void stop(final BundleContext context) {
        LOG.info("Shutting down netconf because YangStoreService service was removed");

        eventLoopGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
        timer.stop();

        regMonitoring.unregister();
        factoriesTracker.close();
    }
}
