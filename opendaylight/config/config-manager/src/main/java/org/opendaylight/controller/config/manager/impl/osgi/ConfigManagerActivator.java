/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import org.opendaylight.controller.config.api.ConfigRegistry;
import org.opendaylight.controller.config.api.ConfigSystemService;
import org.opendaylight.controller.config.manager.impl.ConfigRegistryImpl;
import org.opendaylight.controller.config.manager.impl.jmx.ConfigRegistryJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.JMXNotifierConfigRegistry;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.BindingContextProvider;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.ModuleInfoBundleTracker;
import org.opendaylight.controller.config.manager.impl.osgi.mapping.RefreshingSCPModuleInfoRegistry;
import org.opendaylight.controller.config.manager.impl.util.OsgiRegistrationUtil;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.mdsal.binding.generator.api.ClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
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
        LOG.info("Config manager starting...");
        try {
            // the inner strategy is backed by thread context cl?
            final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();

            final BindingContextProvider bindingContextProvider = new BindingContextProvider();

            final RefreshingSCPModuleInfoRegistry moduleInfoRegistryWrapper = new RefreshingSCPModuleInfoRegistry(
                    moduleInfoBackedContext, moduleInfoBackedContext, moduleInfoBackedContext, moduleInfoBackedContext, bindingContextProvider, context);

            final ModuleInfoBundleTracker moduleInfoBundleTracker = new ModuleInfoBundleTracker(context, moduleInfoRegistryWrapper);

            // start config registry
            final BundleContextBackedModuleFactoriesResolver bundleContextBackedModuleFactoriesResolver = new BundleContextBackedModuleFactoriesResolver(
                    context);
            this.configRegistry = new ConfigRegistryImpl(bundleContextBackedModuleFactoriesResolver, this.configMBeanServer,
                    bindingContextProvider);

            // track bundles containing factories
            final BlankTransactionServiceTracker blankTransactionServiceTracker = new BlankTransactionServiceTracker(
                    this.configRegistry);
            final ModuleFactoryBundleTracker moduleFactoryTracker = new ModuleFactoryBundleTracker(
                    blankTransactionServiceTracker);

            BundleTracker<Collection<ObjectRegistration<YangModuleInfo>>> moduleInfoResolvedBundleTracker =
                    new BundleTracker<>(context, Bundle.RESOLVED | Bundle.STARTING | Bundle.STOPPING | Bundle.ACTIVE,
                            moduleInfoBundleTracker);
            ExtensibleBundleTracker<?> moduleFactoryBundleTracker = new ExtensibleBundleTracker<>(context,
                    moduleFactoryTracker);
            moduleInfoBundleTracker.open(moduleInfoResolvedBundleTracker);

            // start extensible tracker
            moduleFactoryBundleTracker.open();

            // Wrap config registry with JMX notification publishing adapter
            final JMXNotifierConfigRegistry notifyingConfigRegistry =
                    new JMXNotifierConfigRegistry(this.configRegistry, this.configMBeanServer);

            // register config registry to OSGi
            final AutoCloseable clsReg = OsgiRegistrationUtil.registerService(context, moduleInfoBackedContext, ClassLoadingStrategy.class);
            final AutoCloseable configRegReg = OsgiRegistrationUtil.registerService(context, notifyingConfigRegistry, ConfigRegistry.class);

            // register config registry to jmx
            final ConfigRegistryJMXRegistrator configRegistryJMXRegistrator = new ConfigRegistryJMXRegistrator(this.configMBeanServer);
            try {
                configRegistryJMXRegistrator.registerToJMXNoNotifications(this.configRegistry);
            } catch (final InstanceAlreadyExistsException e) {
                configRegistryJMXRegistrator.close();
                throw new IllegalStateException("Config Registry was already registered to JMX", e);
            }

            // register config registry to jmx
            final ConfigRegistryJMXRegistrator configRegistryJMXRegistratorWithNotifications = new ConfigRegistryJMXRegistrator(this.configMBeanServer);
            try {
                configRegistryJMXRegistrator.registerToJMX(notifyingConfigRegistry);
            } catch (final InstanceAlreadyExistsException e) {
                configRegistryJMXRegistrator.close();
                configRegistryJMXRegistratorWithNotifications.close();
                throw new IllegalStateException("Config Registry was already registered to JMX", e);
            }

            // TODO wire directly via moduleInfoBundleTracker
            final ServiceTracker<ModuleFactory, Object> serviceTracker = new ServiceTracker<>(context, ModuleFactory.class,
                    blankTransactionServiceTracker);
            serviceTracker.open();

            final AutoCloseable configMgrReg = OsgiRegistrationUtil.registerService(context, this, ConfigSystemService.class);

            final List<AutoCloseable> list = Arrays.asList(bindingContextProvider, clsReg,
                    OsgiRegistrationUtil.wrap(moduleFactoryBundleTracker), moduleInfoBundleTracker,
                    configRegReg, configRegistryJMXRegistrator, configRegistryJMXRegistratorWithNotifications,
                    OsgiRegistrationUtil.wrap(serviceTracker), moduleInfoRegistryWrapper, notifyingConfigRegistry, configMgrReg);
            this.autoCloseable = OsgiRegistrationUtil.aggregate(list);

            context.addBundleListener(this);
        } catch(final Exception e) {
            LOG.error("Error starting config manager", e);
        } catch(final Error e) {
            // Log JVM Error and re-throw. The OSGi container may silently fail the bundle and not always log
            // the exception. This has been seen on initial feature install.
            LOG.error("Error starting config manager", e);
            throw e;
        }

        LOG.info("Config manager start complete");
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        LOG.info("Config manager stopping");
        context.removeBundleListener(this);
        this.autoCloseable.close();
    }

    @Override
    public void bundleChanged(final BundleEvent event) {
        if(this.configRegistry == null) {
            return;
        }

        // If the system bundle (id 0) is stopping close the ConfigRegistry so it destroys all modules. On
        // shutdown the system bundle is stopped first.
        if(event.getBundle().getBundleId() == SYSTEM_BUNDLE_ID && event.getType() == BundleEvent.STOPPING) {
            this.configRegistry.close();
        }
    }

    @Override
    public void closeAllConfigModules() {
        if(this.configRegistry != null) {
            this.configRegistry.close();
        }
    }
}
