/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.logback.config.loader.test;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.LogbackException;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.status.Status;
import java.util.ArrayList;
import java.util.List;

/**
 * dummy appender for collecting log messages
 *
 * @param <E>
 */
public class TestAppender<E> implements Appender<E> {

    private boolean started;
    private Context context;
    private String name;

    private static List<String> logRecord = new ArrayList<>();

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void addStatus(Status status) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addInfo(String msg) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addInfo(String msg, Throwable ex) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addWarn(String msg) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addWarn(String msg, Throwable ex) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addError(String msg) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addError(String msg, Throwable ex) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addFilter(Filter<E> newFilter) {
        // TODO Auto-generated method stub
    }

    @Override
    public void clearAllFilters() {
        // TODO Auto-generated method stub
    }

    @Override
    public List<Filter<E>> getCopyOfAttachedFiltersList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FilterReply getFilterChainDecision(E event) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void doAppend(E event) throws LogbackException {
        if (event instanceof LoggingEvent) {
            LoggingEvent lEvent = (LoggingEvent) event;
            logRecord.add(String.format("%s -> [%s] %s: %s", event.getClass()
                    .getSimpleName(), lEvent.getLevel(),
                    lEvent.getLoggerName(), lEvent.getMessage()));
        } else {
            logRecord.add(event.getClass() + " -> " + event.toString());
        }
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the logRecord
     */
    public static List<String> getLogRecord() {
        return logRecord;
    }

}
