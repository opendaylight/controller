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
import static org.opendaylight.controller.config.api.jmx.ObjectNameUtil.getInstanceName;
import static org.opendaylight.controller.config.api.jmx.ObjectNameUtil.getTransactionName;

import java.util.List;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
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
}
