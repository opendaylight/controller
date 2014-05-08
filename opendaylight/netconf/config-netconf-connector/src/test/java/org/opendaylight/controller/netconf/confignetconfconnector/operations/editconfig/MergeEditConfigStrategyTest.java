/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.config.api.jmx.ObjectNameUtil.createReadOnlyModuleON;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.test.impl.MultipleDependenciesModule;
import org.opendaylight.controller.config.yang.test.impl.MultipleDependenciesModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.MultipleDependenciesModuleMXBean;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml.AttributeConfigElement;
import org.opendaylight.controller.netconf.confignetconfconnector.mapping.config.ServiceRegistryWrapper;

public class MergeEditConfigStrategyTest extends AbstractConfigTest {
    private static final MultipleDependenciesModuleFactory factory = new MultipleDependenciesModuleFactory();
    public static final String PARENT = "parent";
    public static final String D1 = "d1";
    public static final String D2 = "d2";
    public static final String D3 = "d3";

    private static final String factoryName = factory.getImplementationName();

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, factory));

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        ObjectName d1 = transaction.createModule(factoryName, D1);
        ObjectName d2 = transaction.createModule(factoryName, D2);
        ObjectName parent = transaction.createModule(factoryName, PARENT);
        MultipleDependenciesModuleMXBean multipleDependenciesModuleMXBean = transaction.newMXBeanProxy(parent,
                MultipleDependenciesModuleMXBean.class);
        multipleDependenciesModuleMXBean.setTestingDeps(asList(d1, d2));
        transaction.createModule(factoryName, D3);
        transaction.commit();
    }

    @Test
    public void testMergingOfObjectNames() throws Exception {
        MergeEditConfigStrategy strategy = new MergeEditConfigStrategy();
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        // add D3

        AttributeConfigElement attributeConfigElement = mock(AttributeConfigElement.class);
        doReturn(Optional.of(new ObjectName[] {createReadOnlyModuleON(factoryName, D3)})).when(attributeConfigElement).getResolvedValue();
        doReturn("mocked").when(attributeConfigElement).toString();
        String attributeName = MultipleDependenciesModule.testingDepsJmxAttribute.getAttributeName();
        doReturn(attributeName).when(attributeConfigElement).getJmxName();
        Map<String, AttributeConfigElement> configuration = ImmutableMap.of(
                attributeName,
                attributeConfigElement);

        strategy.executeConfiguration(factoryName, PARENT, configuration, transaction,
                mock(ServiceRegistryWrapper.class));
        transaction.commit();

        // parent's attribute should contain d1,d2,d3
        MultipleDependenciesModuleMXBean proxy = configRegistryClient.newMXBeanProxy(
                createReadOnlyModuleON(factoryName, PARENT),
                MultipleDependenciesModuleMXBean.class);
        List<ObjectName> testingDeps = proxy.getTestingDeps();
        List<ObjectName> expected = asList(createReadOnlyModuleON(factoryName, D1),
                createReadOnlyModuleON(factoryName, D2),
                createReadOnlyModuleON(factoryName, D3));
        assertEquals(expected, testingDeps);
    }
}
