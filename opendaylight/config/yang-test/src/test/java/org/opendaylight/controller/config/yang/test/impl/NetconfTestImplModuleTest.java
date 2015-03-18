/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.test.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.test.types.rev131127.TestIdentity1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.test.types.rev131127.TestIdentity2;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;

public class NetconfTestImplModuleTest  extends AbstractConfigTest {

    public static final String TESTING_DEP_PREFIX = "testing-dep";
    private NetconfTestImplModuleFactory factory;
    private final String instanceName = "n1";

    @Before
    public void setUp() {

        factory = new NetconfTestImplModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext,factory,
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
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        ObjectName nameCreated = transaction.createModule(IdentityTestModuleFactory.NAME, instanceName);
        IdentityTestModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, IdentityTestModuleMXBean.class);

        final IdentitiesContainer c = new IdentitiesContainer();
        c.setAfi(new IdentityAttributeRef(TestIdentity2.QNAME.toString()));
        mxBean.setIdentitiesContainer(c);
        transaction.commit();
    }

    @Test
    public void testDependencyList() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        ObjectName on = createInstance(transaction, instanceName, 4);
        transaction.validateConfig();
        CommitStatus status1 = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertBeanCount(4 + 1, DepTestImplModuleFactory.NAME);
        assertStatus(status1, 1 + 4 + 1, 0, 0);

        transaction = configRegistryClient.createTransaction();

        NetconfTestImplModuleMXBean proxy = transaction.newMXBeanProxy(ObjectNameUtil.withoutTransactionName(on),
                NetconfTestImplModuleMXBean.class);
        proxy.getComplexList();
        List<ObjectName> testingDeps = proxy.getTestingDeps();
        ObjectName testingDep = proxy.getTestingDep();

        assertEquals(TESTING_DEP_PREFIX, ObjectNameUtil.getInstanceName(testingDep));
        assertTestingDeps(testingDeps, 4);

        transaction.abortConfig();

        // check that reuse logic works - equals on list of dependencies.
        transaction = configRegistryClient.createTransaction();
        CommitStatus status2 = transaction.commit();
        assertStatus(status2, 0, 0, 6);

        // replace single dependency
        transaction = configRegistryClient.createTransaction();
        String instanceName1 = TESTING_DEP_PREFIX + 1;
        transaction.destroyModule(DepTestImplModuleFactory.NAME, instanceName1);
        transaction.createModule(DepTestImplModuleFactory.NAME, instanceName1);
        CommitStatus status3 = transaction.commit();
        assertStatus(status3, 1, 1, 4);

    }

    @Test
    public void testNullCheckInListOfDependencies() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        ObjectName on = createInstance(transaction, instanceName, 4);
        NetconfTestImplModuleMXBean proxy = transaction.newMXBeanProxy(on, NetconfTestImplModuleMXBean.class);
        try{
            proxy.setTestingDeps(null);
            fail();
        }catch(RuntimeException e) {
            Throwable cause = e.getCause();
            assertNotNull(cause);
            assertTrue("Invalid type " + cause, cause instanceof IllegalArgumentException);
            assertEquals("Null not supported", cause.getMessage());
        }
        proxy.setTestingDeps(Collections.<ObjectName>emptyList());
    }

    private void assertTestingDeps(List<ObjectName> testingDeps, int i) {
        assertEquals(i, testingDeps.size());

        int c = 1;
        for (ObjectName testingDep : testingDeps) {
            assertEquals(TESTING_DEP_PREFIX + Integer.toString(c++), ObjectNameUtil.getInstanceName(testingDep));
        }
    }


    private ObjectName createInstance(ConfigTransactionJMXClient transaction, String instanceName, int depsCount)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), instanceName);
        NetconfTestImplModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, NetconfTestImplModuleMXBean.class);

        ObjectName dep = transaction.createModule(DepTestImplModuleFactory.NAME, TESTING_DEP_PREFIX);
        mxBean.setTestingDep(dep);

        ArrayList<ObjectName> testingDeps = Lists.newArrayList();
        for (int i = 0; i < depsCount; i++) {
            dep = transaction.createModule(DepTestImplModuleFactory.NAME, TESTING_DEP_PREFIX + Integer.toString(i + 1));
            testingDeps.add(dep);
        }
        mxBean.setTestingDeps(testingDeps);

        return nameCreated;
    }

}
