/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import org.opendaylight.controller.config.manager.impl.ConfigRegistryImpl;
import org.opendaylight.controller.config.manager.impl.jmx.ConfigRegistryJMXRegistrator;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

public class ConfigManagerActivator implements BundleActivator {
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigManagerActivator.class);

    private ExtenderBundleTracker extenderBundleTracker;
    private ConfigRegistryImpl configRegistry;
    private ConfigRegistryJMXRegistrator configRegistryJMXRegistrator;
    private ServiceRegistration configRegistryServiceRegistration;

    private ServiceTracker<BindingIndependentMappingService, BindingIndependentMappingService> tracker;

    @Override
    public void start(BundleContext context) {
        BindingIndependentMappingServiceTracker mappingServiceTracker = new BindingIndependentMappingServiceTracker(
                context, this);
        tracker = new ServiceTracker<>(
                context, BindingIndependentMappingService.class, mappingServiceTracker);

        logger.debug("Waiting for codec registry");

        tracker.open();
    }

    void initConfigManager(BundleContext context, CodecRegistry codecRegistry) {
        BundleContextBackedModuleFactoriesResolver bundleContextBackedModuleFactoriesResolver =
                new BundleContextBackedModuleFactoriesResolver(context);
        MBeanServer configMBeanServer = ManagementFactory.getPlatformMBeanServer();


        // TODO push codecRegistry/IdentityCodec to dependencyResolver

        configRegistry = new ConfigRegistryImpl(
                bundleContextBackedModuleFactoriesResolver, configMBeanServer, codecRegistry);

        // register config registry to OSGi
        configRegistryServiceRegistration = context.registerService(ConfigRegistryImpl.class, configRegistry, null);

        // register config registry to jmx
        configRegistryJMXRegistrator = new ConfigRegistryJMXRegistrator(configMBeanServer);
        try {
            configRegistryJMXRegistrator.registerToJMX(configRegistry);
        } catch (InstanceAlreadyExistsException e) {
            throw new IllegalStateException("Config Registry was already registered to JMX", e);
        }

        // track bundles containing factories
        BlankTransactionServiceTracker blankTransactionServiceTracker = new BlankTransactionServiceTracker(configRegistry);
        extenderBundleTracker = new ExtenderBundleTracker(context, blankTransactionServiceTracker);
        extenderBundleTracker.open();

        ServiceTracker<?, ?> serviceTracker = new ServiceTracker(context, ModuleFactory.class, blankTransactionServiceTracker);
        serviceTracker.open();
    }

    @Override
    public void stop(BundleContext context) {
        try {
            tracker.close();
        } catch (Exception e) {
            logger.warn("Exception while closing tracker", e);
        }
        try {
            configRegistry.close();
        } catch (Exception e) {
            logger.warn("Exception while closing config registry", e);
        }
        try {
            extenderBundleTracker.close();
        } catch (Exception e) {
            logger.warn("Exception while closing extender", e);
        }
        try {
            configRegistryJMXRegistrator.close();
        } catch (Exception e) {
            logger.warn(
                    "Exception while closing config registry jmx registrator",
                    e);
        }
        try {
            configRegistryServiceRegistration.unregister();
        } catch (Exception e) {
            logger.warn("Exception while unregistering config registry", e);
        }
    }
}
