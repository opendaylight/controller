/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.logback.config.loader.impl;

import java.io.File;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Logback configuration loader.
 * Strategy:
 * <ol>
 * <li>reset actual configuration (probably default configuration)</li>
 * <li>load all given logback config xml files in given order</li>
 * </ol>
 */
public abstract class LogbackConfigurationLoader {

    private static Logger LOG = LoggerFactory
            .getLogger(LogbackConfigurationLoader.class);

    /**
     * @param args
     */
    public static void load(Object[] args) {
        LOG.trace("BEFORE logback reconfig");
        // assume SLF4J is bound to logback in the current environment
        LoggerContext context = (LoggerContext) LoggerFactory
                .getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            // Call context.reset() to clear any previous configuration, e.g. default
            // configuration. For multi-step configuration, omit calling context.reset().
            context.reset();
            for (Object logbackFile : args) {
                if (logbackFile instanceof String) {
                    configurator.doConfigure((String) logbackFile);
                } else if (logbackFile instanceof URL) {
                    configurator.doConfigure((URL) logbackFile);
                } else if (logbackFile instanceof File) {
                    configurator.doConfigure((File) logbackFile);
                }

                LOG.trace("applied {}", logbackFile);
            }
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        LOG.trace("AFTER logback reconfig");
    }

}
