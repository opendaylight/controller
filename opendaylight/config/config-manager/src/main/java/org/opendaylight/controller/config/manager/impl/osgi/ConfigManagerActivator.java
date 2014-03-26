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
import org.opendaylight.controller.config.manager.impl.osgi.mapping.CodecRegistryProvider;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.ModuleInfoBundleTracker;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.RefreshingSCPModuleInfoRegistry;
import org.opendaylight.controller.config.manager.impl.util.OsgiRegistrationUtil;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;

import static org.opendaylight.controller.config.manager.impl.util.OsgiRegistrationUtil.registerService;
import static org.opendaylight.controller.config.manager.impl.util.OsgiRegistrationUtil.wrap;

public class ConfigManagerActivator implements BundleActivator {
    private final MBeanServer configMBeanServer = ManagementFactory.getPlatformMBeanServer();

    private AutoCloseable autoCloseable;

    @Override
    public void start(BundleContext context) {

        ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();// the inner strategy is backed by thread context cl?

        RefreshingSCPModuleInfoRegistry moduleInfoRegistryWrapper = new RefreshingSCPModuleInfoRegistry(
                moduleInfoBackedContext, moduleInfoBackedContext, context);

        ModuleInfoBundleTracker moduleInfoBundleTracker = new ModuleInfoBundleTracker(moduleInfoRegistryWrapper);

        CodecRegistryProvider codecRegistryProvider = new CodecRegistryProvider(moduleInfoBackedContext, context);

        // start config registry
        BundleContextBackedModuleFactoriesResolver bundleContextBackedModuleFactoriesResolver = new BundleContextBackedModuleFactoriesResolver(
                context);
        ConfigRegistryImpl configRegistry = new ConfigRegistryImpl(bundleContextBackedModuleFactoriesResolver, configMBeanServer,
                codecRegistryProvider.getCodecRegistry());

        // track bundles containing factories
        BlankTransactionServiceTracker blankTransactionServiceTracker = new BlankTransactionServiceTracker(
                configRegistry);
        ModuleFactoryBundleTracker primaryModuleFactoryBundleTracker = new ModuleFactoryBundleTracker(
                blankTransactionServiceTracker);

        // start extensible tracker
        ExtensibleBundleTracker<?> bundleTracker = new ExtensibleBundleTracker<>(context,
                primaryModuleFactoryBundleTracker, moduleInfoBundleTracker);
        bundleTracker.open();

        // register config registry to OSGi
        AutoCloseable configRegReg = registerService(context, configRegistry, ConfigRegistryImpl.class);

        // register config registry to jmx
        ConfigRegistryJMXRegistrator configRegistryJMXRegistrator = new ConfigRegistryJMXRegistrator(configMBeanServer);
        try {
            configRegistryJMXRegistrator.registerToJMX(configRegistry);
        } catch (InstanceAlreadyExistsException e) {
            throw new IllegalStateException("Config Registry was already registered to JMX", e);
        }

        // TODO wire directly via moduleInfoBundleTracker
        ServiceTracker<ModuleFactory, Object> serviceTracker = new ServiceTracker<>(context, ModuleFactory.class,
                blankTransactionServiceTracker);
        serviceTracker.open();

        List<AutoCloseable> list = Arrays.asList(
                codecRegistryProvider, configRegistry, wrap(bundleTracker), configRegReg, configRegistryJMXRegistrator, wrap(serviceTracker));
        autoCloseable = OsgiRegistrationUtil.aggregate(list);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        autoCloseable.close();
    }
}
