/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dynamicmbean;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.JMX;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.manager.impl.AbstractLockedPlatformMBeanServerTest;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPool;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolConfigMXBean;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolModule;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolModuleFactory;
import org.opendaylight.controller.config.spi.Module;

import static org.junit.Assert.assertEquals;

public abstract class AbstractDynamicWrapperTest extends
        AbstractLockedPlatformMBeanServerTest {
    protected final MBeanServer platformMBeanServer = ManagementFactory
            .getPlatformMBeanServer();
    private static final String moduleName = "impl";
    protected final ObjectName threadPoolDynamicWrapperON = ObjectNameUtil
            .createReadOnlyModuleON(moduleName, "fixed1");
    protected static final String THREAD_COUNT = "ThreadCount";
    protected static final String TRIGGER_NEW_INSTANCE_CREATION = "TriggerNewInstanceCreation";

    protected final int threadCount = 5;
    protected TestingFixedThreadPoolModule threadPoolConfigBean;
    private static final ModuleIdentifier moduleIdentifier = new ModuleIdentifier(
            moduleName, "clientname2");

    protected MBeanServer internalServer;

    @Before
    public void registerToJMX() throws Exception {
        internalServer = MBeanServerFactory.createMBeanServer();
        TestingFixedThreadPoolModuleFactory testingFixedThreadPoolConfigBeanFactory = new TestingFixedThreadPoolModuleFactory();
        threadPoolConfigBean = testingFixedThreadPoolConfigBeanFactory
                .createModule("", null, null);

        threadPoolConfigBean.setThreadCount(threadCount);
        AbstractDynamicWrapper dynamicWrapper = getDynamicWrapper(
                threadPoolConfigBean, moduleIdentifier);
        platformMBeanServer.registerMBean(dynamicWrapper,
                threadPoolDynamicWrapperON);
    }

    @After
    public void unregisterFromJMX() throws Exception {
        TestingFixedThreadPool.cleanUp();
        platformMBeanServer.unregisterMBean(threadPoolDynamicWrapperON);
        MBeanServerFactory.releaseMBeanServer(internalServer);
    }

    protected abstract AbstractDynamicWrapper getDynamicWrapper(Module module,
            ModuleIdentifier moduleIdentifier);

    @Test
    public void testReadAttributes() throws Exception {

        DynamicMBean proxy = JMX.newMBeanProxy(platformMBeanServer,
                threadPoolDynamicWrapperON, DynamicMBean.class);

        assertEquals(threadCount, proxy.getAttribute(THREAD_COUNT));

        assertEquals(threadPoolConfigBean.isTriggerNewInstanceCreation(),
                proxy.getAttribute(TRIGGER_NEW_INSTANCE_CREATION));

        AttributeList attributes = proxy.getAttributes(new String[] {
                THREAD_COUNT, TRIGGER_NEW_INSTANCE_CREATION });
        assertEquals(2, attributes.size());
        Attribute threadCountAttr = (Attribute) attributes.get(0);
        assertEquals(THREAD_COUNT, threadCountAttr.getName());
        assertEquals(threadCount, threadCountAttr.getValue());
        Attribute boolTestAttr = (Attribute) attributes.get(1);
        assertEquals(TRIGGER_NEW_INSTANCE_CREATION, boolTestAttr.getName());
        assertEquals(threadPoolConfigBean.isTriggerNewInstanceCreation(),
                boolTestAttr.getValue());

        MBeanInfo mBeanInfo = proxy.getMBeanInfo();
        assertEquals(2, mBeanInfo.getAttributes().length);

    }

    @Test
    public void testGettersWithMXBeanProxy() {
        TestingFixedThreadPoolConfigMXBean proxy = JMX.newMXBeanProxy(
                platformMBeanServer, threadPoolDynamicWrapperON,
                TestingFixedThreadPoolConfigMXBean.class);
        assertEquals(threadCount, proxy.getThreadCount());
    }

}
