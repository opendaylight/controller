/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.threadpool.util;

import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import org.opendaylight.controller.config.threadpool.ThreadPool;

/**
 * Implementation of {@link ThreadPool} using fixed number of threads wraps
 * {@link ExecutorService}.
 */
public class FixedThreadPoolWrapper implements ThreadPool, Closeable {

    private final ThreadPoolExecutor executor;

    public FixedThreadPoolWrapper(int threadCount, ThreadFactory factory) {
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount, factory);
        executor.prestartAllCoreThreads();
    }

    @Override
    public ExecutorService getExecutor() {
        return Executors.unconfigurableExecutorService(executor);
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    @Override
    public int getMaxThreadCount() {
        return executor.getMaximumPoolSize();
    }

    public void setMaxThreadCount(int maxThreadCount) {
        executor.setCorePoolSize(maxThreadCount);
        executor.setMaximumPoolSize(maxThreadCount);
    }
}
