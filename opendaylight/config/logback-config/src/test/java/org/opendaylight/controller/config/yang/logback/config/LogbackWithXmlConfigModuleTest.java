/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.config;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.ObjectName;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class LogbackWithXmlConfigModuleTest extends AbstractConfigTest {

    private LogbackModuleFactory factory;
    private LoggerContext lc;

    @Before
    public void setUp() throws JoranException, IOException {

        factory = new LogbackModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(factory));

        lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        lc.reset();
        configurator.setContext(lc);
        configurator.doConfigure("src/test/resources/simple_config_logback.xml");
        File f = new File("target/it");
        if (f.exists())
            FileUtils.cleanDirectory(f);
    }

    /**
     * Tests configuration of Logger factory.
     */
    @Test
    public void test() throws InstanceAlreadyExistsException, InstanceNotFoundException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), "singleton");

        LogbackModuleMXBean bean = transaction.newMXBeanProxy(nameCreated, LogbackModuleMXBean.class);

        assertEquals(1, bean.getConsoleAppenderTO().size());

        assertEquals(1, bean.getRollingFileAppenderTO().size());

        transaction.commit();

        transaction = configRegistryClient.createTransaction();

        nameCreated = transaction.lookupConfigBean(factory.getImplementationName(), "singleton");

        bean = JMX.newMXBeanProxy(platformMBeanServer, nameCreated, LogbackModuleMXBean.class);

        assertEquals(1, bean.getConsoleAppenderTO().size());
        assertEquals(1, bean.getRollingFileAppenderTO().size());

    }

    /**
     * Tests filtering loggers. Loggers inherited from ROOT logger and duplicate
     * loggers should be removed.
     */
    @Test
    public void testAllLoggers() throws InstanceAlreadyExistsException, InstanceNotFoundException {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        transaction.createModule(factory.getImplementationName(), "singleton");

        transaction.commit();

        transaction = configRegistryClient.createTransaction();

        LogbackModuleMXBean bean = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(),
                transaction.lookupConfigBean("logback", "singleton"), LogbackModuleMXBean.class);

        assertEquals(5, bean.getLoggerTO().size());
    }

    /**
     * Add new logger using FileAppender
     */
    @Test
    public void testAddNewLogger() throws InstanceAlreadyExistsException, InstanceNotFoundException {

        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        ObjectName nameCreated = transaction.createModule(factory.getImplementationName(), "singleton");
        LogbackModuleMXBean bean = transaction.newMXBeanProxy(nameCreated, LogbackModuleMXBean.class);

        assertEquals(5, bean.getLoggerTO().size());

        List<LoggerTO> loggers = Lists.newArrayList(bean.getLoggerTO());
        LoggerTO logger = new LoggerTO();
        logger.setAppenders(Lists.newArrayList("FILE"));
        logger.setLevel("INFO");
        logger.setLoggerName("fileLogger");
        loggers.add(logger);
        bean.setLoggerTO(loggers);

        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        nameCreated = transaction.lookupConfigBean(factory.getImplementationName(), "singleton");
        bean = JMX.newMXBeanProxy(platformMBeanServer, nameCreated, LogbackModuleMXBean.class);

        assertEquals(6, bean.getLoggerTO().size());
    }

}
