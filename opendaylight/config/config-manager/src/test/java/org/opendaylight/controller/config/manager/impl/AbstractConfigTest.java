/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import junit.framework.Assert;
import org.junit.After;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.impl.jmx.BaseJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.ConfigRegistryJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.InternalJMXRegistrator;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.TestingScheduledThreadPoolImpl;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPool;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.Closeable;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

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
    protected BundleContext mockedContext = mock(BundleContext.class);
    protected ServiceRegistration<?> mockedServiceRegistration;

    private static final Logger logger = LoggerFactory.getLogger(AbstractConfigTest.class);

    protected  Map<Class, BundleContextServiceRegistrationHandler> getBundleContextServiceRegistrationHandlers() {
        return Maps.newHashMap();
    }

    // this method should be called in @Before
    protected void initConfigTransactionManagerImpl(
            ModuleFactoriesResolver resolver) {
        final MBeanServer platformMBeanServer = ManagementFactory
                .getPlatformMBeanServer();

        configRegistryJMXRegistrator = new ConfigRegistryJMXRegistrator(
                platformMBeanServer);
        initBundleContext();

        internalJmxRegistrator = new InternalJMXRegistrator(platformMBeanServer);
        baseJmxRegistrator = new BaseJMXRegistrator(internalJmxRegistrator);

        configRegistry = new ConfigRegistryImpl(resolver,
                platformMBeanServer, baseJmxRegistrator);

        try {
            configRegistryJMXRegistrator.registerToJMX(configRegistry);
        } catch (InstanceAlreadyExistsException e) {
            throw new RuntimeException(e);
        }
        configRegistryClient = new ConfigRegistryJMXClient(platformMBeanServer);
    }

    private void initBundleContext() {
        this.mockedServiceRegistration = mock(ServiceRegistration.class);
        doNothing().when(mockedServiceRegistration).unregister();

        RegisterServiceAnswer answer = new RegisterServiceAnswer();

        doAnswer(answer).when(mockedContext).registerService(Matchers.any(String[].class),
                any(Closeable.class), Matchers.<Dictionary<String, ?>>any());
        doAnswer(answer).when(mockedContext).registerService(Matchers.<Class<Closeable>>any(), any(Closeable.class),
                Matchers.<Dictionary<String, ?>>any());
    }


    public Collection<InputStream> getFilesAsInputStreams(List<String> paths) {
        final Collection<InputStream> resources = new ArrayList<>();
        List<String> failedToFind = new ArrayList<>();
        for (String path : paths) {
            InputStream resourceAsStream = getClass().getResourceAsStream(path);
            if (resourceAsStream == null) {
                failedToFind.add(path);
            } else {
                resources.add(resourceAsStream);
            }
        }
        Assert.assertEquals("Some files were not found", Collections.<String>emptyList(), failedToFind);

        return resources;
    }

    @After
    public final void cleanUpConfigTransactionManagerImpl() {
        configRegistryJMXRegistrator.close();
        configRegistry.close();
        TestingFixedThreadPool.cleanUp();
        TestingScheduledThreadPoolImpl.cleanUp();
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
        assertEquals("New instances mismatch in " + status, expectedNewInstances, status.getNewInstances().size());
        assertEquals("Recreated instances mismatch in " + status, expectedRecreatedInstances, status.getRecreatedInstances()
                .size());
        assertEquals("Reused instances mismatch in " + status, expectedReusedInstances, status.getReusedInstances()
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


    public static interface BundleContextServiceRegistrationHandler {

       void handleServiceRegistration(Object serviceInstance);

    }

    private class RegisterServiceAnswer implements Answer {

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();

            Preconditions.checkArgument(args.length == 3, "Unexpected arguments size (expected 3 was %s)", args.length);

            Object serviceTypeRaw = args[0];
            Object serviceInstance = args[1];

            if (serviceTypeRaw instanceof Class) {
                Class<?> serviceType = (Class<?>) serviceTypeRaw;
                invokeServiceHandler(serviceInstance, serviceType);

            } else if(serviceTypeRaw instanceof String[]) {
                for (String className : (String[]) serviceTypeRaw) {
                    try {
                        Class<?> serviceType = Class.forName(className);
                        invokeServiceHandler(serviceInstance, serviceType);
                    } catch (ClassNotFoundException e) {
                        logger.warn("Not handling service registration of type {} ", className, e);
                    }
                }

            } else
                logger.debug("Not handling service registration of type {}, Unknown type", serviceTypeRaw);

            return mockedServiceRegistration;
        }

        private void invokeServiceHandler(Object serviceInstance, Class<?> serviceType) {
            BundleContextServiceRegistrationHandler serviceRegistrationHandler = getBundleContextServiceRegistrationHandlers()
                    .get(serviceType);

            if (serviceRegistrationHandler != null) {
                serviceRegistrationHandler.handleServiceRegistration(serviceType.cast(serviceInstance));
            }
        }
    }
}
