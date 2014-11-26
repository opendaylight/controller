/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.logback.config.loader.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.opendaylight.controller.logback.config.loader.impl.LogbackConfigUtil;
import org.opendaylight.controller.logback.config.loader.impl.LogbackConfigurationLoader;
import org.opendaylight.controller.logback.config.loader.test.logwork.Debugger;
import org.opendaylight.controller.logback.config.loader.test.logwork.Errorer;
import org.opendaylight.controller.logback.config.loader.test.logwork.Informer;
import org.opendaylight.controller.logback.config.loader.test.logwork.Tracer;
import org.opendaylight.controller.logback.config.loader.test.logwork.Warner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * test of logging config loader - {@link LogbackConfigurationLoader}
 */
@RunWith(JUnit4.class)
public class LogbackConfigurationLoaderTest {

    /** logback config root */
    private static final String LOGBACK_D = "/logback.d";
    private static final Logger LOG = LoggerFactory
            .getLogger(LogbackConfigurationLoaderTest.class);

    /**
     * Test of method {@link LogbackConfigurationLoader#load(boolean, Object[])}
     *
     * @throws Exception
     */
    @Test
    public void testLoad() throws Exception {
        File logConfigRoot = new File(LogbackConfigurationLoaderTest.class
                .getResource(LOGBACK_D).getFile());
        List<File> sortedConfigFiles = LogbackConfigUtil.harvestSortedConfigFiles(logConfigRoot);
        LogbackConfigurationLoader.load(true, sortedConfigFiles.toArray());

        LOG.info("LOGBACK ready -> about to use it");

        Tracer.doSomeAction();
        Debugger.doSomeAction();
        Informer.doSomeAction();
        Warner.doSomeAction();
        Errorer.doSomeAction();

        // check logs
        String[] expectedLogs = new String[] {
            "LoggingEvent -> [INFO] org.opendaylight.controller.logback.config.loader.test.LogbackConfigurationLoaderTest: LOGBACK ready -> about to use it",
            "LoggingEvent -> [TRACE] org.opendaylight.controller.logback.config.loader.test.logwork.Tracer: tracing",
            "LoggingEvent -> [DEBUG] org.opendaylight.controller.logback.config.loader.test.logwork.Tracer: debugging",
            "LoggingEvent -> [INFO] org.opendaylight.controller.logback.config.loader.test.logwork.Tracer: infoing",
            "LoggingEvent -> [WARN] org.opendaylight.controller.logback.config.loader.test.logwork.Tracer: warning",
            "LoggingEvent -> [ERROR] org.opendaylight.controller.logback.config.loader.test.logwork.Tracer: erroring",
            "LoggingEvent -> [DEBUG] org.opendaylight.controller.logback.config.loader.test.logwork.Debugger: debugging",
            "LoggingEvent -> [INFO] org.opendaylight.controller.logback.config.loader.test.logwork.Debugger: infoing",
            "LoggingEvent -> [WARN] org.opendaylight.controller.logback.config.loader.test.logwork.Debugger: warning",
            "LoggingEvent -> [ERROR] org.opendaylight.controller.logback.config.loader.test.logwork.Debugger: erroring",
            "LoggingEvent -> [INFO] org.opendaylight.controller.logback.config.loader.test.logwork.Informer: infoing",
            "LoggingEvent -> [WARN] org.opendaylight.controller.logback.config.loader.test.logwork.Informer: warning",
            "LoggingEvent -> [ERROR] org.opendaylight.controller.logback.config.loader.test.logwork.Informer: erroring",
            "LoggingEvent -> [WARN] org.opendaylight.controller.logback.config.loader.test.logwork.Warner: warning",
            "LoggingEvent -> [ERROR] org.opendaylight.controller.logback.config.loader.test.logwork.Warner: erroring",
            "LoggingEvent -> [ERROR] org.opendaylight.controller.logback.config.loader.test.logwork.Errorer: erroring"
        };

        List<String> logSnapshot = new ArrayList<>(TestAppender.getLogRecord());
        for (String logLine : logSnapshot) {
            LOG.info("\"{}\",", logLine);
        }

        Assert.assertArrayEquals(expectedLogs, logSnapshot.toArray());
    }
}
