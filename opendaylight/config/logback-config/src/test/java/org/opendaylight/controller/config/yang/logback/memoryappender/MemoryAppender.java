package org.opendaylight.controller.config.yang.logback.memoryappender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MemoryAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> events = new LinkedList<>();

    @Override
    protected synchronized void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }
        events.add(event);
    }


    public synchronized List<ILoggingEvent> getLoggingEvents() {
        return new ArrayList<>(events);
    }

    public synchronized void clear() {
        events.clear();
    }
}