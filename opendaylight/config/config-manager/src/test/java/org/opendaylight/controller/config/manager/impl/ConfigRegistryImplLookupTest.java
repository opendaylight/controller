/*
 * Copyright (c) 2013, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Sets;
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

public class ConfigRegistryImplLookupTest extends AbstractLockedPlatformMBeanServerTest {

    private ConfigRegistryImpl configRegistryImpl;
    private BaseJMXRegistrator baseJMXRegistrator;

    private static final String MODULE_NAMEA = "moduleA";
    private static final String MODULE_NAMEB = "moduleB";

    private static final String INSTANCE_NAMEA = "instA";
    private static final String INSTANCE_NAMEB = "instB";
    private static final String INSTANCE_NAMEC = "instC";

    private static final ObjectName NAME1 = ObjectNameUtil.createReadOnlyModuleON(MODULE_NAMEA, INSTANCE_NAMEA);
    private static final ObjectName NAME2 = ObjectNameUtil.createReadOnlyModuleON(MODULE_NAMEA, INSTANCE_NAMEB);
    private static final ObjectName NAME3 = ObjectNameUtil.createReadOnlyModuleON(MODULE_NAMEA, INSTANCE_NAMEC);
    private static final ObjectName NAME4 = ObjectNameUtil.createReadOnlyModuleON(MODULE_NAMEB, INSTANCE_NAMEA);

    private static final ObjectName NAME5 = ObjectNameUtil.createRuntimeBeanName(MODULE_NAMEA, INSTANCE_NAMEA,
            Collections.<String, String>emptyMap());
    private static final ObjectName NAME6 = ObjectNameUtil.createRuntimeBeanName(MODULE_NAMEA, INSTANCE_NAMEB,
            Collections.<String, String>emptyMap());
    private static final ObjectName NAME8 = ObjectNameUtil.createRuntimeBeanName(MODULE_NAMEB, INSTANCE_NAMEA,
            Collections.<String, String>emptyMap());

    private static final ObjectName NAME9 = ObjectNameUtil.createTransactionModuleON("transaction", MODULE_NAMEA,
            INSTANCE_NAMEA);

    @Before
    public void setUp() throws Exception {
        configRegistryImpl = new ConfigRegistryImpl(null, ManagementFactory.getPlatformMBeanServer(), null);
        Field field = configRegistryImpl.getClass().getDeclaredField("baseJMXRegistrator");
        field.setAccessible(true);
        baseJMXRegistrator = (BaseJMXRegistrator) field.get(configRegistryImpl);

        registerModuleBean(new TestingRuntimeBean(), baseJMXRegistrator, NAME1);
        registerModuleBean(new TestingRuntimeBean(), baseJMXRegistrator, NAME2);
        registerModuleBean(new TestingRuntimeBean(), baseJMXRegistrator, NAME3);
        registerModuleBean(new TestingRuntimeBean(), baseJMXRegistrator, NAME4);

        registerRuntimeBean(new TestingRuntimeBean(), baseJMXRegistrator, NAME5);
        registerRuntimeBean(new TestingRuntimeBean(), baseJMXRegistrator, NAME6);
        registerRuntimeBean(new TestingRuntimeBean(), baseJMXRegistrator, NAME8);

        baseJMXRegistrator.createTransactionJMXRegistrator("transaction").createTransactionModuleJMXRegistrator()
                .registerMBean(new TestingRuntimeBean(), NAME9);

    }

    private static void registerModuleBean(final TestingRuntimeBean testingRuntimeBean,
            final BaseJMXRegistrator baseJMXRegistrator, final ObjectName objectName)
            throws InstanceAlreadyExistsException {
        baseJMXRegistrator.createModuleJMXRegistrator().registerMBean(testingRuntimeBean, objectName);
    }

    private static void registerRuntimeBean(final RuntimeBean object, final BaseJMXRegistrator baseJMXRegistrator,
            final ObjectName runtimeON) throws InstanceAlreadyExistsException {
        String factoryName = ObjectNameUtil.getFactoryName(runtimeON);
        String instanceName = ObjectNameUtil.getInstanceName(runtimeON);
        Map<String, String> properties = ObjectNameUtil.getAdditionalPropertiesOfRuntimeBeanName(runtimeON);

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
        assertEquals(Sets.newHashSet(NAME1, NAME2, NAME3, NAME4), beans);
        beans = configRegistryImpl.lookupConfigBeans();
        assertEquals(Sets.newHashSet(NAME1, NAME2, NAME3, NAME4), beans);
    }

    @Test
    public void testLookupConfigBeanWithModuleName() throws Exception {
        Set<ObjectName> bean = configRegistryImpl.lookupConfigBeans(MODULE_NAMEA);
        assertEquals(Sets.newHashSet(NAME1, NAME2, NAME3), bean);
    }

    @Test
    public void testLookupConfigBeanWithModuleNameAndInstanceName() throws Exception {
        Set<ObjectName> bean = configRegistryImpl.lookupConfigBeans(MODULE_NAMEA, INSTANCE_NAMEA);
        assertEquals(Sets.newHashSet(NAME1), bean);
    }

    @Test
    public void testLookupRuntimeBeans() throws Exception {
        Set<ObjectName> beans = configRegistryImpl.lookupRuntimeBeans();
        assertEquals(Sets.newHashSet(NAME5, NAME6, NAME8), beans);
        beans = configRegistryImpl.lookupRuntimeBeans(null, null);
        assertEquals(Sets.newHashSet(NAME5, NAME6, NAME8), beans);
    }

    @Test
    public void testLookupRuntimeBeansWithIFcNameAndImplName() throws Exception {
        Set<ObjectName> beans = configRegistryImpl.lookupRuntimeBeans(MODULE_NAMEA, INSTANCE_NAMEA);
        assertEquals(Sets.newHashSet(NAME5), beans);
    }
}
