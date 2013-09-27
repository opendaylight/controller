/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool
        .TestingScheduledThreadPoolConfigBeanMXBean;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool
        .TestingScheduledThreadPoolModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * TestingScheduledThreadPool exports 2 interfaces: <br>
 * {@link org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.TestingScheduledThreadPoolModuleFactory#NAME}
 * ,<br>
 * {@link org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolModuleFactory#NAME}
 * <br>
 * <br>
 * It also exports 2 runtime beans, one default and one with additional
 * properties of {'a':'b'}.
 */
public class RuntimeBeanTest extends AbstractScheduledTest {

    ObjectName ifc1runtimeON1 = ObjectNameUtil.createRuntimeBeanName(
            TestingScheduledThreadPoolModuleFactory.NAME, scheduled1,
            Maps.<String, String> newHashMap());
    // additional runtime bean
    ObjectName ifc1runtimeON2 = ObjectNameUtil.createRuntimeBeanName(
            TestingScheduledThreadPoolModuleFactory.NAME, scheduled1,
            ImmutableMap.of("a", "b"));

    List<ObjectName> allObjectNames = Lists.newArrayList(ifc1runtimeON1,
            ifc1runtimeON2);

    private ObjectName createScheduled() throws InstanceAlreadyExistsException,
            ConflictingVersionException, ValidationException {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();

        // create using TestingThreadPoolIfc:
        ObjectName createdConfigBean = transaction.createModule(
                TestingScheduledThreadPoolModuleFactory.NAME, scheduled1);
        // commit
        transaction.commit();
        return createdConfigBean;
    }

    @Test
    public void testCreateScheduled() throws Exception {
        createScheduled();
        checkRuntimeBeans();
    }

    private void checkRuntimeBeans() throws Exception {
        // check runtime bean - on 2 places
        for (ObjectName on : allObjectNames)
            checkRuntimeBean(on);
    }

    private void checkRuntimeBean(ObjectName on) throws Exception {
        assertEquals(0,
                platformMBeanServer.getAttribute(on, "ActualNumberOfThreads"));
    }

    private void checkRuntimeBeanDoesNotExist(ObjectName on) throws Exception {
        try {
            checkRuntimeBean(on);
            fail();
        } catch (InstanceNotFoundException e) {

        }
    }

    @Test
    public void testLookup() throws Exception {
        createScheduled();
        assertEquals(Sets.newHashSet(ifc1runtimeON1, ifc1runtimeON2),
                configRegistryClient.lookupRuntimeBeans());
    }

    @Test
    public void testReuse() throws Exception {
        ObjectName createdConfigBean = createScheduled();
        // empty transaction
        CommitStatus commitInfo = configRegistryClient.createTransaction()
                .commit();

        // check that it was reused
        ObjectName readableConfigBean = ObjectNameUtil
                .withoutTransactionName(createdConfigBean);
        List<ObjectName> newInstances = Collections.<ObjectName> emptyList();
        List<ObjectName> reusedInstances = Lists
                .newArrayList(readableConfigBean);
        List<ObjectName> recreatedInstaces = Collections
                .<ObjectName> emptyList();
        assertEquals(new CommitStatus(newInstances, reusedInstances,
                recreatedInstaces), commitInfo);
        checkRuntimeBeans();
    }

    @Test
    public void testRecreate() throws Exception {
        ObjectName createdConfigBean = createScheduled();
        // empty transaction
        ConfigTransactionJMXClient configTransaction = configRegistryClient
                .createTransaction();
        ObjectName scheduledWritableON = configTransaction.lookupConfigBean(
                TestingScheduledThreadPoolModuleFactory.NAME, scheduled1);
        TestingScheduledThreadPoolConfigBeanMXBean scheduledWritableProxy = configTransaction
                .newMXBeanProxy(scheduledWritableON, TestingScheduledThreadPoolConfigBeanMXBean.class);
        scheduledWritableProxy.setRecreate(true);
        CommitStatus commitInfo = configTransaction.commit();
        // check that it was recreated
        ObjectName readableConfigBean = ObjectNameUtil
                .withoutTransactionName(createdConfigBean);
        List<ObjectName> newInstances = Collections.<ObjectName> emptyList();
        List<ObjectName> reusedInstances = Collections.<ObjectName> emptyList();
        List<ObjectName> recreatedInstaces = Lists
                .newArrayList(readableConfigBean);
        assertEquals(new CommitStatus(newInstances, reusedInstances,
                recreatedInstaces), commitInfo);
        checkRuntimeBeans();
    }

    @Test
    public void testDestroy_shouldUnregisterRuntimeBeans() throws Exception {
        ObjectName createdConfigBean = createScheduled();
        ConfigTransactionJMXClient configTransaction = configRegistryClient
                .createTransaction();
        configTransaction.destroyModule(ObjectNameUtil
                .createTransactionModuleON(configTransaction.getTransactionName(), createdConfigBean));
        configTransaction.commit();
        for (ObjectName on : allObjectNames)
            checkRuntimeBeanDoesNotExist(on);
    }

}
