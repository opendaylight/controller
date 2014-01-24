package org.opendaylight.controller.config.yang.test.impl;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NetconfTestImplModuleTest  extends AbstractConfigTest {

    public static final String TESTING_DEP_PREFIX = "testing-dep";
    private NetconfTestImplModuleFactory factory;
    private final String instanceName = "n1";

    @Before
    public void setUp() {

        factory = new NetconfTestImplModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(factory,
                new DepTestImplModuleFactory(), new IdentityTestModuleFactory()));
    }

    @Test
    public void testIdentities() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        ObjectName nameCreated = transaction.createModule(IdentityTestModuleFactory.NAME, instanceName);
        IdentityTestModuleMXBean mxBean = transaction.newMXBeanProxy(nameCreated, IdentityTestModuleMXBean.class);

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

        Assert.assertEquals(TESTING_DEP_PREFIX, ObjectNameUtil.getInstanceName(testingDep));
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
        Assert.assertEquals(i, testingDeps.size());

        int c = 1;
        for (ObjectName testingDep : testingDeps) {
            Assert.assertEquals(TESTING_DEP_PREFIX + Integer.toString(c++), ObjectNameUtil.getInstanceName(testingDep));
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
