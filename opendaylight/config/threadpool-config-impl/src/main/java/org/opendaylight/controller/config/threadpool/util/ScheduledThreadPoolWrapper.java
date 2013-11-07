/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.threadpool.util;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.opendaylight.controller.config.threadpool.ScheduledThreadPool;

/**
 * Implementation of {@link ScheduledThreadPool} wraps
 * {@link ScheduledExecutorService}.
 */
public class ScheduledThreadPoolWrapper implements ScheduledThreadPool, Closeable {

    private final ScheduledThreadPoolExecutor executor;
    private final int threadCount;

    public ScheduledThreadPoolWrapper(int threadCount, ThreadFactory factory) {
        this.threadCount = threadCount;
        this.executor = new ScheduledThreadPoolExecutor(threadCount, factory);
        executor.prestartAllCoreThreads();
    }

    @Override
    public ScheduledExecutorService getExecutor() {
        return Executors.unconfigurableScheduledExecutorService(executor);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    @Override
    public int getMaxThreadCount() {
        return threadCount;
    }

}
