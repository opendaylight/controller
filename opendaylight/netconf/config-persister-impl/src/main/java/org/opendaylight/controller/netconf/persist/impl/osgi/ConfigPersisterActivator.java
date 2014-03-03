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
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.persist.impl.ConfigPersisterNotificationHandler;
import org.opendaylight.controller.netconf.persist.impl.ConfigPusher;
import org.opendaylight.controller.netconf.persist.impl.PersisterAggregator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
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

    public static final String MAX_WAIT_FOR_CAPABILITIES_MILLIS_PROPERTY = "maxWaitForCapabilitiesMillis";
    private static final long MAX_WAIT_FOR_CAPABILITIES_MILLIS_DEFAULT = TimeUnit.MINUTES.toMillis(2);
    public static final String CONFLICTING_VERSION_TIMEOUT_MILLIS_PROPERTY = "conflictingVersionTimeoutMillis";
    private static final long CONFLICTING_VERSION_TIMEOUT_MILLIS_DEFAULT = TimeUnit.SECONDS.toMillis(30);

    public static final String NETCONF_CONFIG_PERSISTER = "netconf.config.persister";

    public static final String STORAGE_ADAPTER_CLASS_PROP_SUFFIX = "storageAdapterClass";


    private static final MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();

    private List<AutoCloseable> autoCloseables;


    @Override
    public void start(final BundleContext context) throws Exception {
        logger.debug("ConfigPersister starting");
        autoCloseables = new ArrayList<>();
        PropertiesProviderBaseImpl propertiesProvider = new PropertiesProviderBaseImpl(context);


        final PersisterAggregator persisterAggregator = PersisterAggregator.createFromProperties(propertiesProvider);
        autoCloseables.add(persisterAggregator);
        final long maxWaitForCapabilitiesMillis = getMaxWaitForCapabilitiesMillis(propertiesProvider);
        final List<ConfigSnapshotHolder> configs = persisterAggregator.loadLastConfigs();
        final long conflictingVersionTimeoutMillis = getConflictingVersionTimeoutMillis(propertiesProvider);
        logger.trace("Following configs will be pushed: {}", configs);
        ServiceTrackerCustomizer<NetconfOperationServiceFactory, NetconfOperationServiceFactory> configNetconfCustomizer = new ServiceTrackerCustomizer<NetconfOperationServiceFactory, NetconfOperationServiceFactory>() {
            @Override
            public NetconfOperationServiceFactory addingService(ServiceReference<NetconfOperationServiceFactory> reference) {
                NetconfOperationServiceFactory service = reference.getBundle().getBundleContext().getService(reference);
                final ConfigPusher configPusher = new ConfigPusher(service, maxWaitForCapabilitiesMillis, conflictingVersionTimeoutMillis);
                logger.debug("Configuration Persister got %s", service);
                final Thread pushingThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            configPusher.pushConfigs(configs);
                            logger.info("Configuration Persister initialization completed.");
                            ConfigPersisterNotificationHandler jmxNotificationHandler = new ConfigPersisterNotificationHandler(platformMBeanServer, persisterAggregator);
                            synchronized (ConfigPersisterActivator.this) {
                                autoCloseables.add(jmxNotificationHandler);
                            }
                        } catch (NetconfDocumentedException e) {
                            logger.error("Configurations push failure due to {}",e);
                        }
                    }
                }, "config-pusher");
                synchronized (ConfigPersisterActivator.this){
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
        };

        Filter filter = context.createFilter(getFilterString());

        ServiceTracker<NetconfOperationServiceFactory, NetconfOperationServiceFactory> tracker =
                new ServiceTracker<>(context, filter, configNetconfCustomizer);
        tracker.open();
    }


    @VisibleForTesting
    public static String getFilterString() {
        return "(&" +
                "(" + Constants.OBJECTCLASS + "=" + NetconfOperationServiceFactory.class.getName() + ")" +
                "(name" + "=" + "config-netconf-connector" + ")" +
                ")";
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
        Exception lastException = null;
        for (AutoCloseable autoCloseable : autoCloseables) {
            try {
                autoCloseable.close();
            } catch (Exception e) {
                if (lastException == null) {
                    lastException = e;
                } else {
                    lastException.addSuppressed(e);
                }
            }
        }
        if (lastException != null) {
            throw lastException;
        }
    }
}
