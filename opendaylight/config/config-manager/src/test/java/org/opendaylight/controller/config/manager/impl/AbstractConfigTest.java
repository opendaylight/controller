/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.Closeable;
import java.lang.management.ManagementFactory;
import java.util.Dictionary;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.After;
import org.mockito.Matchers;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.impl.jmx.BaseJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.ConfigRegistryJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.InternalJMXRegistrator;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Each test that relies on
 * {@link org.opendaylight.controller.config.manager.impl.ConfigRegistryImpl}
 * needs to subclass this test.
 * {@link org.opendaylight.controller.config.manager.impl.ConfigRegistryImpl} is
 * registered to platform MBean Server using
 * {@link #initConfigTransactionManagerImpl(org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver)}
 * typically during setting up the each test.
 */
public abstract class AbstractConfigTest extends
        AbstractLockedPlatformMBeanServerTest {
    protected ConfigRegistryJMXRegistrator configRegistryJMXRegistrator;
    protected ConfigRegistryImpl configRegistry;
    protected ConfigRegistryJMXClient configRegistryClient;
    protected BaseJMXRegistrator baseJmxRegistrator;
    protected InternalJMXRegistrator internalJmxRegistrator;
    protected BundleContext mockedContext;
    protected ServiceRegistration<?> mockedServiceRegistration;

    // this method should be called in @Before
    protected void initConfigTransactionManagerImpl(
            ModuleFactoriesResolver resolver) {
        final MBeanServer platformMBeanServer = ManagementFactory
                .getPlatformMBeanServer();

        configRegistryJMXRegistrator = new ConfigRegistryJMXRegistrator(
                platformMBeanServer);
        this.mockedContext = mock(BundleContext.class);
        this.mockedServiceRegistration = mock(ServiceRegistration.class);
        doNothing().when(mockedServiceRegistration).unregister();
        doReturn(mockedServiceRegistration).when(mockedContext).registerService(
                Matchers.any(String[].class), any(Closeable.class),
                any(Dictionary.class));
        internalJmxRegistrator = new InternalJMXRegistrator(platformMBeanServer);
        baseJmxRegistrator = new BaseJMXRegistrator(internalJmxRegistrator);

        configRegistry = new ConfigRegistryImpl(resolver, mockedContext,
                platformMBeanServer, baseJmxRegistrator);
        try {
            configRegistryJMXRegistrator.registerToJMX(configRegistry);
        } catch (InstanceAlreadyExistsException e) {
            throw new RuntimeException(e);
        }
        configRegistryClient = new ConfigRegistryJMXClient(platformMBeanServer);
    }

    @After
    public final void cleanUpConfigTransactionManagerImpl() {
        configRegistryJMXRegistrator.close();
        configRegistry.close();
    }

    /**
     * Can be called in @After of tests if some other cleanup is needed that
     * would be discarded by closing config beans in this method
     */
    protected void destroyAllConfigBeans() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        Set<ObjectName> all = transaction.lookupConfigBeans();
        // workaround for getting same Module more times
        while (all.size() > 0) {
            transaction.destroyModule(all.iterator().next());
            all = transaction.lookupConfigBeans();
        }
        transaction.commit();
    }

    protected void assertSame(ObjectName oN1, ObjectName oN2) {
        assertEquals(oN1.getKeyProperty("instanceName"),
                oN2.getKeyProperty("instanceName"));
        assertEquals(oN1.getKeyProperty("interfaceName"),
                oN2.getKeyProperty("interfaceName"));
    }

    protected void assertStatus(CommitStatus status, int expectedNewInstances,
            int expectedRecreatedInstances, int expectedReusedInstances) {
        assertEquals(expectedNewInstances, status.getNewInstances().size());
        assertEquals(expectedRecreatedInstances, status.getRecreatedInstances()
                .size());
        assertEquals(expectedReusedInstances, status.getReusedInstances()
                .size());
    }

    protected ObjectName createTestConfigBean(
            ConfigTransactionJMXClient transaction, String implementationName,
            String name) throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(implementationName,
                name);
        return nameCreated;
    }

    protected void assertBeanCount(int i, String configMXBeanName) {
        assertEquals(i, configRegistry.lookupConfigBeans(configMXBeanName)
                .size());
    }

    protected void assertBeanExists(int count, String moduleName,
            String instanceName) {
        assertEquals(1,
                configRegistry.lookupConfigBeans(moduleName, instanceName)
                        .size());
    }

    /**
     *
     * @param configBeanClass
     *            Empty constructor class of config bean to be instantiated
     *            whenever create
     * @param implementationName
     * @return
     */
    protected ClassBasedModuleFactory createClassBasedCBF(
            Class<? extends Module> configBeanClass, String implementationName) {
        return new ClassBasedModuleFactory(implementationName, configBeanClass);
    }
}
