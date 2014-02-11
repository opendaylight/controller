/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.JoranException;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.config.yang.logback.config.LoggerExtractor;
import org.opendaylight.controller.config.yang.logback.memoryappender.MemoryAppender;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class ResettingLogbackTestBase {
    protected final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    protected final LoggerExtractor loggerExtractor = new LoggerExtractor();

    @Before
    @After
    public void resetLogback() {
        lc.reset();
    }

    protected MemoryAppender getMemoryAppender() {
        List<Appender<ILoggingEvent>> appenders = loggerExtractor.getAppendersOfRoot();
        for (Appender<ILoggingEvent> appender : appenders) {
            if (MemoryAppender.class.isInstance(appender)) {
                return (MemoryAppender) appender;
            }
        }
        throw new IllegalStateException("Not found in " + appenders);
    }

    public void reconfigureUsingClassPathFile(String classPathFileName) throws IOException, JoranException {
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        byte[] memoryAppenderBytes;

        try (InputStream inputStream = getClass().getResourceAsStream(classPathFileName)) {
            assertNotNull("Cannot find " + classPathFileName, inputStream);
            memoryAppenderBytes = IOUtils.toByteArray(inputStream);
        }
        try (InputStream bis = new ByteArrayInputStream(memoryAppenderBytes)) {
            configurator.doConfigure(bis);
        }
    }

}
