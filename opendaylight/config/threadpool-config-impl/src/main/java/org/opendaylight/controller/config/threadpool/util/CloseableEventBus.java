/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.threadpool.util;

import java.io.Closeable;

import org.opendaylight.controller.config.yang.threadpool.impl.EventBusRuntimeMXBean;
import org.opendaylight.controller.config.yang.threadpool.impl.EventBusRuntimeRegistration;
import org.opendaylight.controller.config.yang.threadpool.impl.EventBusRuntimeRegistrator;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Closeable {@link EventBus}.
 */
public class CloseableEventBus extends EventBus implements Closeable {

    private final EventBusRuntimeRegistration rootRegistration;

    public CloseableEventBus(String identifier, EventBusRuntimeRegistrator rootRegistrator) {
        super(identifier);
        rootRegistration = rootRegistrator.register(new EventBusRuntimeMXBean() {
            private long deadEventsCounter = 0;

            @Subscribe
            public void increaseDeadEvents(DeadEvent deadEvent) {
                deadEventsCounter++;
            }

            @Override
            public Long countDeadEvents() {
                return deadEventsCounter;
            }
        });

    }

    @Override
    public void close() {
        rootRegistration.close();

    }
}
