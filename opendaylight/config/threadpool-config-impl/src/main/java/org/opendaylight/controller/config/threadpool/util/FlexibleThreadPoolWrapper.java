/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.threadpool.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.config.threadpool.ThreadPool;

/**
 * Implementation of {@link ThreadPool} using flexible number of threads wraps
 * {@link ExecutorService}.
 */
public class FlexibleThreadPoolWrapper implements ThreadPool, Closeable {
    private final ThreadPoolExecutor executor;

    public FlexibleThreadPoolWrapper(int minThreadCount, int maxThreadCount, long keepAlive, TimeUnit timeUnit,
            ThreadFactory threadFactory) {

        executor = new ThreadPoolExecutor(minThreadCount, maxThreadCount, keepAlive, timeUnit,
                new SynchronousQueue<Runnable>(), threadFactory);
        executor.prestartAllCoreThreads();
    }

    @Override
    public ExecutorService getExecutor() {
        return Executors.unconfigurableExecutorService(executor);
    }

    public int getMinThreadCount() {
        return executor.getCorePoolSize();
    }

    public void setMinThreadCount(int minThreadCount) {
        executor.setCorePoolSize(minThreadCount);
    }

    @Override
    public int getMaxThreadCount() {
        return executor.getMaximumPoolSize();
    }

    public void setMaxThreadCount(int maxThreadCount) {
        executor.setMaximumPoolSize(maxThreadCount);
    }

    public long getKeepAliveMillis() {
        return executor.getKeepAliveTime(TimeUnit.MILLISECONDS);
    }

    public void setKeepAliveMillis(long keepAliveMillis) {
        executor.setKeepAliveTime(keepAliveMillis, TimeUnit.MILLISECONDS);
    }

    public void setThreadFactory(ThreadFactory threadFactory) {
        executor.setThreadFactory(threadFactory);
    }

    public void prestartAllCoreThreads() {
        executor.prestartAllCoreThreads();
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
    }

}
