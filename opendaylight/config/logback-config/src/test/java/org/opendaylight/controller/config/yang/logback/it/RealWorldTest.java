/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.logback.it;

import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import nu.xom.Document;
import nu.xom.Node;
import nu.xom.Nodes;
import org.junit.Test;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.logback.appender.AbstractAppenderModuleTest;
import org.opendaylight.controller.config.yang.logback.appender.ConsoleAppenderModuleFactory;
import org.opendaylight.controller.config.yang.logback.appender.FileAppenderModuleFactory;
import org.opendaylight.controller.config.yang.logback.appender.RollingFileAppenderModuleFactory;
import org.opendaylight.controller.config.yang.logback.config.LogbackModuleFactory;
import org.opendaylight.controller.config.yang.logback.config.LogbackModuleMXBean;
import org.opendaylight.controller.config.yang.logback.config.LogbackReconfigurator;
import org.opendaylight.controller.config.yang.logback.config.LoggerTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RealWorldTest extends AbstractAppenderModuleTest {
    private static final Logger logger = LoggerFactory.getLogger(RealWorldTest.class);

    @Override
    protected Collection<? extends ModuleFactory> getTestedFactories() {
        return asList(new ConsoleAppenderModuleFactory(), new FileAppenderModuleFactory(),
                new RollingFileAppenderModuleFactory(), new LogbackModuleFactory());
    }

    @Test
    public void test() throws Exception {
        logger.info("test0");
        resettingLogbackTestBase.reconfigureUsingClassPathFile("/real_world_logback.xml");
        {
            ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
            CommitStatus status = transaction.commit();
            assertStatus(status, 4, 0, 0);
        }

        LogbackReconfigurator logbackInstance = (LogbackReconfigurator)
                getInstanceFromCurrentConfig(new ModuleIdentifier(LogbackModuleFactory.NAME, LogbackModuleFactory.INSTANCE_NAME));
        Document logbackDocument = logbackInstance.getLogbackDocument();
        // check that there is 1 rolling file appender
        assertNodeIs("opendaylight.log", logbackDocument.query("/configuration/appender[@class='" + RollingFileAppender.class.getCanonicalName() + "']/@name"));
        // check that there is 1 file appender
        assertNodeIs("audit-file", logbackDocument.query("/configuration/appender[@class='" + FileAppender.class.getCanonicalName() + "']/@name"));
        // check that there is 1 console appender
        assertNodeIs("STDOUT", logbackDocument.query("/configuration/appender[@class='" + ConsoleAppender.class.getCanonicalName() + "']/@name"));
        // root logger is connected to STDOUT and opendaylight.log
        Node rootLoggerElement = logbackDocument.query("/configuration/root").get(0);
        assertNodeIs("ERROR", rootLoggerElement.query("@level"));
        assertEquals(2, rootLoggerElement.query("appender-ref").size());
        assertEquals(1, rootLoggerElement.query("appender-ref[@ref='STDOUT']").size());
        assertEquals(1, rootLoggerElement.query("appender-ref[@ref='opendaylight.log']").size());
        assertEquals(logbackDocument.toXML(), 5, logbackDocument.query("/configuration/logger").size());
        Nodes auditLoggerQuery = logbackDocument.query("/configuration/logger[@additivity='false'][@name='audit']");
        assertEquals(1, auditLoggerQuery.size());
        assertEquals(1, auditLoggerQuery.get(0).query("appender-ref").size());
        assertEquals(1, auditLoggerQuery.get(0).query("appender-ref[@ref='audit-file']").size());

        File logFile = new File("target/logs/opendaylight.log".replace("/", File.separator));
        assertTrue(logFile.exists());
        long length1 = logFile.length();
        logger.info("test1");
        long length2 = logFile.length();
        assertTrue("Something should be logged", length2 > length1);
        // change level of this package to warn
        {
            ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
            ObjectName on = transaction.lookupConfigBean(LogbackModuleFactory.NAME, LogbackModuleFactory.INSTANCE_NAME);
            LogbackModuleMXBean mxBean = transaction.newMXBeanProxy(on, LogbackModuleMXBean.class);
            List<LoggerTO> loggerTOs = mxBean.getLoggerTO();
            LoggerTO newLoggerTO = new LoggerTO();
            newLoggerTO.setLevel("WARN");
            newLoggerTO.setLoggerName(logger.getName());
            newLoggerTO.setAppenders(Collections.<String>emptyList());
            loggerTOs.add(newLoggerTO);
            mxBean.setLoggerTO(loggerTOs);


            CommitStatus status = transaction.commit();
            assertStatus(status, 0, 1, 3);
        }
        long length3 = logFile.length();
        logger.info("test2");
        assertEquals("Nothing should change", length3, logFile.length());
        logger.warn("test3");
        assertTrue(logFile.length() > length3);

    }
}
