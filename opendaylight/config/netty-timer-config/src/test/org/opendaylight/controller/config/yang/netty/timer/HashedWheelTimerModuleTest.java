package org.opendaylight.controller.config.yang.netty.timer;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.threadpool.impl.NamingThreadFactoryModuleFactory;
import org.opendaylight.controller.config.yang.threadpool.impl.NamingThreadFactoryModuleMXBean;

public class HashedWheelTimerModuleTest extends AbstractConfigTest {

    private HashedWheelTimerModuleFactory factory;
    private NamingThreadFactoryModuleFactory threadFactory;
    private final String instanceName = "hashed-wheel-timer1";

    @Before
    public void setUp() {
        factory = new HashedWheelTimerModuleFactory();
        threadFactory = new NamingThreadFactoryModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(factory, threadFactory));
    }

    public void testValidationExceptionTickDuration() throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        try {
            createInstance(transaction, instanceName, 0L, 10, true);
            transaction.validateConfig();
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains("TickDuration value must be greater than 0"));
        }
    }

    public void testValidationExceptionTicksPerWheel() throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        try {
            createInstance(transaction, instanceName, 500L, 0, true);
            transaction.validateConfig();
            Assert.fail();
        } catch (ValidationException e) {
            Assert.assertTrue(e.getMessage().contains("TicksPerWheel value must be greater than 0"));
        }
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createInstance(transaction, instanceName, 500L, 10, true);
        createInstance(transaction, instanceName + 1, null, null, false);
        createInstance(transaction, instanceName + 2, 500L, 10, false);
        createInstance(transaction, instanceName + 3, 500L, null, false);
        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertBeanCount(4, factory.getImplementationName());
        assertStatus(status, 5, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction, instanceName, 500L, 10, true);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, factory.getImplementationName());
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 0, 2);
    }

    @Test
    public void testReconfigure() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException, InstanceNotFoundException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createInstance(transaction, instanceName, 500L, 10, true);
        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        assertBeanCount(1, factory.getImplementationName());
        HashedWheelTimerModuleMXBean mxBean = transaction.newMBeanProxy(
                transaction.lookupConfigBean(factory.getImplementationName(), instanceName),
                HashedWheelTimerModuleMXBean.class);
        mxBean.setTicksPerWheel(20);
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 1, 1);
    }

    private ObjectName createInstance(ConfigTransactionJMXClient transaction, String instanceName,
            final Long tickDuration, final Integer ticksPerWheel, final boolean hasThreadfactory)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), instanceName);
        HashedWheelTimerModuleMXBean mxBean = transaction
                .newMBeanProxy(nameCreated, HashedWheelTimerModuleMXBean.class);
        mxBean.setTickDuration(tickDuration);
        mxBean.setTicksPerWheel(ticksPerWheel);
        if (hasThreadfactory) {
            mxBean.setThreadFactory(createThreadfactoryInstance(transaction, "thread-factory1", "th"));
        }
        return nameCreated;
    }

    private ObjectName createThreadfactoryInstance(ConfigTransactionJMXClient transaction, String instanceName,
            final String namePrefix) throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(threadFactory.getImplementationName(), instanceName);
        NamingThreadFactoryModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated,
                NamingThreadFactoryModuleMXBean.class);
        mxBean.setNamePrefix(namePrefix);
        return nameCreated;
    }

}
