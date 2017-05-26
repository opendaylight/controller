/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.lang.management.ManagementFactory;
import org.junit.Test;
import org.opendaylight.controller.config.manager.impl.AbstractLockedPlatformMBeanServerTest;
import org.opendaylight.controller.config.manager.impl.ConfigRegistryImpl;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolModuleFactory;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigRegistryImplTest extends
        AbstractLockedPlatformMBeanServerTest {
    private static final Logger LOG = LoggerFactory
            .getLogger(ConfigRegistryImplTest.class);

    @Test
    public void testFailOnTwoFactoriesExportingSameImpl() {
        ModuleFactory factory = new TestingFixedThreadPoolModuleFactory();
        BundleContext context = mock(BundleContext.class);
        ConfigRegistryImpl configRegistry = null;
        try {
            ModuleFactoriesResolver resolver = new HardcodedModuleFactoriesResolver(mock(BundleContext.class),
                    factory, factory);

            configRegistry = new ConfigRegistryImpl(resolver,
                    ManagementFactory.getPlatformMBeanServer(), null);

            configRegistry.beginConfig();
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage()
                            .startsWith("Module name is not unique. Found two conflicting factories with same name " +
                                    "'fixed':"));
            verifyZeroInteractions(context);
        } finally {
            try {
                configRegistry.close();
            } catch (Exception e) {
                // ignore
                LOG.warn("Ignoring exception", e);
            }
        }
    }

}
