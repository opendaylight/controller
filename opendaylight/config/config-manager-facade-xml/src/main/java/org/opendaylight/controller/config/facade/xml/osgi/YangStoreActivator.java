/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.osgi;

import com.google.common.base.Preconditions;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanServer;
import org.opendaylight.controller.config.facade.xml.ConfigSubsystemFacadeFactory;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.repo.spi.SchemaSourceProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start yang store service and the XML config manager facade
 */
public class YangStoreActivator implements BundleActivator {

    private static final Logger LOG = LoggerFactory.getLogger(YangStoreActivator.class);

    private final MBeanServer configMBeanServer = ManagementFactory.getPlatformMBeanServer();

    private ServiceRegistration<YangStoreService> yangStoreServiceServiceRegistration;
    private ConfigRegistryLookupThread configRegistryLookup = null;
    private BundleContext context;
    private ServiceRegistration<ConfigSubsystemFacadeFactory> osgiRegistrayion;

    @Override
    public void start(final BundleContext context) throws Exception {
        LOG.debug("ConfigPersister starting");
        this.context = context;

        final ServiceTrackerCustomizer<SchemaContextProvider, YangStoreService> schemaServiceTrackerCustomizer = new ServiceTrackerCustomizer<SchemaContextProvider, YangStoreService>() {

            private final AtomicBoolean alreadyStarted = new AtomicBoolean(false);

            @Override
            public YangStoreService addingService(final ServiceReference<SchemaContextProvider> reference) {
                LOG.debug("Got addingService(SchemaContextProvider) event");
                if((reference.getProperty(SchemaSourceProvider.class.getName()) == null) &&
                    (reference.getProperty(BindingRuntimeContext.class.getName()) == null)) {
                    LOG.debug("SchemaContextProvider not from config-manager. Ignoring");
                    return null;
                }

                // Yang store service should not be registered multiple times
                if(!this.alreadyStarted.compareAndSet(false, true)) {
                    LOG.warn("Starting yang store service multiple times. Received new service {}", reference);
                    throw new RuntimeException("Starting yang store service multiple times");
                }
                final SchemaContextProvider schemaContextProvider = reference.getBundle().getBundleContext().getService(reference);
                final Object sourceProvider = Preconditions.checkNotNull(
                    reference.getProperty(SchemaSourceProvider.class.getName()), "Source provider not found");
                Preconditions.checkArgument(sourceProvider instanceof SchemaSourceProvider);

                // TODO avoid cast
                final YangStoreService yangStoreService = new YangStoreService(schemaContextProvider,
                    ((SchemaSourceProvider<YangTextSchemaSource>) sourceProvider));

                final BindingRuntimeContext runtimeContext = (BindingRuntimeContext) reference
                        .getProperty(BindingRuntimeContext.class.getName());
                LOG.debug("BindingRuntimeContext retrieved as {}", runtimeContext);
                if(runtimeContext != null) {
                    yangStoreService.refresh(runtimeContext);
                }

                YangStoreActivator.this.yangStoreServiceServiceRegistration = context.registerService(YangStoreService.class, yangStoreService,
                        new Hashtable<>());
                YangStoreActivator.this.configRegistryLookup = new ConfigRegistryLookupThread(yangStoreService);
                YangStoreActivator.this.configRegistryLookup.start();
                return yangStoreService;
            }

            @Override
            public void modifiedService(final ServiceReference<SchemaContextProvider> reference, final YangStoreService service) {
                if (service == null) {
                    return;
                }

                LOG.debug("Got modifiedService(SchemaContextProvider) event");
                final BindingRuntimeContext runtimeContext = (BindingRuntimeContext) reference
                    .getProperty(BindingRuntimeContext.class.getName());
                LOG.debug("BindingRuntimeContext retrieved as {}", runtimeContext);
                service.refresh(runtimeContext);
            }

            @Override
            public void removedService(final ServiceReference<SchemaContextProvider> reference, final YangStoreService service) {
                if(service == null) {
                    return;
                }

                LOG.debug("Got removedService(SchemaContextProvider) event");
                this.alreadyStarted.set(false);
                YangStoreActivator.this.configRegistryLookup.interrupt();
                YangStoreActivator.this.yangStoreServiceServiceRegistration.unregister();
                YangStoreActivator.this.yangStoreServiceServiceRegistration = null;
            }
        };

        final ServiceTracker<SchemaContextProvider, YangStoreService> schemaContextProviderServiceTracker =
                new ServiceTracker<>(context, SchemaContextProvider.class, schemaServiceTrackerCustomizer);
        schemaContextProviderServiceTracker.open();
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        if(this.configRegistryLookup != null) {
            this.configRegistryLookup.interrupt();
        }
        if(this.osgiRegistrayion != null) {
            this.osgiRegistrayion.unregister();
        }
        if (this.yangStoreServiceServiceRegistration != null) {
            this.yangStoreServiceServiceRegistration.unregister();
            this.yangStoreServiceServiceRegistration = null;
        }
    }

    /**
     * Find ConfigRegistry from config manager in JMX
     */
    private class ConfigRegistryLookupThread extends Thread {
        public static final int ATTEMPT_TIMEOUT_MS = 1000;
        private static final int SILENT_ATTEMPTS = 30;

        private final YangStoreService yangStoreService;

        private ConfigRegistryLookupThread(final YangStoreService yangStoreService) {
            super("config-registry-lookup");
            this.yangStoreService = yangStoreService;
        }

        @Override
        public void run() {

            ConfigRegistryJMXClient configRegistryJMXClient;
            ConfigRegistryJMXClient configRegistryJMXClientNoNotifications;
            int i = 0;
            // Config registry might not be present yet, but will be eventually
            while(true) {

                try {
                    configRegistryJMXClient = new ConfigRegistryJMXClient(YangStoreActivator.this.configMBeanServer);
                    configRegistryJMXClientNoNotifications = ConfigRegistryJMXClient.createWithoutNotifications(YangStoreActivator.this.configMBeanServer);
                    break;
                } catch (final IllegalStateException e) {
                    ++i;
                    if (i > SILENT_ATTEMPTS) {
                        LOG.info("JMX client not created after {} attempts, still trying", i, e);
                    } else {
                        LOG.debug("JMX client could not be created, reattempting, try {}", i, e);
                    }
                    try {
                        Thread.sleep(ATTEMPT_TIMEOUT_MS);
                    } catch (final InterruptedException e1) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Interrupted while reattempting connection", e1);
                    }
                }
            }

            final ConfigRegistryJMXClient jmxClient = configRegistryJMXClient;
            final ConfigRegistryJMXClient jmxClientNoNotifications = configRegistryJMXClientNoNotifications;
            if (i > SILENT_ATTEMPTS) {
                LOG.info("Created JMX client after {} attempts", i);
            } else {
                LOG.debug("Created JMX client after {} attempts", i);
            }

            final ConfigSubsystemFacadeFactory configSubsystemFacade =
                    new ConfigSubsystemFacadeFactory(jmxClient, jmxClientNoNotifications, this.yangStoreService);
            YangStoreActivator.this.osgiRegistrayion = YangStoreActivator.this.context.registerService(ConfigSubsystemFacadeFactory.class, configSubsystemFacade,
                    new Hashtable<>());
        }
    }
}

