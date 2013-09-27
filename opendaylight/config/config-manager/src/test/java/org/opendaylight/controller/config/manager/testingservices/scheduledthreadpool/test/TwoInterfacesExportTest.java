/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import javax.annotation.Nullable;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.junit.Test;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPConfigMXBean;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.TestingScheduledThreadPoolImpl;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool
        .TestingScheduledThreadPoolModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

public class TwoInterfacesExportTest extends AbstractScheduledTest {

    private void assertExists(String moduleName, String instanceName)
            throws Exception {
        assertExists(null, moduleName, instanceName);
    }

    private void assertExists(@Nullable ConfigTransactionJMXClient transaction,
            String moduleName, String instanceName)
            throws InstanceNotFoundException {
        if (transaction != null) {
            transaction.lookupConfigBean(moduleName, instanceName);
            // make a dummy call
            configRegistryClient.newMBeanProxy(
                    ObjectNameUtil.createTransactionModuleON(
                            transaction.getTransactionName(), moduleName,
                            instanceName), DynamicMBean.class).getMBeanInfo();
        } else {
            configRegistryClient.lookupConfigBean(moduleName, instanceName);
            // make a dummy call
            configRegistryClient.newMBeanProxy(
                    ObjectNameUtil.createReadOnlyModuleON(moduleName,
                            instanceName), DynamicMBean.class).getMBeanInfo();
        }
    }

    private void assertNotExists(String moduleName, String instanceName) {
        assertNotExists(null, moduleName, instanceName);
    }

    private void assertNotExists(
            @Nullable ConfigTransactionJMXClient transaction,
            String moduleName, String instanceName) {

        if (transaction != null) {
            try {
                transaction.lookupConfigBean(moduleName, instanceName);
                fail();
            } catch (InstanceNotFoundException e) {

            }
        } else {
            try {
                configRegistryClient.lookupConfigBean(moduleName, instanceName);
                fail();
            } catch (InstanceNotFoundException e) {

            }
        }
    }

    @Test
    public void twoInterfaceNamesAfterCreatingConfigBean() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();

        // create using TestingThreadPoolIfc:
        ObjectName scheduled1name = transaction.createModule(
                TestingScheduledThreadPoolModuleFactory.NAME, scheduled1);

        ObjectName retrievedName = transaction.lookupConfigBean(
                TestingScheduledThreadPoolModuleFactory.NAME, scheduled1);
        assertEquals(scheduled1name, retrievedName);

        // getExistingConfigBean should resolve moduleName
        String moduleName = TestingScheduledThreadPoolModuleFactory.NAME;
        retrievedName = transaction.lookupConfigBean(moduleName, scheduled1);
        ObjectName expected = ObjectNameUtil.createTransactionModuleON(
                transaction.getTransactionName(), moduleName, scheduled1);
        assertEquals(expected, retrievedName);

        // commit
        transaction.commit();
        assertEquals(1, TestingScheduledThreadPoolImpl.allExecutors.size());
        assertFalse(TestingScheduledThreadPoolImpl.allExecutors.get(0)
                .isTerminated());
        assertEquals(0,
                TestingScheduledThreadPoolImpl.getNumberOfCloseMethodCalls());

        assertExists(moduleName, scheduled1);

        // destroy using ThreadPool ifc
        transaction = configRegistryClient.createTransaction();
        transaction.destroyModule(ObjectNameUtil.createTransactionModuleON(
                transaction.getTransactionName(), moduleName, scheduled1));
        transaction.commit();
        assertEquals(1, TestingScheduledThreadPoolImpl.allExecutors.size());
        assertTrue(TestingScheduledThreadPoolImpl.allExecutors.get(0)
                .isTerminated());
        assertEquals(1,
                TestingScheduledThreadPoolImpl.getNumberOfCloseMethodCalls());

        // should not be in platform:

        assertNotExists(moduleName, scheduled1);

        transaction = configRegistryClient.createTransaction();
        // should not be in transaction
        assertNotExists(transaction, moduleName, scheduled1);
    }

    @Test
    public void tryToRegisterThreadPoolWithSameName()
            throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();

        transaction.createModule(TestingScheduledThreadPoolModuleFactory.NAME,
                scheduled1);
        try {
            transaction.createModule(
                    TestingScheduledThreadPoolModuleFactory.NAME, scheduled1);
            fail();
        } catch (InstanceAlreadyExistsException e) {
            assertThat(
                    e.getMessage(),
                    containsString("There is an instance registered with name ModuleIdentifier{factoryName='scheduled', instanceName='scheduled1'}"));
        }
    }

    // --
    @Test
    public void testRegisteringAllIfcNames() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        transaction.createModule(TestingScheduledThreadPoolModuleFactory.NAME,
                scheduled1);
        transaction.commit();
        assertExists(TestingScheduledThreadPoolModuleFactory.NAME, scheduled1);
        // another transaction
        transaction = configRegistryClient.createTransaction();
        assertExists(transaction, TestingScheduledThreadPoolModuleFactory.NAME,
                scheduled1);
    }

    @Test
    public void testWithAPSP_useScheduledNames()
            throws InstanceAlreadyExistsException, ValidationException {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        ObjectName scheduledName = transaction.createModule(
                TestingScheduledThreadPoolModuleFactory.NAME, scheduled1);

        ObjectName apspName = transaction.createModule(
                TestingParallelAPSPModuleFactory.NAME, "apsp1");
        TestingParallelAPSPConfigMXBean apspProxy = transaction.newMBeanProxy(
                apspName, TestingParallelAPSPConfigMXBean.class);
        apspProxy.setThreadPool(scheduledName);
        apspProxy.setSomeParam("someParam");
        transaction.validateConfig();

    }

    @Test
    public void testWithAPSP_useIfcNameMismatch() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        transaction.createModule(TestingScheduledThreadPoolModuleFactory.NAME,
                scheduled1);

        ObjectName apspName = transaction.createModule(
                TestingParallelAPSPModuleFactory.NAME, "apsp1");
        TestingParallelAPSPConfigMXBean apspProxy = transaction.newMBeanProxy(
                apspName, TestingParallelAPSPConfigMXBean.class);
        apspProxy.setThreadPool(ObjectNameUtil.createReadOnlyModuleON(
                TestingScheduledThreadPoolModuleFactory.NAME, scheduled1));
        apspProxy.setSomeParam("someParam");
        transaction.validateConfig();
        transaction.commit();

    }

}
