/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import org.junit.Test;
import org.opendaylight.controller.config.yang.logback.ResettingLogbackTestBase;
import org.opendaylight.controller.config.yang.logback.memoryappender.MemoryAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.config.yang.logback.appender.AbstractAppenderModuleTest.assertNodeIs;

public class LogbackReconfiguratorTest extends ResettingLogbackTestBase {
    private static final Logger logger = LoggerFactory.getLogger(LogbackReconfiguratorTest.class);

    @Test
    public void testCreateLogbackDocument() {
        LoggerTO rootTO = createSimpleRootLoggerTO();
        LoggerTO loggerTO = new LoggerTO();
        loggerTO.setLoggerName("foo");
        loggerTO.setLevel(Level.WARN.toString());
        LoggerTO loggerTO2 = new LoggerTO();
        loggerTO2.setLoggerName("foo2");
        loggerTO2.setLevel(Level.ERROR.toString());
        loggerTO2.setAdditivity(false);
        Document logbackDocument = LogbackReconfigurator.createLogbackDocument(Arrays.asList(getMemoryAppenderElement()),
                Arrays.asList(rootTO, loggerTO, loggerTO2));


        // <root level="INFO"><appender-ref ref="MEMORY" /></root>

        Node rootLoggerElement = logbackDocument.query("/configuration/root").get(0);
        assertNodeIs(Level.INFO.toString(), rootLoggerElement.query("@level"));
        Nodes rootAppenders = rootLoggerElement.query("appender-ref");
        assertEquals(1, rootAppenders.size());
        assertNodeIs("MEMORY", rootAppenders.get(0).query("@ref"));

        // <appender name="MEMORY" class="...MemoryAppender" />
        assertEquals(1, logbackDocument.query("/configuration/appender").size());
        assertNodeIs("MEMORY", logbackDocument.query("/configuration/appender[@class='" + MemoryAppender.class.getCanonicalName() + "']/@name"));

        // <logger level="WARN" name="foo"/>
        Nodes loggers = logbackDocument.query("/configuration/logger");
        assertEquals(2, loggers.size());
        Node loggerElement = logbackDocument.query("/configuration/logger[@name='foo']").get(0);
        assertNodeIs(Level.WARN.toString(), loggerElement.query("@level"));
        assertNodeIs("foo", loggerElement.query("@name"));
        assertNodeIs(Boolean.TRUE.toString(), loggerElement.query("@additivity"));

        Node loggerElement2 = logbackDocument.query("/configuration/logger[@name='foo2']").get(0);
        assertNodeIs(Level.ERROR.toString(), loggerElement2.query("@level"));
        assertNodeIs("foo2", loggerElement2.query("@name"));
        assertNodeIs(Boolean.FALSE.toString(), loggerElement2.query("@additivity"));
    }

    @Test
    public void testSimplestMemoryAppender() {
        assertEquals(0, loggerExtractor.getAppendersOfRoot().size());
        logger.info("test1");
        configureSimpleMemoryAppender();

        logger.info("test2");
        assertEquals(1, loggerExtractor.getAppendersOfRoot().size());
        MemoryAppender memoryAppender = getMemoryAppender();
        List<ILoggingEvent> loggingEvents = memoryAppender.getLoggingEvents();
        assertEquals(1, loggingEvents.size());
        assertEquals("test2", loggingEvents.get(0).getMessage());
        lc.reset();
        assertEquals(0, loggerExtractor.getAppendersOfRoot().size());
    }

    private void configureSimpleMemoryAppender() {
        LoggerTO loggerTO = createSimpleRootLoggerTO();
        new LogbackReconfigurator(Arrays.asList(getMemoryAppenderElement()), Arrays.asList(loggerTO), mock(LogbackStatusListener.class));
    }

    private LoggerTO createSimpleRootLoggerTO() {
        LoggerTO loggerTO = new LoggerTO();
        loggerTO.setAppenders(Arrays.asList("MEMORY"));
        loggerTO.setLevel(Level.INFO.toString());
        loggerTO.setLoggerName(Logger.ROOT_LOGGER_NAME);
        return loggerTO;
    }

    private Element getMemoryAppenderElement() {
        /*
        produce
            <appender name="MEMORY" class="org.opendaylight.controller.config.yang.logback.memoryappender.MemoryAppender"/>
         */
        Element appender = new Element("appender");
        appender.addAttribute(new Attribute("name", "MEMORY"));
        appender.addAttribute(new Attribute("class", MemoryAppender.class.getCanonicalName()));
        return appender;
    }
}
