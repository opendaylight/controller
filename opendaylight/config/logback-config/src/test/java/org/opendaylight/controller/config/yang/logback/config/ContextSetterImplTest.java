/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class ContextSetterImplTest {

    @Mock
    private LogbackRuntimeRegistrator runtimeRegistratorMock;
    @Mock
    private DependencyResolver dependencyResolverMock;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        LogbackRuntimeRegistration reg = mock(LogbackRuntimeRegistration.class);
        doReturn(reg).when(runtimeRegistratorMock).register(any(LogbackRuntimeMXBean.class));
    }

    @Test
    public void testUpdate() throws Exception {
        Multimap<String, String> loggersToAppenders = HashMultimap.create();
        loggersToAppenders.put("l1", "a1");
        loggersToAppenders.put("l1", "a2");
        createContextSetter(loggersToAppenders);

        assertLoggerWithAppenders("l1", "a1", "a2");
    }

    @Test
    public void testUpdateTwice() throws Exception {
        Multimap<String, String> loggersToAppenders = HashMultimap.create();
        loggersToAppenders.put("l1", "a1");
        loggersToAppenders.put("l1", "a2");
        createContextSetter(loggersToAppenders);

        loggersToAppenders.clear();
        loggersToAppenders.put("l1", "a3");
        loggersToAppenders.put("l1", "a2");
        loggersToAppenders.put("l1", "a4");
        createContextSetter(loggersToAppenders);

        assertLoggerWithAppenders("l1", "a2", "a3", "a4");
    }

    @Test
    public void testKeepOtherLoggers() throws Exception {
        Multimap<String, String> loggersToAppenders = HashMultimap.create();
        loggersToAppenders.put("l1", "a1");
        loggersToAppenders.put("l1", "a2");
        loggersToAppenders.put("l2", "a22");
        createContextSetter(loggersToAppenders);

        loggersToAppenders.clear();
        loggersToAppenders.put("l1", "a3");
        createContextSetter(loggersToAppenders);

        assertLoggerWithAppenders("l1", "a3");
        assertLoggerWithAppenders("l2", "a22");
    }

    private void createContextSetter(Multimap<String, String> loggersToAppenders) {
        ContextSetterImpl setter = new ContextSetterImpl(runtimeRegistratorMock);

        List<LoggerTO> logger = Lists.newArrayList();
        List<ConsoleAppenderTO> consoleAppenders = Lists.newArrayList();

        for (String loggerName : loggersToAppenders.keySet()) {
            LoggerTO l1 = createLogger(loggerName, loggersToAppenders.get(loggerName));
            logger.add(l1);
            for (String appenderName : loggersToAppenders.get(loggerName)) {
                consoleAppenders.add(createConsoleAppender(appenderName));
            }

        }

        LogbackModule logbackModule = createLogbackModule(logger, consoleAppenders);
        setter.updateContext(logbackModule);
    }

    private void assertLoggerWithAppenders(String name, String... appenders) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = context.getLogger(name);
        Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders();

        Multimap<String, Appender<?>> foundAppenders = HashMultimap.create();
        while (it.hasNext()) {
            final Appender<ILoggingEvent> app = it.next();
            foundAppenders.put(app.getName(), app);
        }

        if (appenders.length == 0) {
            assertEquals(0, foundAppenders.values().size());
        }

        for (String appender : appenders) {
            boolean isPresent = foundAppenders.get(appender).isEmpty();
            assertFalse("Appender " + appender + " for logger " + name + " was not present, present appenders: "
                    + foundAppenders.keys(), isPresent);
        }

    }

    private LogbackModule createLogbackModule(List<LoggerTO> logger, List<ConsoleAppenderTO> consoleAppenders) {
        LogbackModule logbackModule = new LogbackModule(new ModuleIdentifier("fact", "first"), dependencyResolverMock);
        logbackModule.setLoggerTO(logger);
        logbackModule.setConsoleAppenderTO(consoleAppenders);
        logbackModule.setRollingFileAppenderTO(Lists.<RollingFileAppenderTO> newArrayList());
        logbackModule.setFileAppenderTO(Lists.<FileAppenderTO> newArrayList());
        return logbackModule;
    }

    private LoggerTO createLogger(String name, Collection<String> appenders) {
        LoggerTO l1 = new LoggerTO();
        l1.setAppenders(Lists.newArrayList(appenders));
        l1.setLoggerName(name);
        l1.setLevel("INFO");
        return l1;
    }

    private ConsoleAppenderTO createConsoleAppender(String name) {
        ConsoleAppenderTO a = new ConsoleAppenderTO();
        a.setName(name);
        a.setEncoderPattern("%-4relative [%thread] %-5level %logger{35} - %msg%n");
        return a;
    }
}
