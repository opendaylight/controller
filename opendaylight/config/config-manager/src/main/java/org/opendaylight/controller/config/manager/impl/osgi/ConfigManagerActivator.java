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
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;

public class ConfigManagerActivator implements BundleActivator {
	private static final Logger logger = LoggerFactory.getLogger(ConfigManagerActivator.class);

	private ExtenderBundleTracker extenderBundleTracker;
	private ConfigRegistryImpl configRegistry;
	private ConfigRegistryJMXRegistrator configRegistryJMXRegistrator;

	@Override
	public void start(BundleContext context) throws Exception {
		extenderBundleTracker = new ExtenderBundleTracker(context);
		extenderBundleTracker.open();
		BundleContextBackedModuleFactoriesResolver bundleContextBackedModuleFactoriesResolver = new BundleContextBackedModuleFactoriesResolver(
				context);

		MBeanServer configMBeanServer = ManagementFactory.getPlatformMBeanServer();
		configRegistry = new ConfigRegistryImpl(bundleContextBackedModuleFactoriesResolver, context, configMBeanServer);
		// register config registry to jmx

		configRegistryJMXRegistrator = new ConfigRegistryJMXRegistrator(configMBeanServer);
		configRegistryJMXRegistrator.registerToJMX(configRegistry);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
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
			logger.warn("Exception while closing config registry jmx registrator", e);
		}
	}
}
