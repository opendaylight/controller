/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.test.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.test.types.rev131127.TestIdentity1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.test.types.rev131127.TestIdentity2;

public class NetconfTestImplModuleTest  extends AbstractConfigTest {

    public static final String TESTING_DEP_PREFIX = "testing-dep";
    private NetconfTestImplModuleFactory factory;
    private final String instanceName = "n1";

    @Before
    public void setUp() {

        this.factory = new NetconfTestImplModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(this.mockedContext,this.factory,
                new DepTestImplModuleFactory(), new IdentityTestModuleFactory()));
    }

    @Override
    protected BindingRuntimeContext getBindingRuntimeContext() {
        final BindingRuntimeContext ret = super.getBindingRuntimeContext();
        doReturn(TestIdentity1.class).when(ret).getIdentityClass(TestIdentity1.QNAME);
        doReturn(TestIdentity2.class).when(ret).getIdentityClass(TestIdentity2.QNAME);
        return ret;
    }

    @Test
    public void testIdentities() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();

        final ObjectName nameCreated = transaction.createModule(IdentityTestModuleFactory.NAME, this.instanceName);
        final IdentityTestModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, IdentityTestModuleMXBean.class);

        final IdentitiesContainer c = new IdentitiesContainer();
        c.setAfi(new IdentityAttributeRef(TestIdentity2.QNAME.toString()));
        mxBean.setIdentitiesContainer(c);
        transaction.commit();
    }

    @Test
    public void testDependencyList() throws Exception {
        ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();

        final ObjectName on = createInstance(transaction, this.instanceName, 4);
        transaction.validateConfig();
        final CommitStatus status1 = transaction.commit();

        assertBeanCount(1, this.factory.getImplementationName());
        assertBeanCount(4 + 1, DepTestImplModuleFactory.NAME);
        assertStatus(status1, 1 + 4 + 1, 0, 0);

        transaction = this.configRegistryClient.createTransaction();

        final NetconfTestImplModuleMXBean proxy = transaction.newMXBeanProxy(ObjectNameUtil.withoutTransactionName(on),
                NetconfTestImplModuleMXBean.class);
        proxy.getComplexList();
        final List<ObjectName> testingDeps = proxy.getTestingDeps();
        final ObjectName testingDep = proxy.getTestingDep();

        assertEquals(TESTING_DEP_PREFIX, ObjectNameUtil.getInstanceName(testingDep));
        assertTestingDeps(testingDeps, 4);

        transaction.abortConfig();

        // check that reuse logic works - equals on list of dependencies.
        transaction = this.configRegistryClient.createTransaction();
        final CommitStatus status2 = transaction.commit();
        assertStatus(status2, 0, 0, 6);

        // replace single dependency
        transaction = this.configRegistryClient.createTransaction();
        final String instanceName1 = TESTING_DEP_PREFIX + 1;
        transaction.destroyModule(DepTestImplModuleFactory.NAME, instanceName1);
        transaction.createModule(DepTestImplModuleFactory.NAME, instanceName1);
        final CommitStatus status3 = transaction.commit();
        assertStatus(status3, 1, 1, 4);

    }

    @Test
    public void testNullCheckInListOfDependencies() throws Exception {
        final ConfigTransactionJMXClient transaction = this.configRegistryClient.createTransaction();

        final ObjectName on = createInstance(transaction, this.instanceName, 4);
        final NetconfTestImplModuleMXBean proxy = transaction.newMXBeanProxy(on, NetconfTestImplModuleMXBean.class);
        proxy.setTestingDeps(null);
        assertTrue(proxy.getTestingDeps().isEmpty());
        proxy.setTestingDeps(Collections.<ObjectName>emptyList());
    }

    private void assertTestingDeps(final List<ObjectName> testingDeps, final int i) {
        assertEquals(i, testingDeps.size());

        int c = 1;
        for (final ObjectName testingDep : testingDeps) {
            assertEquals(TESTING_DEP_PREFIX + Integer.toString(c++), ObjectNameUtil.getInstanceName(testingDep));
        }
    }


    private ObjectName createInstance(final ConfigTransactionJMXClient transaction, final String instanceName, final int depsCount)
            throws InstanceAlreadyExistsException {
        final ObjectName nameCreated = transaction.createModule(this.factory.getImplementationName(), instanceName);
        final NetconfTestImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, NetconfTestImplModuleMXBean.class);

        ObjectName dep = transaction.createModule(DepTestImplModuleFactory.NAME, TESTING_DEP_PREFIX);
        mxBean.setTestingDep(dep);

        final ArrayList<ObjectName> testingDeps = Lists.newArrayList();
        for (int i = 0; i < depsCount; i++) {
            dep = transaction.createModule(DepTestImplModuleFactory.NAME, TESTING_DEP_PREFIX + Integer.toString(i + 1));
            testingDeps.add(dep);
        }
        mxBean.setTestingDeps(testingDeps);

        return nameCreated;
    }

}
