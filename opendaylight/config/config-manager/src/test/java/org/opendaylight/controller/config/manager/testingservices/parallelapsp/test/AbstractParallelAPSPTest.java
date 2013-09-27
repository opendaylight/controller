/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.testingservices.parallelapsp.test;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import org.opendaylight.controller.config.manager.impl.AbstractConfigWithJolokiaTest;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPConfigMXBean;
import org.opendaylight.controller.config.manager.testingservices.parallelapsp.TestingParallelAPSPModuleFactory;
import org.opendaylight.controller.config.manager.testingservices.threadpool.TestingFixedThreadPoolConfigMXBean;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

abstract class AbstractParallelAPSPTest extends AbstractConfigWithJolokiaTest {
    protected final String fixed1 = "fixed1";
    protected final String apsp1 = "apsp-parallel";

    abstract String getThreadPoolImplementationName();

    protected ObjectName createParallelAPSP(
            ConfigTransactionJMXClient transaction, ObjectName threadPoolON)
            throws InstanceAlreadyExistsException {
        ObjectName apspName = transaction.createModule(
                TestingParallelAPSPModuleFactory.NAME, apsp1);
        TestingParallelAPSPConfigMXBean parallelAPSPConfigProxy = transaction
                .newMXBeanProxy(apspName, TestingParallelAPSPConfigMXBean.class);
        parallelAPSPConfigProxy.setSomeParam("ahoj");
        parallelAPSPConfigProxy.setThreadPool(threadPoolON);
        return apspName;
    }

    protected ObjectName createFixed1(ConfigTransactionJMXClient transaction,
            int numberOfThreads) throws InstanceAlreadyExistsException {
        ObjectName name = transaction.createModule(
                getThreadPoolImplementationName(), fixed1);

        TestingFixedThreadPoolConfigMXBean fixedConfigProxy = transaction
                .newMXBeanProxy(name, TestingFixedThreadPoolConfigMXBean.class);
        fixedConfigProxy.setThreadCount(numberOfThreads);

        return name;
    }
}
