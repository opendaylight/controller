/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ContextSetter}. Resets running logback
 * configuration.
 */
public class ContextSetterImpl implements ContextSetter, Closeable {

    private final LogbackStatusListener statusListener;
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ContextSetterImpl.class);

    public ContextSetterImpl(final LogbackRuntimeRegistrator rootRuntimeBeanRegistratorWrapper) {
        statusListener = new LogbackStatusListener(rootRuntimeBeanRegistratorWrapper);
        statusListener.register();
    }

    @Override
    public void updateContext(final LogbackModule module) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        List<ch.qos.logback.classic.Logger> loggersBefore = context.getLoggerList();

        createLoggers(context, module, Sets.newHashSet(loggersBefore));
    }

    private Map<String, Appender<ILoggingEvent>> createConsoleAppenders(final LoggerContext context, final LogbackModule module) {
        Map<String, Appender<ILoggingEvent>> appendersMap = new HashMap<>();
        for (ConsoleAppenderTO appender : module.getConsoleAppenderTO()) {
            Preconditions.checkState(appendersMap.containsKey(appender.getName()) == false,
                    "Duplicate appender name %s", appender.getName());
            ch.qos.logback.core.ConsoleAppender<ILoggingEvent> app = new ch.qos.logback.core.ConsoleAppender<>();
            app.setContext(context);
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            encoder.setPattern(appender.getEncoderPattern());
            encoder.start();
            app.setEncoder(encoder);
            ThresholdFilter filter = new ThresholdFilter();
            filter.setContext(context);
            filter.setLevel(appender.getThresholdFilter());
            filter.start();
            app.getCopyOfAttachedFiltersList().add(filter);
            app.setName(appender.getName());
            app.start();
            appendersMap.put(app.getName(), app);
        }
        return appendersMap;
    }

    private void createLoggers(final LoggerContext context, final LogbackModule module,
            final Set<ch.qos.logback.classic.Logger> loggersBefore) {

        Map<String, Appender<ILoggingEvent>> appendersMap = getAppenders(module, context);

        for (LoggerTO logger : module.getLoggerTO()) {
            LOG.trace("Setting configuration for logger {}", logger.getLoggerName());
            final ch.qos.logback.classic.Logger logbackLogger = context.getLogger(logger.getLoggerName());

            Optional<Set<Appender<ILoggingEvent>>> appendersBefore = getAppendersBefore(loggersBefore, logbackLogger);
            LOG.trace("Logger {}: Appenders registered before: {}", logger.getLoggerName(),
                    appendersBefore.isPresent() ? appendersBefore.get() : "NO APPENDERS BEFORE");

            logbackLogger.setLevel(Level.toLevel(logger.getLevel()));

            addNewAppenders(appendersMap, logger, logbackLogger, appendersBefore);
            removeBeforeAppenders(loggersBefore, logger, logbackLogger, appendersBefore);
        }
    }

    private void addNewAppenders(final Map<String, Appender<ILoggingEvent>> appendersMap, final LoggerTO logger,
            final ch.qos.logback.classic.Logger logbackLogger, final Optional<Set<Appender<ILoggingEvent>>> appendersBefore) {
        if (logger.getAppenders() != null) {
            for (String appenderName : logger.getAppenders()) {
                if (appendersMap.containsKey(appenderName)) {
                    logbackLogger.addAppender(appendersMap.get(appenderName));
                    LOG.trace("Logger {}: Adding new appender: {}", logger.getLoggerName(), appenderName);
                } else {
                    throw new IllegalStateException("No appender " + appenderName
                            + " found. This error should have been discovered by validation");
                }
            }
        }
    }

    private void removeBeforeAppenders(final Set<ch.qos.logback.classic.Logger> loggersBefore, final LoggerTO logger,
            final ch.qos.logback.classic.Logger logbackLogger, final Optional<Set<Appender<ILoggingEvent>>> appendersBefore) {
        if (appendersBefore.isPresent()) {
            for (Appender<ILoggingEvent> appenderBefore : appendersBefore.get()) {
                logbackLogger.detachAppender(appenderBefore);
                appenderBefore.stop();
                LOG.trace("Logger {}: Removing old appender: {}", logger.getLoggerName(),
                        appenderBefore.getName());
            }
            loggersBefore.remove(logbackLogger);
        }
    }

    private Optional<Set<Appender<ILoggingEvent>>> getAppendersBefore(final Set<ch.qos.logback.classic.Logger> loggersBefore,
            final ch.qos.logback.classic.Logger logbackLogger) {
        if (loggersBefore.contains(logbackLogger)) {
            Iterator<Appender<ILoggingEvent>> appenderIt = logbackLogger.iteratorForAppenders();
            Set<Appender<ILoggingEvent>> appendersBefore = Sets.newHashSet();
            while (appenderIt.hasNext()) {
                appendersBefore.add(appenderIt.next());
            }
            return Optional.of(appendersBefore);
        } else {
            return Optional.absent();
        }

    }

    private Map<String, Appender<ILoggingEvent>> getAppenders(final LogbackModule module, final LoggerContext context) {
        Map<String, Appender<ILoggingEvent>> appendersMap = new HashMap<>();
        addAllAppenders(appendersMap, createRollingAppenders(context, module));
        addAllAppenders(appendersMap, createFileAppenders(context, module));
        addAllAppenders(appendersMap, createConsoleAppenders(context, module));

        return appendersMap;
    }

    private void addAllAppenders(final Map<String, Appender<ILoggingEvent>> allAppenders,
            final Map<String, Appender<ILoggingEvent>> appendersToAdd) {
        for (String appenderName : appendersToAdd.keySet()) {
            Preconditions.checkState(allAppenders.containsKey(appenderName) == false, "Duplicate appender name %s",
                    appenderName);
            allAppenders.put(appenderName, appendersToAdd.get(appenderName));
        }
    }

    private Map<String, Appender<ILoggingEvent>> createFileAppenders(final LoggerContext context, final LogbackModule module) {
        Map<String, Appender<ILoggingEvent>> appendersMap = new HashMap<>();
        for (FileAppenderTO appender : module.getFileAppenderTO()) {
            Preconditions.checkState(appendersMap.containsKey(appender.getName()) == false,
                    "Duplicate appender name %s", appender.getName());
            ch.qos.logback.core.FileAppender<ILoggingEvent> app = new ch.qos.logback.core.FileAppender<>();
            app.setAppend(appender.getAppend());
            app.setContext(context);
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            encoder.setPattern(appender.getEncoderPattern());
            encoder.start();
            app.setEncoder(encoder);
            app.setFile(appender.getFileName());
            app.setName(appender.getName());
            app.start();
            appendersMap.put(app.getName(), app);
        }

        return appendersMap;
    }

    private Map<String, Appender<ILoggingEvent>> createRollingAppenders(final LoggerContext context, final LogbackModule module) {
        Map<String, Appender<ILoggingEvent>> appendersMap = new HashMap<>();
        for (RollingFileAppenderTO appender : module.getRollingFileAppenderTO()) {
            Preconditions.checkState(appendersMap.containsKey(appender.getName()) == false,
                    "Duplicate appender name %s", appender.getName());
            ch.qos.logback.core.rolling.RollingFileAppender<ILoggingEvent> app = new ch.qos.logback.core.rolling.RollingFileAppender<>();
            app.setAppend(appender.getAppend());
            app.setContext(context);
            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(context);
            encoder.setPattern(appender.getEncoderPattern());
            encoder.start();
            app.setEncoder(encoder);
            app.setFile(appender.getFileName());
            if (appender.getRollingPolicyType().equals("FixedWindowRollingPolicy")) {
                FixedWindowRollingPolicy policy = new FixedWindowRollingPolicy();
                policy.setContext(context);
                policy.setMaxIndex(appender.getMaxIndex());
                policy.setMinIndex(appender.getMinIndex());
                policy.setFileNamePattern(appender.getFileNamePattern());
                policy.setParent(app);
                policy.start();
                app.setRollingPolicy(policy);
            } else if (appender.getRollingPolicyType().equals("TimeBasedRollingPolicy")) {
                TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
                policy.setContext(context);
                policy.setMaxHistory(appender.getMaxHistory());
                if (appender.getCleanHistoryOnStart() != null) {
                    policy.setCleanHistoryOnStart(appender.getCleanHistoryOnStart());
                }
                policy.setFileNamePattern(appender.getFileNamePattern());
                policy.setParent(app);
                policy.start();
                app.setRollingPolicy(policy);
            }
            SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
            triggeringPolicy.setContext(context);
            triggeringPolicy.setMaxFileSize(appender.getMaxFileSize());
            triggeringPolicy.start();
            app.setTriggeringPolicy(triggeringPolicy);
            app.setName(appender.getName());
            app.start();
            appendersMap.put(app.getName(), app);
        }
        return appendersMap;
    }

    @Override
    public void close() throws IOException {
        statusListener.close();
    }
}
