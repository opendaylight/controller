/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.test.impl;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.opendaylight.controller.config.api.jmx.ObjectNameUtil.getInstanceName;
import static org.opendaylight.controller.config.api.jmx.ObjectNameUtil.getTransactionName;

import java.util.List;
import javax.management.ObjectName;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

public class MultipleDependenciesModuleTest extends AbstractConfigTest {
    private static final MultipleDependenciesModuleFactory factory = new MultipleDependenciesModuleFactory();

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, factory));
    }

    @Test
    public void testMultipleDependencies() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        ObjectName d1 = transaction.createModule(factory.getImplementationName(), "d1");
        ObjectName d2 = transaction.createModule(factory.getImplementationName(), "d2");

        assertEquals(transaction.getTransactionName(), getTransactionName(d1));

        ObjectName parent = transaction.createModule(factory.getImplementationName(), "parent");
        MultipleDependenciesModuleMXBean multipleDependenciesModuleMXBean = transaction.newMXBeanProxy(parent, MultipleDependenciesModuleMXBean.class);
        multipleDependenciesModuleMXBean.setTestingDeps(asList(d1, d2));
        List<ObjectName> found = multipleDependenciesModuleMXBean.getTestingDeps();
        ObjectName d1WithoutTxName = found.get(0);
        assertEquals(getInstanceName(d1), getInstanceName(d1WithoutTxName));
        // check that transaction name gets stripped automatically from attribute.
        // d1,2 contained tx name, found doesn't
        assertNull(getTransactionName(d1WithoutTxName));
        transaction.commit();
    }

    @Test
    public void testCloseOrdering() throws Exception {
        // Tests whether close is called in correct order on the module instances on following graph
        // Each module tests whether its dependencies were closed before it (to simulate resource clean up failure)
        // R1
        // | \
        // M1 M2
        // |
        // L1
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        ObjectName r1 = transaction.createModule(factory.getImplementationName(), "root1");
        ObjectName m1 = transaction.createModule(factory.getImplementationName(), "middle1");
        ObjectName m2 = transaction.createModule(factory.getImplementationName(), "middle2");
        ObjectName l1 = transaction.createModule(factory.getImplementationName(), "leaf1");

        MultipleDependenciesModuleMXBean r1Proxy = transaction.newMXBeanProxy(r1, MultipleDependenciesModuleMXBean.class);
        MultipleDependenciesModuleMXBean i1Proxy = transaction.newMXBeanProxy(m1, MultipleDependenciesModuleMXBean.class);
        r1Proxy.setSingle(m1);
        i1Proxy.setSingle(l1);
        r1Proxy.setTestingDeps(asList(m2));
        transaction.commit();

        configRegistryClient.createTransaction().commit();
        transaction = configRegistryClient.createTransaction();
        MultipleDependenciesModuleMXBean l1Proxy = transaction.newMXBeanProxy(l1, MultipleDependenciesModuleMXBean.class);
        l1Proxy.setSimple(true);
        transaction.commit();
    }

    @Test
    public void testDestroyModuleDependency() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        ObjectName r1 = transaction.createModule(factory.getImplementationName(), "root1");
        ObjectName m1 = transaction.createModule(factory.getImplementationName(), "middle1");

        MultipleDependenciesModuleMXBean r1Proxy = transaction.newMXBeanProxy(r1, MultipleDependenciesModuleMXBean.class);
        r1Proxy.setSingle(m1);
        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        transaction.destroyModule(factory.getImplementationName(), "middle1");
        try {
            transaction.commit();
            fail("Validation exception expected");
        } catch (ValidationException e) {
            assertThat(e.getFailedValidations().keySet(), CoreMatchers.hasItem("multiple-dependencies"));
        }
    }
}
