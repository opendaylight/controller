/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import static org.opendaylight.controller.config.manager.impl.util.OsgiRegistrationUtil.registerService;
import static org.opendaylight.controller.config.manager.impl.util.OsgiRegistrationUtil.wrap;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import org.opendaylight.controller.config.api.ConfigSystemService;
import org.opendaylight.controller.config.api.ConfigRegistry;
import org.opendaylight.controller.config.manager.impl.ConfigRegistryImpl;
import org.opendaylight.controller.config.manager.impl.jmx.ConfigRegistryJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.JMXNotifierConfigRegistry;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.BindingContextProvider;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.ModuleInfoBundleTracker;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.RefreshingSCPModuleInfoRegistry;
import org.opendaylight.controller.config.manager.impl.util.OsgiRegistrationUtil;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManagerActivator implements BundleActivator, SynchronousBundleListener, ConfigSystemService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigManagerActivator.class);

    private static final long SYSTEM_BUNDLE_ID = 0;

    private final MBeanServer configMBeanServer = ManagementFactory.getPlatformMBeanServer();

    private AutoCloseable autoCloseable;

    private ConfigRegistryImpl configRegistry;

    @Override
    public void start(final BundleContext context) {
        try {
            ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();// the inner strategy is backed by thread context cl?

            BindingContextProvider bindingContextProvider = new BindingContextProvider();

            RefreshingSCPModuleInfoRegistry moduleInfoRegistryWrapper = new RefreshingSCPModuleInfoRegistry(
                    moduleInfoBackedContext, moduleInfoBackedContext, moduleInfoBackedContext, moduleInfoBackedContext, bindingContextProvider, context);

            ModuleInfoBundleTracker moduleInfoBundleTracker = new ModuleInfoBundleTracker(context, moduleInfoRegistryWrapper);

            // start config registry
            BundleContextBackedModuleFactoriesResolver bundleContextBackedModuleFactoriesResolver = new BundleContextBackedModuleFactoriesResolver(
                    context);
            configRegistry = new ConfigRegistryImpl(bundleContextBackedModuleFactoriesResolver, configMBeanServer,
                    bindingContextProvider);

            // track bundles containing factories
            BlankTransactionServiceTracker blankTransactionServiceTracker = new BlankTransactionServiceTracker(
                    configRegistry);
            ModuleFactoryBundleTracker moduleFactoryTracker = new ModuleFactoryBundleTracker(
                    blankTransactionServiceTracker);

            boolean scanResolvedBundlesForModuleInfo = true;
            BundleTracker<Collection<ObjectRegistration<YangModuleInfo>>> moduleInfoResolvedBundleTracker = null;
            ExtensibleBundleTracker<?> moduleFactoryBundleTracker;
            if(scanResolvedBundlesForModuleInfo) {
                moduleInfoResolvedBundleTracker = new BundleTracker<>(context, Bundle.RESOLVED | Bundle.STARTING |
                            Bundle.STOPPING | Bundle.ACTIVE, moduleInfoBundleTracker);
                moduleFactoryBundleTracker = new ExtensibleBundleTracker<>(context, moduleFactoryTracker);
            } else {
                moduleFactoryBundleTracker = new ExtensibleBundleTracker<>(context,
                        moduleFactoryTracker, moduleInfoBundleTracker);
            }

            moduleInfoBundleTracker.open(moduleInfoResolvedBundleTracker);

            // start extensible tracker
            moduleFactoryBundleTracker.open();

            // Wrap config registry with JMX notification publishing adapter
            final JMXNotifierConfigRegistry notifyingConfigRegistry =
                    new JMXNotifierConfigRegistry(configRegistry, configMBeanServer);

            // register config registry to OSGi
            AutoCloseable clsReg = registerService(context, moduleInfoBackedContext, ClassLoadingStrategy.class);
            AutoCloseable configRegReg = registerService(context, notifyingConfigRegistry, ConfigRegistry.class);

            // register config registry to jmx
            ConfigRegistryJMXRegistrator configRegistryJMXRegistrator = new ConfigRegistryJMXRegistrator(configMBeanServer);
            try {
                configRegistryJMXRegistrator.registerToJMXNoNotifications(configRegistry);
            } catch (InstanceAlreadyExistsException e) {
                configRegistryJMXRegistrator.close();
                throw new IllegalStateException("Config Registry was already registered to JMX", e);
            }

            // register config registry to jmx
            final ConfigRegistryJMXRegistrator configRegistryJMXRegistratorWithNotifications = new ConfigRegistryJMXRegistrator(configMBeanServer);
            try {
                configRegistryJMXRegistrator.registerToJMX(notifyingConfigRegistry);
            } catch (InstanceAlreadyExistsException e) {
                configRegistryJMXRegistrator.close();
                configRegistryJMXRegistratorWithNotifications.close();
                throw new IllegalStateException("Config Registry was already registered to JMX", e);
            }

            // TODO wire directly via moduleInfoBundleTracker
            ServiceTracker<ModuleFactory, Object> serviceTracker = new ServiceTracker<>(context, ModuleFactory.class,
                    blankTransactionServiceTracker);
            serviceTracker.open();

            AutoCloseable configMgrReg = registerService(context, this, ConfigSystemService.class);

            List<AutoCloseable> list = Arrays.asList(bindingContextProvider, clsReg,
                    wrap(moduleFactoryBundleTracker), moduleInfoBundleTracker,
                    configRegReg, configRegistryJMXRegistrator, configRegistryJMXRegistratorWithNotifications,
                    wrap(serviceTracker), moduleInfoRegistryWrapper, notifyingConfigRegistry, configMgrReg);
            autoCloseable = OsgiRegistrationUtil.aggregate(list);

            context.addBundleListener(this);
        } catch(Exception e) {
            LOG.warn("Error starting config manager", e);
        } catch(Error e) {
            // Log JVM Error and re-throw. The OSGi container may silently fail the bundle and not always log
            // the exception. This has been seen on initial feature install.
            LOG.error("Error starting config manager", e);
            throw e;
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        context.removeBundleListener(this);
        autoCloseable.close();
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if(configRegistry == null) {
            return;
        }

        // If the system bundle (id 0) is stopping close the ConfigRegistry so it destroys all modules. On
        // shutdown the system bundle is stopped first.
        if(event.getBundle().getBundleId() == SYSTEM_BUNDLE_ID && event.getType() == BundleEvent.STOPPING) {
            configRegistry.close();
        }
    }

    @Override
    public void closeAllConfigModules() {
        if(configRegistry != null) {
            configRegistry.close();
        }
    }
}
