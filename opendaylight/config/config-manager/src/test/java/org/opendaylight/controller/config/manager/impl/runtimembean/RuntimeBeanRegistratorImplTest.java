/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.runtimembean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.StringContains.containsString;

import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.api.runtime.HierarchicalRuntimeBeanRegistration;
import org.opendaylight.controller.config.manager.impl.AbstractLockedPlatformMBeanServerTest;
import org.opendaylight.controller.config.manager.impl.jmx.BaseJMXRegistrator;
import org.opendaylight.controller.config.manager.impl.jmx.HierarchicalRuntimeBeanRegistrationImpl;
import org.opendaylight.controller.config.manager.impl.jmx.RootRuntimeBeanRegistratorImpl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class RuntimeBeanRegistratorImplTest extends
        AbstractLockedPlatformMBeanServerTest {
    static final String module1 = "module1";
    static final String INSTANCE_NAME = "instanceName";
    String additionalKey = "key";
    String additionalValue = "value";
    Map<String, String> additionalProperties = ImmutableMap.of(additionalKey,
            additionalValue);

    private BaseJMXRegistrator baseJMXRegistrator;
    private RootRuntimeBeanRegistratorImpl tested;
    private final ModuleIdentifier moduleIdentifier = new ModuleIdentifier(
            module1, INSTANCE_NAME);

    @Before
    public void setUp() {
        baseJMXRegistrator = new BaseJMXRegistrator(
                ManagementFactory.getPlatformMBeanServer());
        tested = baseJMXRegistrator
                .createRuntimeBeanRegistrator(moduleIdentifier);
    }

    @After
    public void tearDown() {
        tested.close();
        assertEquals(0, baseJMXRegistrator.getRegisteredObjectNames().size());
    }

    protected void checkExists(ObjectName on) throws Exception {
        platformMBeanServer.getMBeanInfo(on);
    }

    protected void checkNotExists(ObjectName on) throws Exception {
        try {
            platformMBeanServer.getMBeanInfo(on);
            fail();
        } catch (InstanceNotFoundException e) {

        }
    }

    @Test
    public void testRegisterMBeanWithoutAdditionalProperties() throws Exception {
        createRoot();
    }

    private HierarchicalRuntimeBeanRegistrationImpl createRoot()
            throws Exception {
        HierarchicalRuntimeBeanRegistrationImpl rootRegistration = tested
                .registerRoot(new TestingRuntimeBean());

        ObjectName expectedON1 = ObjectNameUtil.createRuntimeBeanName(module1,
                INSTANCE_NAME, Maps.<String, String> newHashMap());

        assertEquals(expectedON1, rootRegistration.getObjectName());
        checkExists(rootRegistration.getObjectName());
        return rootRegistration;
    }

    @Test
    public void testRegisterMBeanWithAdditionalProperties() throws Exception {
        HierarchicalRuntimeBeanRegistrationImpl rootRegistration = createRoot();
        createAdditional(rootRegistration);
    }

    private HierarchicalRuntimeBeanRegistration createAdditional(
            HierarchicalRuntimeBeanRegistrationImpl rootRegistration)
            throws Exception {

        HierarchicalRuntimeBeanRegistrationImpl registration = rootRegistration
                .register(additionalKey, additionalValue, new TestingRuntimeBean());

        ObjectName expectedON1 = ObjectNameUtil.createRuntimeBeanName(module1,
                INSTANCE_NAME, additionalProperties);

        assertEquals(expectedON1, registration.getObjectName());
        checkExists(registration.getObjectName());
        return registration;
    }

    @Test
    public void testCloseRegistration() throws Exception {
        HierarchicalRuntimeBeanRegistrationImpl rootRegistration = createRoot();
        rootRegistration.close();
        checkNotExists(rootRegistration.getObjectName());
    }

    @Test
    public void testCloseRegistrator() throws Exception {
        HierarchicalRuntimeBeanRegistrationImpl rootRegistration = createRoot();
        HierarchicalRuntimeBeanRegistration childRegistration = createAdditional(rootRegistration);
        tested.close();
        checkNotExists(rootRegistration.getObjectName());
        checkNotExists(childRegistration.getObjectName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegistration_overrideType() throws Exception {
        HierarchicalRuntimeBeanRegistrationImpl rootRegistration = createRoot();
        rootRegistration.register("type", "xxx", new TestingRuntimeBean());
    }

    @Test
    public void testRegistrationException() throws Exception {
        HierarchicalRuntimeBeanRegistrationImpl rootRegistration = createRoot();
        try {
            createRoot();
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString(rootRegistration
                    .getObjectName().toString()));
            assertThat(e.getMessage(),
                    containsString("Could not register runtime bean"));
            assertThat(e.getMessage(),
                    containsString(moduleIdentifier.toString()));
        }
    }

    @Test
    public void testIgnoringExceptionInClose() throws Exception {
        HierarchicalRuntimeBeanRegistrationImpl rootRegistration = createRoot();
        platformMBeanServer.unregisterMBean(rootRegistration.getObjectName());
        rootRegistration.close();
    }

}
