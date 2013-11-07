/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.threadpool.util;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;

import java.io.Closeable;
import java.io.IOException;

import org.opendaylight.controller.config.threadpool.ThreadPool;
import org.opendaylight.controller.config.yang.threadpool.impl.AsyncEventBusRuntimeMXBean;
import org.opendaylight.controller.config.yang.threadpool.impl.AsyncEventBusRuntimeRegistration;
import org.opendaylight.controller.config.yang.threadpool.impl.AsyncEventBusRuntimeRegistrator;

/**
 * Closeable version of {@link AsyncEventBus}.
 */
public class CloseableAsyncEventBus extends AsyncEventBus implements Closeable {
    private final ThreadPool threadPool;
    private final AsyncEventBusRuntimeRegistration rootRegistration;

    public CloseableAsyncEventBus(String identifier, ThreadPool threadPool,
            AsyncEventBusRuntimeRegistrator rootRegistrator) {
        super(identifier, threadPool.getExecutor());
        this.threadPool = threadPool;
        rootRegistration = rootRegistrator.register(new AsyncEventBusRuntimeMXBean() {
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

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    @Override
    public void close() throws IOException {
        rootRegistration.close();
    }

}
