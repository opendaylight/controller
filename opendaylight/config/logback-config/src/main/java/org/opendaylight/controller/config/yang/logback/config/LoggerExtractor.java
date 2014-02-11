/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Using logback APIs extract all loggers as they appeared in logback.xml during startup. This
 * is a bit tricky because logback keeps all initialized loggers, not only configured.
 * Every time a logger is initialized, the reference will be pushed to
 * {@link ch.qos.logback.classic.LoggerContext#getLoggerList()}.
 * When a logger in XML has name 'foo.bar', both 'foo.bar' and 'foo' will be initialized.
 * In order to detect loggers that were actually configured and contain additional value,
 * a filter in {@link #getReducedLoggers()} is employed.
 */
public class LoggerExtractor {
    private final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

    public Map<String /* name */, LoggerTO> getReducedLoggerTOs() {
        List<ch.qos.logback.classic.Logger> loggers = getReducedLoggers();
        Map<String /* name */, LoggerTO> result = new HashMap<>();
        for (ch.qos.logback.classic.Logger l : loggers) {
            List<String> appenderNames = new ArrayList<>();
            for (Appender<ILoggingEvent> a : getAppenders(l)) {
                appenderNames.add(a.getName());
            }
            LoggerTO to = new LoggerTO();
            to.setLoggerName(l.getName());
            to.setLevel(l.getEffectiveLevel().toString());
            to.setAppenders(appenderNames);
            to.setAdditivity(l.isAdditive());
            result.put(l.getName(), to);
        }
        return result;
    }

    /**
     * Each logger returned is guaranteed to have not-null effective logger.
     */
    public List<Logger> getReducedLoggers() {
        List<ch.qos.logback.classic.Logger> loggerList = lc.getLoggerList();
        boolean rerun = true;
        while (rerun) {
            rerun = false;
            for (Iterator<Logger> it = loggerList.iterator(); it.hasNext(); ) {
                ch.qos.logback.classic.Logger inspectedLogger = it.next();
                // reduce if
                ch.qos.logback.classic.Logger parent = getParent(inspectedLogger);
                if (parent != null) {
                    // no properties are different from parent
                    boolean same = isLogLevelMatching(inspectedLogger, parent);
                    boolean emptyAppenders = inspectedLogger.iteratorForAppenders().hasNext() == false;
                    boolean sameAppendersAsAscendant = false;
                    if (same && emptyAppenders == false) {
                        sameAppendersAsAscendant = areAppendersMatchingAscendant(inspectedLogger, parent);
                    }
                    same &= (emptyAppenders || sameAppendersAsAscendant);
                    if (same) {
                        it.remove();
                        rerun = true;
                    }
                }
            }
        }
        // clean up loggers with nullable effective level just in case
        for (Iterator<ch.qos.logback.classic.Logger> it = loggerList.iterator(); it.hasNext(); ) {
            ch.qos.logback.classic.Logger inspectedLogger = it.next();
            if (inspectedLogger.getEffectiveLevel() == null) {
                it.remove();
            }
        }
        return loggerList;
    }

    private boolean isLogLevelMatching(ch.qos.logback.classic.Logger inspectedLogger, ch.qos.logback.classic.Logger parent) {
        return ObjectUtils.equals(inspectedLogger.getEffectiveLevel(), parent.getEffectiveLevel());
    }

    private boolean areAppendersMatchingAscendant(ch.qos.logback.classic.Logger inspectedLogger, ch.qos.logback.classic.Logger parent) {
        // even if appenders are specified, if they match ascendant with non empty appenders, still considered same
        List<Appender<ILoggingEvent>> actualAppenders = getAppenders(inspectedLogger);
        ch.qos.logback.classic.Logger ascendant = parent;
        while (ascendant != null) {
            List<Appender<ILoggingEvent>> ascendantAppenders = getAppenders(ascendant);
            if (actualAppenders.equals(ascendantAppenders) && isLogLevelMatching(inspectedLogger, ascendant)) {
                return true;
            } else if (ascendantAppenders.isEmpty() == false || isLogLevelMatching(inspectedLogger, ascendant) == false) {
                // do not continue if appenders are not emtpy - this means it has custom configuration and is different
                return false;
            } // try parent of ascendant
            ascendant = getParent(ascendant);
        }
        return false;
    }

    private ch.qos.logback.classic.Logger getParent(ch.qos.logback.classic.Logger logger) {
        Field field;
        try {
            field = logger.getClass().getDeclaredField("parent");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
        field.setAccessible(true);
        try {
            return (ch.qos.logback.classic.Logger) field.get(logger);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @VisibleForTesting
    public List<Appender<ILoggingEvent>> getAppendersOfRoot() {
        return getAppenders(lc.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME));
    }

    @VisibleForTesting
    public List<Appender<ILoggingEvent>> getAppenders(ch.qos.logback.classic.Logger logger) {
        return Lists.newArrayList(logger.iteratorForAppenders());
    }
}
