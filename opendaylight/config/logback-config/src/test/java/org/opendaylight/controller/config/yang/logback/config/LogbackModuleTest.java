/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.config;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;

public class LogbackModuleTest extends AbstractConfigTest {

    private static final String INSTANCE_NAME = "singleton";

    private LogbackModuleFactory factory;

    @Before
    public void setUp() throws IOException, ClassNotFoundException,
            InterruptedException {

        factory = new LogbackModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(
                factory));
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException {

        CommitStatus status = createBeans(
        true, "target/rollingApp",
                "%-4relative [%thread] %-5level %logger{35} - %msg%n", "30MB",
                1, 5, "target/%i.log", "rolling", "consoleName", "ALL",
                "logger1", "DEBUG").commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 1, 0, 0);
    }

    @Test
    public void testReusingInstance() throws InstanceAlreadyExistsException {
        createBeans(
        true, "target/rollingApp",
                "%-4relative [%thread] %-5level %logger{35} - %msg%n", "30MB",
                1, 5, "target/%i.log", "rolling", "consoleName", "ALL",
                "logger1", "DEBUG").commit();

        assertBeanCount(1, factory.getImplementationName());

        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 0, 1);
    }

    @Test
    public void testRecreateInstance() throws InstanceAlreadyExistsException,
            ValidationException, ConflictingVersionException,
            InstanceNotFoundException {
        createBeans(
        true, "target/rollingApp",
                "%-4relative [%thread] %-5level %logger{35} - %msg%n", "30MB",
                1, 5, "target/%i.log", "rolling", "consoleName", "ALL",
                "logger1", "DEBUG").commit();

        assertBeanCount(1, LogbackModuleFactory.NAME);
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();

        ObjectName logback = transaction.lookupConfigBean(
                LogbackModuleFactory.NAME, "singleton");
        LogbackModuleMXBean nwBean = transaction.newMXBeanProxy(logback,
                LogbackModuleMXBean.class);
        CommitStatus status = transaction.commit();
        assertBeanCount(1, LogbackModuleFactory.NAME);

        assertStatus(status, 0, 0, 1);
    }

    @Test
    public void testDestroyInstance() throws InstanceNotFoundException,
            InstanceAlreadyExistsException {
        createBeans(
        true, "target/rollingApp",
                "%-4relative [%thread] %-5level %logger{35} - %msg%n", "30MB",
                1, 5, "target/%i.log", "rolling", "consoleName", "ALL",
                "logger1", "DEBUG").commit();
        assertBeanCount(1, factory.getImplementationName());

        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        transaction.destroyConfigBean(factory.getImplementationName(),
                INSTANCE_NAME);
        CommitStatus status = transaction.commit();

        assertBeanCount(0, factory.getImplementationName());
        assertStatus(status, 0, 0, 0);
    }

    @Ignore
    @Test
    public void testValidation1() throws InstanceAlreadyExistsException {
        try {
            createBeans(
            true, "target/rollingApp",
                    "%-4relative [%thread] %-5level %logger{35} - %msg%n",
                    "30MB", 1, 5, "target/%i.log", "rolling", "consoleName",
                    "ALL", "logger1", "DEBUG").commit();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getFailedValidations().toString(),
                    containsString("FileName is null"));
        }
    }

    @Test
    public void testValidation2() throws InstanceAlreadyExistsException {
        try {
            createBeans(
            true, "target/rollingApp", null, "30MB", 1, 5, "target/%i.log",
                    "rolling", "consoleName", "ALL", "logger1", "DEBUG")
                    .commit();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getFailedValidations().toString(),
                    containsString("EncoderPattern is null"));
        }
    }

    @Test
    public void testValidation4() throws InstanceAlreadyExistsException {
        try {
            createBeans(
            true, "target/rollingApp",
                    "%-4relative [%thread] %-5level %logger{35} - %msg%n",
                    null, 1, 5, "target/%i.log", "rolling", "consoleName",
                    "ALL", "logger1", "DEBUG").commit();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getFailedValidations().toString(),
                    containsString("MaxFileSize is null"));
        }
    }

    @Test
    public void testValidation6() throws InstanceAlreadyExistsException {
        try {
            createBeans(
            true, "", "%-4relative [%thread] %-5level %logger{35} - %msg%n",
                    "30MB", 1, 5, "target/%i.log", "rolling", "consoleName",
                    "ALL", "logger1", "DEBUG").commit();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getFailedValidations().toString(),
                    containsString("FileName needs to be set"));
        }
    }

    @Test
    public void testValidation7() throws InstanceAlreadyExistsException {
        try {
            createBeans(

            true, "target/rollingApp", "", "30MB", 1, 5, "target/%i.log",
                    "rolling", "consoleName", "ALL", "logger1", "DEBUG")
                    .commit();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getFailedValidations().toString(),
                    containsString("EncoderPattern needs to be set"));
        }
    }

    @Test
    public void testValidation8() throws InstanceAlreadyExistsException {
        try {
            createBeans(
            true, "target/rollingApp",
                    "%-4relative [%thread] %-5level %logger{35} - %msg%n",
                    "30MB", 1, 5, "target/%i.log", "rolling", "consoleName",
                    "ALL", null, "DEBUG").commit();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getFailedValidations().toString(),
                    containsString("LoggerName is null"));
        }
    }

    @Test
    public void testValidation9() throws InstanceAlreadyExistsException {
        try {
            createBeans(
            true, "target/rollingApp",
                    "%-4relative [%thread] %-5level %logger{35} - %msg%n",
                    "30MB", 1, 5, "target/%i.log", "rolling", "consoleName",
                    "ALL", "", "DEBUG").commit();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getFailedValidations().toString(),
                    containsString("LoggerName needs to be set"));
        }
    }

    @Test
    public void testValidation10() throws InstanceAlreadyExistsException {
        try {
            createBeans(
            true, "target/rollingApp",
                    "%-4relative [%thread] %-5level %logger{35} - %msg%n",
                    "30MB", null, 5, "target/%i.log", "rolling", "consoleName",
                    "ALL", "logger1", "DEBUG").commit();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getFailedValidations().toString(),
                    containsString("MinIndex is null"));
        }
    }

    @Test
    public void testValidation11() throws InstanceAlreadyExistsException {
        try {
            createBeans(
            true, "target/rollingApp",
                    "%-4relative [%thread] %-5level %logger{35} - %msg%n",
                    "30MB", 1, null, "target/%i.log", "rolling", "consoleName",
                    "ALL", "logger1", "DEBUG").commit();
            fail();
        } catch (ValidationException e) {
            assertThat(e.getFailedValidations().toString(),
                    containsString("MaxIndex is null"));
        }
    }

    private ConfigTransactionJMXClient createBeans(
    Boolean isAppend, String rollingFileName, String encoderPattern,
            String maxFileSize, Integer minIndex, Integer maxIndex,
            String fileNamePattern, String rollingName, String consoleName,
            String thresholdFilter, String loggerName, String level )
            throws InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        ObjectName nameCreated = transaction.createModule(
                factory.getImplementationName(), INSTANCE_NAME);
        LogbackModuleMXBean bean = transaction.newMXBeanProxy(nameCreated,
                LogbackModuleMXBean.class);

        List<RollingFileAppenderTO> rollingAppenders = new ArrayList<>();
        RollingFileAppenderTO rollingAppender = new RollingFileAppenderTO();
        rollingAppender.setAppend(isAppend);
        rollingAppender.setEncoderPattern(encoderPattern);
        rollingAppender.setFileName(rollingFileName);
        rollingAppender.setMaxFileSize(maxFileSize);
        rollingAppender.setMaxIndex(maxIndex);
        rollingAppender.setMinIndex(minIndex);
        rollingAppender.setFileNamePattern(fileNamePattern);
        rollingAppender.setName(rollingName);
        rollingAppenders.add(rollingAppender);

        List<ConsoleAppenderTO> consoleAppenders = new ArrayList<>();
        ConsoleAppenderTO consoleAppender = new ConsoleAppenderTO();
        consoleAppender.setEncoderPattern(encoderPattern);
        consoleAppender.setName(consoleName);
        consoleAppender.setThresholdFilter(thresholdFilter);
        consoleAppenders.add(consoleAppender);

        List<LoggerTO> loggers = new ArrayList<>();

        LoggerTO logger = new LoggerTO();

        logger.setAppenders(Arrays.<String> asList());

        logger.setLevel(level);
        logger.setLoggerName(loggerName);
        loggers.add(logger);
        bean.setLoggerTO(loggers);
        bean.setRollingFileAppenderTO(rollingAppenders);
        bean.setConsoleAppenderTO(consoleAppenders);

        transaction.validateConfig();

        return transaction;
    }

}
