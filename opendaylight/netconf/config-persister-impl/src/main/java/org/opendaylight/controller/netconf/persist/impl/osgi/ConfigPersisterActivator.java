/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl.osgi;

import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.persist.impl.ConfigPersisterNotificationHandler;
import org.opendaylight.controller.netconf.persist.impl.ConfigPusher;
import org.opendaylight.controller.netconf.persist.impl.PersisterAggregator;
import org.opendaylight.controller.netconf.util.CloseableUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConfigPersisterActivator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigPersisterActivator.class);
    private static final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();

    public static final String MAX_WAIT_FOR_CAPABILITIES_MILLIS_PROPERTY = "maxWaitForCapabilitiesMillis";
    private static final long MAX_WAIT_FOR_CAPABILITIES_MILLIS_DEFAULT = TimeUnit.MINUTES.toMillis(2);
    public static final String CONFLICTING_VERSION_TIMEOUT_MILLIS_PROPERTY = "conflictingVersionTimeoutMillis";
    private static final long CONFLICTING_VERSION_TIMEOUT_MILLIS_DEFAULT = TimeUnit.SECONDS.toMillis(30);

    public static final String NETCONF_CONFIG_PERSISTER = "netconf.config.persister";

    public static final String STORAGE_ADAPTER_CLASS_PROP_SUFFIX = "storageAdapterClass";

    private List<AutoCloseable> autoCloseables;


    @Override
    public void start(final BundleContext context) throws Exception {
        logger.debug("ConfigPersister starting");
        autoCloseables = new ArrayList<>();
        PropertiesProviderBaseImpl propertiesProvider = new PropertiesProviderBaseImpl(context);

        final PersisterAggregator persisterAggregator = PersisterAggregator.createFromProperties(propertiesProvider);
        autoCloseables.add(persisterAggregator);
        long maxWaitForCapabilitiesMillis = getMaxWaitForCapabilitiesMillis(propertiesProvider);
        List<ConfigSnapshotHolder> configs = persisterAggregator.loadLastConfigs();
        long conflictingVersionTimeoutMillis = getConflictingVersionTimeoutMillis(propertiesProvider);
        logger.trace("Following configs will be pushed: {}", configs);

        InnerCustomizer innerCustomizer = new InnerCustomizer(configs, maxWaitForCapabilitiesMillis,
                conflictingVersionTimeoutMillis, persisterAggregator);
        OuterCustomizer outerCustomizer = new OuterCustomizer(context, innerCustomizer);
        new ServiceTracker<>(context, NetconfOperationProvider.class, outerCustomizer).open();
    }

    private long getConflictingVersionTimeoutMillis(PropertiesProviderBaseImpl propertiesProvider) {
        String timeoutProperty = propertiesProvider.getProperty(CONFLICTING_VERSION_TIMEOUT_MILLIS_PROPERTY);
        return timeoutProperty == null ? CONFLICTING_VERSION_TIMEOUT_MILLIS_DEFAULT : Long.valueOf(timeoutProperty);
    }

    private long getMaxWaitForCapabilitiesMillis(PropertiesProviderBaseImpl propertiesProvider) {
        String timeoutProperty = propertiesProvider.getProperty(MAX_WAIT_FOR_CAPABILITIES_MILLIS_PROPERTY);
        return timeoutProperty == null ? MAX_WAIT_FOR_CAPABILITIES_MILLIS_DEFAULT : Long.valueOf(timeoutProperty);
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        CloseableUtil.closeAll(autoCloseables);
    }


    @VisibleForTesting
    public static String getFilterString() {
        return "(&" +
                "(" + Constants.OBJECTCLASS + "=" + NetconfOperationServiceFactory.class.getName() + ")" +
                "(name" + "=" + "config-netconf-connector" + ")" +
                ")";
    }

    class OuterCustomizer implements ServiceTrackerCustomizer<NetconfOperationProvider, NetconfOperationProvider> {
        private final BundleContext context;
        private final InnerCustomizer innerCustomizer;

        OuterCustomizer(BundleContext context, InnerCustomizer innerCustomizer) {
            this.context = context;
            this.innerCustomizer = innerCustomizer;
        }

        @Override
        public NetconfOperationProvider addingService(ServiceReference<NetconfOperationProvider> reference) {
            logger.trace("Got OuterCustomizer.addingService {}", reference);
            // JMX was registered, track config-netconf-connector
            Filter filter;
            try {
                filter = context.createFilter(getFilterString());
            } catch (InvalidSyntaxException e) {
                throw new IllegalStateException(e);
            }
            new ServiceTracker<>(context, filter, innerCustomizer).open();
            return null;
        }

        @Override
        public void modifiedService(ServiceReference<NetconfOperationProvider> reference, NetconfOperationProvider service) {

        }

        @Override
        public void removedService(ServiceReference<NetconfOperationProvider> reference, NetconfOperationProvider service) {

        }
    }

    class InnerCustomizer implements ServiceTrackerCustomizer<NetconfOperationServiceFactory, NetconfOperationServiceFactory> {
        private final List<ConfigSnapshotHolder> configs;
        private final PersisterAggregator persisterAggregator;
        private final long maxWaitForCapabilitiesMillis, conflictingVersionTimeoutMillis;


        InnerCustomizer(List<ConfigSnapshotHolder> configs, long maxWaitForCapabilitiesMillis, long conflictingVersionTimeoutMillis,
                        PersisterAggregator persisterAggregator) {
            this.configs = configs;
            this.maxWaitForCapabilitiesMillis = maxWaitForCapabilitiesMillis;
            this.conflictingVersionTimeoutMillis = conflictingVersionTimeoutMillis;
            this.persisterAggregator = persisterAggregator;
        }

        @Override
        public NetconfOperationServiceFactory addingService(ServiceReference<NetconfOperationServiceFactory> reference) {
            logger.trace("Got InnerCustomizer.addingService {}", reference);
            NetconfOperationServiceFactory service = reference.getBundle().getBundleContext().getService(reference);

            final ConfigPusher configPusher = new ConfigPusher(service, maxWaitForCapabilitiesMillis, conflictingVersionTimeoutMillis);
            logger.debug("Configuration Persister got {}", service);
            final Thread pushingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    configPusher.pushConfigs(configs);
                    logger.info("Configuration Persister initialization completed.");
                    ConfigPersisterNotificationHandler jmxNotificationHandler = new ConfigPersisterNotificationHandler(platformMBeanServer, persisterAggregator);
                    synchronized (ConfigPersisterActivator.this) {
                        autoCloseables.add(jmxNotificationHandler);
                    }
                }
            }, "config-pusher");
            synchronized (ConfigPersisterActivator.this) {
                autoCloseables.add(new AutoCloseable() {
                    @Override
                    public void close() throws Exception {
                        pushingThread.interrupt();
                    }
                });
            }
            pushingThread.start();
            return service;
        }

        @Override
        public void modifiedService(ServiceReference<NetconfOperationServiceFactory> reference, NetconfOperationServiceFactory service) {
        }

        @Override
        public void removedService(ServiceReference<NetconfOperationServiceFactory> reference, NetconfOperationServiceFactory service) {
        }

    }
}

