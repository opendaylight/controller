package org.opendaylight.controller.config.threadpool.eventbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.threadpool.impl.EventBusModuleFactory;

public class SyncEventBusConfigBeanTest extends AbstractConfigTest {

    private EventBusModuleFactory factory;
    private final String instanceName = "sync1";

    @Before
    public void setUp() {

        factory = new EventBusModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(factory));
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException, ValidationException,
            ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        createSynced(transaction, instanceName);
        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertEquals(1, configRegistry.lookupConfigBeans(factory.getImplementationName()).size());
        assertEquals(1, status.getNewInstances().size());
        assertEquals(0, status.getRecreatedInstances().size());
        assertEquals(0, status.getReusedInstances().size());
        // TODO test dead event collector
    }

    @Test
    public void testReusingOldInstance() throws InstanceAlreadyExistsException, ConflictingVersionException,
            ValidationException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        createSynced(transaction, instanceName);

        transaction.commit();

        assertEquals(1, configRegistry.lookupConfigBeans(factory.getImplementationName()).size());

        transaction = configRegistryClient.createTransaction();
        CommitStatus status = transaction.commit();

        assertEquals(1, configRegistry.lookupConfigBeans(factory.getImplementationName()).size());
        assertEquals(0, status.getNewInstances().size());
        assertEquals(0, status.getRecreatedInstances().size());
        assertEquals(1, status.getReusedInstances().size());

    }

    @Test
    public void testInstanceAlreadyExistsException() throws ConflictingVersionException, ValidationException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();

        try {
            createSynced(transaction, instanceName);
            transaction.commit();
        } catch (InstanceAlreadyExistsException e1) {
            fail();
        }

        transaction = configRegistryClient.createTransaction();
        try {
            createSynced(transaction, instanceName);
            fail();
        } catch (InstanceAlreadyExistsException e) {
            assertThat(
                    e.getMessage(),
                    containsString("There is an instance registered with name ModuleIdentifier{factoryName='eventbus', instanceName='sync1'}"));
        }
    }

    private ObjectName createSynced(ConfigTransactionJMXClient transaction, String instanceName)
            throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), instanceName);
        return nameCreated;
    }
}
