/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import java.lang.management.ManagementFactory;
import java.util.Collection;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;

import org.opendaylight.controller.config.manager.impl.ConfigRegistryImpl;
import org.opendaylight.controller.config.manager.impl.jmx.ConfigRegistryJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.ModuleInfoBundleTracker;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.RuntimeGeneratedMappingServiceActivator;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManagerActivator implements BundleActivator {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManagerActivator.class);

    private ExtensibleBundleTracker<Collection<Registration<YangModuleInfo>>> bundleTracker;
    private ConfigRegistryImpl configRegistry;
    private ConfigRegistryJMXRegistrator configRegistryJMXRegistrator;
    private ServiceRegistration configRegistryServiceRegistration;

    private final MBeanServer configMBeanServer = ManagementFactory.getPlatformMBeanServer();

    private RuntimeGeneratedMappingServiceActivator mappingServiceActivator;

    @Override
    public void start(BundleContext context) {

        // track bundles containing YangModuleInfo
        ModuleInfoBundleTracker moduleInfoBundleTracker = new ModuleInfoBundleTracker();
        mappingServiceActivator = new RuntimeGeneratedMappingServiceActivator(moduleInfoBundleTracker);
        CodecRegistry codecRegistry = mappingServiceActivator.startRuntimeMappingService(context).getCodecRegistry();

        // start config registry
        BundleContextBackedModuleFactoriesResolver bundleContextBackedModuleFactoriesResolver = new BundleContextBackedModuleFactoriesResolver(
                context);
        configRegistry = new ConfigRegistryImpl(bundleContextBackedModuleFactoriesResolver, configMBeanServer,
                codecRegistry);

        // track bundles containing factories
        BlankTransactionServiceTracker blankTransactionServiceTracker = new BlankTransactionServiceTracker(
                configRegistry);
        ModuleFactoryBundleTracker moduleFactoryBundleTracker = new ModuleFactoryBundleTracker(
                blankTransactionServiceTracker);

        // start extensible tracker
        bundleTracker = new ExtensibleBundleTracker<>(context, moduleInfoBundleTracker, moduleFactoryBundleTracker);
        bundleTracker.open();

        // register config registry to OSGi
        configRegistryServiceRegistration = context.registerService(ConfigRegistryImpl.class, configRegistry, null);

        // register config registry to jmx
        configRegistryJMXRegistrator = new ConfigRegistryJMXRegistrator(configMBeanServer);
        try {
            configRegistryJMXRegistrator.registerToJMX(configRegistry);
        } catch (InstanceAlreadyExistsException e) {
            throw new IllegalStateException("Config Registry was already registered to JMX", e);
        }

        ServiceTracker<ModuleFactory, Object> serviceTracker = new ServiceTracker<>(context, ModuleFactory.class,
                blankTransactionServiceTracker);
        serviceTracker.open();
    }

    @Override
    public void stop(BundleContext context) {
        try {
            configRegistry.close();
        } catch (Exception e) {
            logger.warn("Exception while closing config registry", e);
        }
        try {
            bundleTracker.close();
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
        try {
            mappingServiceActivator.close();
        } catch (Exception e) {
            logger.warn("Exception while closing mapping service", e);
        }
    }
}
