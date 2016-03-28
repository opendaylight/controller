/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.persist.impl.osgi;

import com.google.common.collect.Lists;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServer;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacadeFactory;
import org.opendaylight.controller.config.persist.api.ConfigPusher;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.persist.impl.ConfigPusherImpl;
import org.opendaylight.controller.config.persist.impl.PersisterAggregator;
import org.opendaylight.controller.config.util.CloseableUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
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

    private final List<AutoCloseable> autoCloseables = Lists.newArrayList();
    private volatile BundleContext context;

    ServiceRegistration<?> registration;

    @Override
    public void start(final BundleContext context) throws Exception {
        LOG.debug("ConfigPersister starting");
        this.context = context;

        PropertiesProviderBaseImpl propertiesProvider = new PropertiesProviderBaseImpl(context);

        final PersisterAggregator persisterAggregator = PersisterAggregator.createFromProperties(propertiesProvider);
        autoCloseables.add(persisterAggregator);
        final long maxWaitForCapabilitiesMillis = getMaxWaitForCapabilitiesMillis(propertiesProvider);
        final List<ConfigSnapshotHolder> configs = persisterAggregator.loadLastConfigs();
        final long conflictingVersionTimeoutMillis = getConflictingVersionTimeoutMillis(propertiesProvider);
        LOG.debug("Following configs will be pushed: {}", configs);

        ServiceTrackerCustomizer<ConfigSubsystemFacadeFactory, ConfigSubsystemFacadeFactory> schemaServiceTrackerCustomizer = new ServiceTrackerCustomizer<ConfigSubsystemFacadeFactory, ConfigSubsystemFacadeFactory>() {

            @Override
            public ConfigSubsystemFacadeFactory addingService(ServiceReference<ConfigSubsystemFacadeFactory> reference) {
                LOG.debug("Got addingService(SchemaContextProvider) event");
                // Yang store service should not be registered multiple times
                ConfigSubsystemFacadeFactory ConfigSubsystemFacadeFactory = reference.getBundle().getBundleContext().getService(reference);
                startPusherThread(configs, maxWaitForCapabilitiesMillis, ConfigSubsystemFacadeFactory, conflictingVersionTimeoutMillis, persisterAggregator);
                return ConfigSubsystemFacadeFactory;
            }

            @Override
            public void modifiedService(ServiceReference<ConfigSubsystemFacadeFactory> reference, ConfigSubsystemFacadeFactory service) {
                LOG.warn("Config manager facade was modified unexpectedly");
            }

            @Override
            public void removedService(ServiceReference<ConfigSubsystemFacadeFactory> reference, ConfigSubsystemFacadeFactory service) {
                LOG.warn("Config manager facade was removed unexpectedly");
            }
        };

        ServiceTracker<ConfigSubsystemFacadeFactory, ConfigSubsystemFacadeFactory> schemaContextProviderServiceTracker =
                new ServiceTracker<>(context, ConfigSubsystemFacadeFactory.class, schemaServiceTrackerCustomizer);
        schemaContextProviderServiceTracker.open();
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
            autoCloseables.clear();
            if (registration != null) {
                registration.unregister();
            }
            this.context = null;
        }
    }

    private void startPusherThread(final List<? extends ConfigSnapshotHolder> configs, final long maxWaitForCapabilitiesMillis,
                           final ConfigSubsystemFacadeFactory service, final long conflictingVersionTimeoutMillis, final Persister persisterAggregator){
        LOG.debug("Creating new job queue");
        final ConfigPusherImpl configPusher = new ConfigPusherImpl(service,
                maxWaitForCapabilitiesMillis, conflictingVersionTimeoutMillis);
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
                            configPusher.process(autoCloseables, platformMBeanServer, persisterAggregator, false);
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
        pushingThread.setDaemon(true);
        pushingThread.start();
    }
}

