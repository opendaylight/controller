/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.api.runtime.RuntimeBean;
import org.opendaylight.controller.config.manager.impl.jmx.BaseJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.RootRuntimeBeanRegistratorImpl;
import org.opendaylight.controller.config.manager.impl.runtimembean.TestingRuntimeBean;

import com.google.common.collect.Sets;

public class ConfigRegistryImplLookupTest extends
        AbstractLockedPlatformMBeanServerTest {

    private ConfigRegistryImpl configRegistryImpl;
    private BaseJMXRegistrator baseJMXRegistrator;

    private static final String moduleNameA = "moduleA";
    private static final String moduleNameB = "moduleB";

    private static final String instanceNameA = "instA";
    private static final String instanceNameB = "instB";
    private static final String instanceNameC = "instC";

    private static final ObjectName name1 = ObjectNameUtil
            .createReadOnlyModuleON(moduleNameA, instanceNameA);
    private static final ObjectName name2 = ObjectNameUtil
            .createReadOnlyModuleON(moduleNameA, instanceNameB);
    private static final ObjectName name3 = ObjectNameUtil
            .createReadOnlyModuleON(moduleNameA, instanceNameC);
    private static final ObjectName name4 = ObjectNameUtil
            .createReadOnlyModuleON(moduleNameB, instanceNameA);

    private static final ObjectName name5 = ObjectNameUtil
            .createRuntimeBeanName(moduleNameA, instanceNameA, Collections.<String, String>emptyMap());
    private static final ObjectName name6 = ObjectNameUtil
            .createRuntimeBeanName(moduleNameA, instanceNameB, Collections.<String, String>emptyMap());
    private static final ObjectName name8 = ObjectNameUtil
            .createRuntimeBeanName(moduleNameB, instanceNameA, Collections.<String, String>emptyMap());

    private static final ObjectName name9 = ObjectNameUtil
            .createTransactionModuleON("transaction", moduleNameA, instanceNameA);

    @Before
    public void setUp() throws Exception {
        configRegistryImpl = new ConfigRegistryImpl(null, null,
                ManagementFactory.getPlatformMBeanServer());
        Field field = configRegistryImpl.getClass().getDeclaredField(
                "baseJMXRegistrator");
        field.setAccessible(true);
        baseJMXRegistrator = (BaseJMXRegistrator) field.get(configRegistryImpl);

        registerModuleBean(new TestingRuntimeBean(), baseJMXRegistrator, name1);
        registerModuleBean(new TestingRuntimeBean(), baseJMXRegistrator, name2);
        registerModuleBean(new TestingRuntimeBean(), baseJMXRegistrator, name3);
        registerModuleBean(new TestingRuntimeBean(), baseJMXRegistrator, name4);

        registerRuntimeBean(new TestingRuntimeBean(), baseJMXRegistrator, name5);
        registerRuntimeBean(new TestingRuntimeBean(), baseJMXRegistrator, name6);
        registerRuntimeBean(new TestingRuntimeBean(), baseJMXRegistrator, name8);

        baseJMXRegistrator.createTransactionJMXRegistrator("transaction")
                .createTransactionModuleJMXRegistrator()
                .registerMBean(new TestingRuntimeBean(), name9);

    }

    private void registerModuleBean(TestingRuntimeBean testingRuntimeBean,
            BaseJMXRegistrator baseJMXRegistrator, ObjectName objectName)
            throws InstanceAlreadyExistsException {
        baseJMXRegistrator.createModuleJMXRegistrator().registerMBean(
                testingRuntimeBean, objectName);
    }

    private void registerRuntimeBean(RuntimeBean object,
            BaseJMXRegistrator baseJMXRegistrator, ObjectName runtimeON)
            throws InstanceAlreadyExistsException {
        String factoryName = ObjectNameUtil.getFactoryName(runtimeON);
        String instanceName = ObjectNameUtil.getInstanceName(runtimeON);
        Map<String, String> properties = ObjectNameUtil
                .getAdditionalPropertiesOfRuntimeBeanName(runtimeON);

        RootRuntimeBeanRegistratorImpl runtimeBeanRegistrator = baseJMXRegistrator
                .createRuntimeBeanRegistrator(new ModuleIdentifier(factoryName, instanceName));

        assertThat(properties.isEmpty(), is(true));

        runtimeBeanRegistrator.registerRoot(object);
    }

    @After
    public void cleanUp() {
        baseJMXRegistrator.close();
    }

    @Test
    public void testLookupConfigBeans() throws Exception {
        Set<ObjectName> beans = configRegistryImpl.lookupConfigBeans();
        assertEquals(Sets.newHashSet(name1, name2, name3, name4), beans);
        beans = configRegistryImpl.lookupConfigBeans();
        assertEquals(Sets.newHashSet(name1, name2, name3, name4), beans);
    }

    @Test
    public void testLookupConfigBeanWithModuleName() throws Exception {
        Set<ObjectName> bean = configRegistryImpl
                .lookupConfigBeans(moduleNameA);
        assertEquals(Sets.newHashSet(name1, name2, name3), bean);
    }

    @Test
    public void testLookupConfigBeanWithModuleNameAndInstanceName()
            throws Exception {
        Set<ObjectName> bean = configRegistryImpl.lookupConfigBeans(
                moduleNameA, instanceNameA);
        assertEquals(Sets.newHashSet(name1), bean);
    }

    @Test
    public void testLookupRuntimeBeans() throws Exception {
        Set<ObjectName> beans = configRegistryImpl.lookupRuntimeBeans();
        assertEquals(Sets.newHashSet(name5, name6, name8), beans);
        beans = configRegistryImpl.lookupRuntimeBeans(null, null);
        assertEquals(Sets.newHashSet(name5, name6, name8), beans);
    }

    @Test
    public void testLookupRuntimeBeansWithIFcNameAndImplName() throws Exception {
        Set<ObjectName> beans = configRegistryImpl.lookupRuntimeBeans(
                moduleNameA, instanceNameA);
        assertEquals(Sets.newHashSet(name5), beans);
    }

}
