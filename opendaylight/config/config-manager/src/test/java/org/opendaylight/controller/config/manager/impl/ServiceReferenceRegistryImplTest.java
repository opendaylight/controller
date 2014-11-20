/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.config.api.jmx.ObjectNameUtil.withoutTransactionName;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.api.jmx.ServiceReferenceMXBean;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest.RecordingBundleContextServiceRegistrationHandler.RegistrationHolder;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.test.AbstractParallelAPSPTest;
import org.opendaylight.controller.config.manager.testingservices.scheduledthreadpool.TestingScheduledThreadPoolModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.seviceinterface.TestingThreadPoolServiceInterface;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingThreadPoolIfc;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

public class ServiceReferenceRegistryImplTest extends AbstractParallelAPSPTest {


    @Before
    public void setUp() {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(
                mockedContext,
                new TestingFixedThreadPoolModuleFactory(),
                new TestingParallelAPSPModuleFactory(),
                new TestingScheduledThreadPoolModuleFactory()));
    }

    @Override
    protected String getThreadPoolImplementationName() {
        return TestingFixedThreadPoolModuleFactory.NAME;
    }

    @Test
    public void test() throws Exception {
        ConfigTransactionJMXClient transaction1 = configRegistryClient.createTransaction();
        // create fixed1
        int fixedNrOfThreads = 20, scheduledNrOfThreads = 30;

        ObjectName fixedTPTransactionON = transaction1.createModule(getThreadPoolImplementationName(), fixed1);
        platformMBeanServer.setAttribute(fixedTPTransactionON, new Attribute("ThreadCount", fixedNrOfThreads));

        ObjectName scheduledTPTransactionON = transaction1.createModule(
                TestingScheduledThreadPoolModuleFactory.NAME, "scheduled1");
        platformMBeanServer.setAttribute(scheduledTPTransactionON, new Attribute("ThreadCount",
                scheduledNrOfThreads));

        String refName = "ref";
        ObjectName serviceReference = transaction1.saveServiceReference(TestingThreadPoolServiceInterface.QNAME, refName,
                fixedTPTransactionON);
        // create apsp-parallel
        createParallelAPSP(transaction1, serviceReference);
        transaction1.commit();

        // check fixed1 is used
        ServiceReferenceMXBean serviceReferenceMXBean = JMX.newMXBeanProxy(platformMBeanServer,
                withoutTransactionName(serviceReference), ServiceReferenceMXBean.class);
        assertEquals(withoutTransactionName(fixedTPTransactionON), serviceReferenceMXBean.getCurrentImplementation());
        checkApspThreadCount(fixedNrOfThreads);
        // check OSGi SR
        List<RegistrationHolder> registrations =
                ((RecordingBundleContextServiceRegistrationHandler) currentBundleContextServiceRegistrationHandler).getRegistrations();
        assertEquals(1, registrations.size());
        RegistrationHolder record = registrations.get(0);
        assertEquals(TestingThreadPoolIfc.class, record.clazz);
        assertEquals(ImmutableMap.of("name","ref"), (Map<String, String>) record.props);

        // switch reference to scheduled
        ConfigTransactionJMXClient transaction2 = configRegistryClient.createTransaction();
        transaction2.saveServiceReference(TestingThreadPoolServiceInterface.QNAME, refName,
                ObjectNameUtil.withTransactionName(scheduledTPTransactionON, transaction2.getTransactionName()));
        transaction2.commit();
        // check scheduled is used
        checkApspThreadCount(scheduledNrOfThreads);
        // check that dummy MXBean points to scheduled
        assertEquals(withoutTransactionName(scheduledTPTransactionON), serviceReferenceMXBean.getCurrentImplementation());

        // empty transaction
        configRegistryClient.createTransaction().commit();

        // get service mapping
        Map<String,Map<String,ObjectName>> serviceMapping = configRegistryClient.getServiceMapping();
        Map<String,Map<String,ObjectName>> expectedMapping = ImmutableMap.of(TestingThreadPoolServiceInterface.QNAME,
                (Map<String, ObjectName>)ImmutableMap.of(refName, withoutTransactionName(scheduledTPTransactionON)));
        assertEquals(expectedMapping, serviceMapping);

        // destroy all
        ConfigTransactionJMXClient transaction4 = configRegistryClient.createTransaction();
        Set<ObjectName> objectNames = transaction4.lookupConfigBeans();
        for(ObjectName on: objectNames) {
            transaction4.destroyModule(on);
        }
        transaction4.commit();

        serviceMapping = configRegistryClient.getServiceMapping();
        assertTrue(serviceMapping.isEmpty());
    }

    private void checkApspThreadCount(int fixedNrOfThreads) throws MBeanException, AttributeNotFoundException,
            InstanceNotFoundException, ReflectionException {
        ObjectName apspON = ObjectNameUtil.createReadOnlyModuleON(TestingParallelAPSPModuleFactory.NAME, apsp1);
        assertEquals(fixedNrOfThreads, platformMBeanServer.getAttribute(apspON, "MaxNumberOfThreads"));
    }
}
