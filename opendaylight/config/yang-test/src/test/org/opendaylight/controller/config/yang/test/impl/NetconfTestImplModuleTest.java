package org.opendaylight.controller.config.yang.test.impl;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;

public class NetconfTestImplModuleTest  extends AbstractConfigTest {

    public static final String TESTING_DEP_PREFIX = "testing-dep";
    private NetconfTestImplModuleFactory factory;
    private final String instanceName = "n1";

    @Before
    public void setUp() {

        factory = new NetconfTestImplModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(factory,
                new DepTestImplModuleFactory()));
    }

    @Test
    public void testDependencyList() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        ObjectName on = createInstance(transaction, instanceName, 4);
        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertBeanCount(4 + 1, DepTestImplModuleFactory.NAME);
        assertStatus(status, 1 + 4 + 1, 0, 0);

        transaction = configRegistryClient.createTransaction();

        NetconfTestImplModuleMXBean proxy = transaction.newMXBeanProxy(ObjectNameUtil.withoutTransactionName(on),
                NetconfTestImplModuleMXBean.class);
        proxy.getComplexList();
        List<ObjectName> testingDeps = proxy.getTestingDeps();
        ObjectName testingDep = proxy.getTestingDep();

        Assert.assertEquals(TESTING_DEP_PREFIX, ObjectNameUtil.getInstanceName(testingDep));
        assertTestingDeps(testingDeps, 4);

        transaction.abortConfig();
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
