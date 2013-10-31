/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dynamicmbean;

import org.junit.Test;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.TransactionIdentifier;
import org.opendaylight.controller.config.manager.impl.dynamicmbean.ReadOnlyAtomicBoolean.ReadOnlyAtomicBooleanImpl;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPConfigMXBean;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPModule;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolConfigMXBean;
import org.opendaylight.controller.config.spi.Module;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.JMX;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DynamicWritableWrapperTest extends AbstractDynamicWrapperTest {
    private final int newThreadCount = 10;
    private final AtomicBoolean atomicBoolean = new AtomicBoolean();
    private final ReadOnlyAtomicBoolean readOnlyAtomicBoolean = new ReadOnlyAtomicBooleanImpl(
            atomicBoolean);

    @Override
    protected AbstractDynamicWrapper getDynamicWrapper(Module module,
            ModuleIdentifier moduleIdentifier) {
        return new DynamicWritableWrapper(module, moduleIdentifier,
                new TransactionIdentifier("transaction-1"),
                readOnlyAtomicBoolean, MBeanServerFactory.createMBeanServer(),
                platformMBeanServer);
    }

    @Test
    public void testSetAttribute() throws Exception {
        DynamicMBean proxy = JMX.newMBeanProxy(platformMBeanServer,
                threadPoolDynamicWrapperON, DynamicMBean.class);

        proxy.setAttribute(new Attribute(THREAD_COUNT, newThreadCount));

        assertEquals(newThreadCount, proxy.getAttribute(THREAD_COUNT));
        assertEquals(newThreadCount, threadPoolConfigBean.getThreadCount());

        AttributeList attributeList = new AttributeList();
        attributeList.add(new Attribute(THREAD_COUNT, threadCount));
        boolean bool = true;
        attributeList.add(new Attribute(TRIGGER_NEW_INSTANCE_CREATION, bool));
        proxy.setAttributes(attributeList);

        assertEquals(threadCount, threadPoolConfigBean.getThreadCount());
        assertEquals(bool, threadPoolConfigBean.isTriggerNewInstanceCreation());
    }

    @Test
    public void testSettersWithMXBeanProxy() {
        TestingFixedThreadPoolConfigMXBean proxy = JMX.newMXBeanProxy(
                platformMBeanServer, threadPoolDynamicWrapperON,
                TestingFixedThreadPoolConfigMXBean.class);
        proxy.setThreadCount(newThreadCount);
        assertEquals(newThreadCount, threadPoolConfigBean.getThreadCount());
    }

    /*
     * Try to call setter with ObjectName containing transaction name. Verify
     * that ObjectName without transaction name was actually passed on the
     * config bean.
     */
    @Test
    public void testObjectNameSetterWithONContainingTransaction_shouldBeTranslatedToReadOnlyON()
            throws Exception {
        TestingParallelAPSPModuleFactory testingParallelAPSPConfigBeanFactory = new TestingParallelAPSPModuleFactory();
        TestingParallelAPSPModule apspConfigBean = testingParallelAPSPConfigBeanFactory
                .createModule("", null, null);
        ModuleIdentifier moduleIdentifier2 = new ModuleIdentifier("apsp",
                "parallel");
        ObjectName dynON2 = ObjectNameUtil
                .createReadOnlyModuleON(moduleIdentifier2);
        AbstractDynamicWrapper dyn = getDynamicWrapper(apspConfigBean,
                moduleIdentifier2);
        platformMBeanServer.registerMBean(dyn, dynON2);
        try {
            TestingParallelAPSPConfigMXBean proxy = JMX.newMBeanProxy(
                    platformMBeanServer, dynON2,
                    TestingParallelAPSPConfigMXBean.class);
            ObjectName withTransactionName = ObjectNameUtil
                    .createTransactionModuleON("transaction1", "moduleName", "instanceName");
            proxy.setThreadPool(withTransactionName);
            ObjectName withoutTransactionName = ObjectNameUtil
                    .withoutTransactionName(withTransactionName);
            assertEquals(withoutTransactionName, proxy.getThreadPool());
        } finally {
            platformMBeanServer.unregisterMBean(dynON2);
        }
    }

    private void setNumberOfThreads(int numberOfThreads) throws Exception {
        DynamicMBean proxy = JMX.newMBeanProxy(platformMBeanServer,
                threadPoolDynamicWrapperON, DynamicMBean.class);

        proxy.setAttribute(new Attribute(THREAD_COUNT, numberOfThreads));

    }

    @Test
    public void testDisablingOfWriteOperations() throws Exception {
        setNumberOfThreads(newThreadCount);
        atomicBoolean.set(true);
        try {
            setNumberOfThreads(newThreadCount);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Operation is not allowed now", e.getMessage());
        } finally {
            atomicBoolean.set(false);
        }

    }

}
