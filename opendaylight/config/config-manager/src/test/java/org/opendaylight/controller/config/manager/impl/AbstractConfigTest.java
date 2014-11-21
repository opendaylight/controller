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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import com.google.common.base.Preconditions;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import org.junit.After;
import org.junit.Before;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import org.opendaylight.yangtools.yang.data.impl.codec.CodecRegistry;
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
    @Mock
    protected BundleContext mockedContext;
    @Mock
    protected ServiceRegistration<?> mockedServiceRegistration;

    @Before
    public void setUpMocks() {
        MockitoAnnotations.initMocks(this);
    }


    // Default handler for OSGi service registration
    protected static class RecordingBundleContextServiceRegistrationHandler implements BundleContextServiceRegistrationHandler {
        private final List<RegistrationHolder> registrations = new LinkedList<>();
        @Override
        public void handleServiceRegistration(Class<?> clazz, Object serviceInstance, Dictionary<String, ?> props) {

            registrations.add(new RegistrationHolder(clazz, serviceInstance, props));
        }

        public List<RegistrationHolder> getRegistrations() {
            return registrations;
        }

        protected static class RegistrationHolder {
            protected final Class<?> clazz;
            protected final Object instance;
            protected final Dictionary<String, ?> props;

            public RegistrationHolder(Class<?> clazz, Object instance, Dictionary<String, ?> props) {
                this.clazz = clazz;
                this.instance = instance;
                this.props = props;
            }
        }

    }

    protected BundleContextServiceRegistrationHandler currentBundleContextServiceRegistrationHandler;

    protected BundleContextServiceRegistrationHandler getBundleContextServiceRegistrationHandler(Class<?> serviceType) {
        return currentBundleContextServiceRegistrationHandler;
    }

    // this method should be called in @Before
    protected void initConfigTransactionManagerImpl(
            ModuleFactoriesResolver resolver) {

        final MBeanServer platformMBeanServer = ManagementFactory
                .getPlatformMBeanServer();

        configRegistryJMXRegistrator = new ConfigRegistryJMXRegistrator(platformMBeanServer);
        initBundleContext();

        internalJmxRegistrator = new InternalJMXRegistrator(platformMBeanServer);
        baseJmxRegistrator = new BaseJMXRegistrator(internalJmxRegistrator);

        configRegistry = new ConfigRegistryImpl(resolver,
                platformMBeanServer, baseJmxRegistrator, getCodecRegistry());

        try {
            configRegistryJMXRegistrator.registerToJMX(configRegistry);
        } catch (InstanceAlreadyExistsException e) {
            throw new RuntimeException(e);
        }
        configRegistryClient = new ConfigRegistryJMXClient(platformMBeanServer);
        currentBundleContextServiceRegistrationHandler = new RecordingBundleContextServiceRegistrationHandler();
    }

    private void initBundleContext() {
        doNothing().when(mockedServiceRegistration).unregister();
        RegisterServiceAnswer answer = new RegisterServiceAnswer();
        doAnswer(answer).when(mockedContext).registerService(Matchers.<String>any(), any(), Matchers.<Dictionary<String, ?>>any());
        doAnswer(answer).when(mockedContext).registerService(Matchers.<Class>any(), any(), Matchers.<Dictionary<String, ?>>any());
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

    protected void assertStatus(CommitStatus status, int expectedNewInstances,
            int expectedRecreatedInstances, int expectedReusedInstances) {
        assertEquals("New instances mismatch in " + status, expectedNewInstances, status.getNewInstances().size());
        assertEquals("Recreated instances mismatch in " + status, expectedRecreatedInstances, status.getRecreatedInstances()
                .size());
        assertEquals("Reused instances mismatch in " + status, expectedReusedInstances, status.getReusedInstances()
                .size());
    }


    protected void assertBeanCount(int i, String configMXBeanName) {
        assertEquals(i, configRegistry.lookupConfigBeans(configMXBeanName)
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

    protected CodecRegistry getCodecRegistry() {
        return mock(CodecRegistry.class);
    }

    public static interface BundleContextServiceRegistrationHandler {

        void handleServiceRegistration(Class<?> clazz, Object serviceInstance, Dictionary<String, ?> props);

    }

    private class RegisterServiceAnswer implements Answer<ServiceRegistration<?>> {

        @Override
        public ServiceRegistration<?> answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();

            Preconditions.checkArgument(args.length == 3, "Unexpected arguments size (expected 3 was %s)", args.length);

            Object serviceTypeRaw = args[0];
            Object serviceInstance = args[1];
            Dictionary<String, ?> props = (Dictionary<String, ?>) args[2];

            if (serviceTypeRaw instanceof Class) {
                Class<?> serviceType = (Class<?>) serviceTypeRaw;
                invokeServiceHandler(serviceInstance, serviceType, props);

            } else if(serviceTypeRaw instanceof String[]) {
                for (String className : (String[]) serviceTypeRaw) {
                    invokeServiceHandler(serviceInstance, className, props);
                }
            } else if (serviceTypeRaw instanceof String) {
                invokeServiceHandler(serviceInstance, (String) serviceTypeRaw, props);
            } else {
                throw new IllegalStateException("Not handling service registration of type, Unknown type" +  serviceTypeRaw);
            }


            return mockedServiceRegistration;
        }

        public void invokeServiceHandler(Object serviceInstance, String className, Dictionary<String, ?> props) {
            try {
                Class<?> serviceType = Class.forName(className);
                invokeServiceHandler(serviceInstance, serviceType, props);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Not handling service registration of type " +  className, e);
            }
        }

        private void invokeServiceHandler(Object serviceInstance, Class<?> serviceType, Dictionary<String, ?> props) {
            BundleContextServiceRegistrationHandler serviceRegistrationHandler = getBundleContextServiceRegistrationHandler(serviceType);

            if (serviceRegistrationHandler != null) {
                serviceRegistrationHandler.handleServiceRegistration(serviceType, serviceInstance, props);
            }
        }
    }

    /**
     * Expand inner exception wrapped by JMX
     *
     * @param innerObject jmx proxy which will be wrapped and returned
     */
    protected <T> T rethrowCause(final T innerObject) {

        Object proxy = Proxy.newProxyInstance(innerObject.getClass().getClassLoader(),
                innerObject.getClass().getInterfaces(), new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            try {
                                return method.invoke(innerObject, args);
                            } catch (InvocationTargetException e) {
                                try {
                                    throw e.getTargetException();
                                } catch (RuntimeMBeanException e2) {
                                    throw e2.getTargetException();
                                }
                            }
                        }
                    }
        );
        return (T) proxy;
    }

}
