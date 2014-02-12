/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.util;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppenderDiscovery {
    private final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

    /**
     * Get actual list of appenders of a given class indexed by appender name.
     * @param appenderClass All returned appenders must have class equal to this one
     */
    public <APPENDER extends Appender<ILoggingEvent>> Map<String/*name*/, APPENDER> findAppenders(Class<APPENDER> appenderClass) {
        Map<String, APPENDER> result = new HashMap<>();
        List<ch.qos.logback.classic.Logger> loggerList = lc.getLoggerList();
        for (ch.qos.logback.classic.Logger logger : loggerList) {
            for (Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders(); appenderIterator.hasNext(); ) {
                Appender<ILoggingEvent> next = appenderIterator.next();
                if (appenderClass.equals(next.getClass())) {
                    result.put(next.getName(), appenderClass.cast(next));
                }
            }
        }
        return result;
    }

    public Set<Class<Appender<ILoggingEvent>>> findAllAppenderClasses() {
        Set<Class<Appender<ILoggingEvent>>> result = new HashSet<>();
        for (ch.qos.logback.classic.Logger logger : lc.getLoggerList()) {
            for (Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders(); appenderIterator.hasNext(); ) {
                Appender<ILoggingEvent> next = appenderIterator.next();
                    result.add((Class<Appender<ILoggingEvent>>) next.getClass());
            }
        }
        return result;
    }


}
