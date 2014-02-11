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
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.opendaylight.controller.config.yang.logback.ResettingLogbackTestBase;
import org.opendaylight.controller.config.yang.logback.memoryappender.MemoryAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class LoggerExtractorTest extends ResettingLogbackTestBase {
    private static final Logger logger = LoggerFactory.getLogger(LoggerExtractorTest.class);


    @Test
    public void testSwitchingConfigFile() throws Exception {
        logger.info("test1");
        reconfigureUsingMemoryFile();
        logger.info("test2");
        assertEquals(2, loggerExtractor.getAppendersOfRoot().size());
        MemoryAppender memoryAppender = getMemoryAppender();
        List<ILoggingEvent> loggingEvents = memoryAppender.getLoggingEvents();
        assertEquals(1, loggingEvents.size());
        assertEquals("test2", loggingEvents.get(0).getMessage());

        lc.reset();
        assertEquals(0, loggerExtractor.getAppendersOfRoot().size());
    }

    private void reconfigureUsingMemoryFile() throws IOException, JoranException {
        String classPathFileName = "/memory_appender.xml";
        reconfigureUsingClassPathFile(classPathFileName);
    }



    @Test
    public void testGettingLoggers() throws Exception {
        reconfigureUsingMemoryFile();
        testReducedLoggers();
        logger.info("test");
        LoggerFactory.getLogger("w.e.a.b.c.").error("test");
        testReducedLoggers();
    }

    private void testReducedLoggers() {
        List<ch.qos.logback.classic.Logger> loggers = loggerExtractor.getReducedLoggers();
        Map<String, Entry<Level, List<String /* appender name */>>> expected = Maps.newHashMap(), actual = Maps.newHashMap();
        for (ch.qos.logback.classic.Logger l : loggers) {
            List<String> appenderNames = new ArrayList<>();
            for (Appender<ILoggingEvent> a : loggerExtractor.getAppenders(l)) {
                appenderNames.add(a.getName());
            }
            actual.put(l.getName(), Maps.immutableEntry(l.getEffectiveLevel(), appenderNames));
        }

        expected.put(Logger.ROOT_LOGGER_NAME, Maps.immutableEntry(Level.INFO, asList("STDOUT", "MEMORY")));
        expected.put("stdout", Maps.immutableEntry(Level.DEBUG, asList("STDOUT")));
        expected.put("w", Maps.immutableEntry(Level.WARN, Arrays.<String>asList()));
        expected.put("w.e", Maps.immutableEntry(Level.ERROR, Arrays.<String>asList()));
        assertEquals(expected, actual);
    }


}
