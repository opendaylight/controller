/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl.osgi;

import com.google.common.annotations.VisibleForTesting;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.persist.impl.ConfigPusherImpl;
import org.opendaylight.controller.netconf.persist.impl.PersisterAggregator;
import org.opendaylight.controller.netconf.util.CloseableUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigPersisterActivator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigPersisterActivator.class);
    private static final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();

    public static final String MAX_WAIT_FOR_CAPABILITIES_MILLIS_PROPERTY = "maxWaitForCapabilitiesMillis";
    private static final long MAX_WAIT_FOR_CAPABILITIES_MILLIS_DEFAULT = TimeUnit.MINUTES.toMillis(2);
    public static final String CONFLICTING_VERSION_TIMEOUT_MILLIS_PROPERTY = "conflictingVersionTimeoutMillis";
    private static final long CONFLICTING_VERSION_TIMEOUT_MILLIS_DEFAULT = TimeUnit.MINUTES.toMillis(1);

    public static final String NETCONF_CONFIG_PERSISTER = "netconf.config.persister";

    public static final String STORAGE_ADAPTER_CLASS_PROP_SUFFIX = "storageAdapterClass";

    private List<AutoCloseable> autoCloseables;
    private volatile BundleContext context;

    ServiceRegistration<?> registration;

    @Override
    public void start(final BundleContext context) throws Exception {
        LOG.debug("ConfigPersister starting");
        this.context = context;

        autoCloseables = new ArrayList<>();
        PropertiesProviderBaseImpl propertiesProvider = new PropertiesProviderBaseImpl(context);

        final PersisterAggregator persisterAggregator = PersisterAggregator.createFromProperties(propertiesProvider);
        autoCloseables.add(persisterAggregator);
        long maxWaitForCapabilitiesMillis = getMaxWaitForCapabilitiesMillis(propertiesProvider);
        List<ConfigSnapshotHolder> configs = persisterAggregator.loadLastConfigs();
        long conflictingVersionTimeoutMillis = getConflictingVersionTimeoutMillis(propertiesProvider);
        LOG.debug("Following configs will be pushed: {}", configs);

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
    public void stop(BundleContext context) throws Exception {
        synchronized(autoCloseables) {
            CloseableUtil.closeAll(autoCloseables);
            if (registration != null) {
                registration.unregister();
            }
            this.context = null;
        }
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
            LOG.trace("Got OuterCustomizer.addingService {}", reference);
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
            LOG.trace("Got InnerCustomizer.addingService {}", reference);
            NetconfOperationServiceFactory service = reference.getBundle().getBundleContext().getService(reference);

            LOG.debug("Creating new job queue");

            final ConfigPusherImpl configPusher = new ConfigPusherImpl(service, maxWaitForCapabilitiesMillis, conflictingVersionTimeoutMillis);
            LOG.debug("Configuration Persister got {}", service);
            LOG.debug("Context was {}", context);
            LOG.debug("Registration was {}", registration);

            final Thread pushingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(configs != null && !configs.isEmpty()) {
                            configPusher.pushConfigs(configs);
                        }
                        if(context != null) {
                            registration = context.registerService(ConfigPusher.class.getName(), configPusher, null);
                            configPusher.process(autoCloseables, platformMBeanServer, persisterAggregator);
                        } else {
                            LOG.warn("Unable to process configs as BundleContext is null");
                        }
                    } catch (InterruptedException e) {
                        LOG.info("ConfigPusher thread stopped",e);
                    }
                    LOG.info("Configuration Persister initialization completed.");
                }
            }, "config-pusher");
            synchronized (autoCloseables) {
                autoCloseables.add(new AutoCloseable() {
                    @Override
                    public void close() {
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

