/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.threadpool.util;

import java.io.Closeable;
import java.util.OptionalInt;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
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

    public FlexibleThreadPoolWrapper(final int minThreadCount, final int maxThreadCount, final long keepAlive,
            final TimeUnit timeUnit, final ThreadFactory threadFactory) {
        this(minThreadCount, maxThreadCount, keepAlive, timeUnit, threadFactory, getQueue(OptionalInt.empty()));
    }

    public FlexibleThreadPoolWrapper(final int minThreadCount, final int maxThreadCount, final long keepAlive,
            final TimeUnit timeUnit, final ThreadFactory threadFactory, final OptionalInt queueCapacity) {
        this(minThreadCount, maxThreadCount, keepAlive, timeUnit, threadFactory, getQueue(queueCapacity));
    }

    private FlexibleThreadPoolWrapper(final int minThreadCount, final int maxThreadCount, final long keepAlive,
            final TimeUnit timeUnit, final ThreadFactory threadFactory, final BlockingQueue<Runnable> queue) {

        executor = new ThreadPoolExecutor(minThreadCount, maxThreadCount, keepAlive, timeUnit,
                queue, threadFactory, new FlexibleRejectionHandler());
        executor.prestartAllCoreThreads();
    }

    /**
     * Overriding the queue:
     * ThreadPoolExecutor would not create new threads if the queue is not full, thus adding
     * occurs in RejectedExecutionHandler.
     * This impl saturates threadpool first, then queue. When both are full caller will get blocked.
     */
    private static ForwardingBlockingQueue getQueue(final OptionalInt capacity) {
        return new ForwardingBlockingQueue(
            capacity.isPresent() ? new LinkedBlockingQueue<>(capacity.orElseThrow()) : new LinkedBlockingQueue<>());
    }

    @Override
    public ExecutorService getExecutor() {
        return Executors.unconfigurableExecutorService(executor);
    }

    public int getMinThreadCount() {
        return executor.getCorePoolSize();
    }

    public void setMinThreadCount(final int minThreadCount) {
        executor.setCorePoolSize(minThreadCount);
    }

    @Override
    public int getMaxThreadCount() {
        return executor.getMaximumPoolSize();
    }

    public void setMaxThreadCount(final int maxThreadCount) {
        executor.setMaximumPoolSize(maxThreadCount);
    }

    public long getKeepAliveMillis() {
        return executor.getKeepAliveTime(TimeUnit.MILLISECONDS);
    }

    public void setKeepAliveMillis(final long keepAliveMillis) {
        executor.setKeepAliveTime(keepAliveMillis, TimeUnit.MILLISECONDS);
    }

    public void setThreadFactory(final ThreadFactory threadFactory) {
        executor.setThreadFactory(threadFactory);
    }

    public void prestartAllCoreThreads() {
        executor.prestartAllCoreThreads();
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    /**
     * if the max threads are met, then it will raise a rejectedExecution. We then push to the queue.
     */
    private static final class FlexibleRejectionHandler implements RejectedExecutionHandler {
        @Override
        @SuppressWarnings("checkstyle:parameterName")
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                throw new RejectedExecutionException("Interrupted while waiting on the queue", e);
            }
        }
    }

    private static final class ForwardingBlockingQueue
            extends com.google.common.util.concurrent.ForwardingBlockingQueue<Runnable> {
        private final BlockingQueue<Runnable> delegate;

        ForwardingBlockingQueue(final BlockingQueue<Runnable> delegate) {
            this.delegate = delegate;
        }

        @Override
        protected BlockingQueue<Runnable> delegate() {
            return delegate;
        }

        @Override
        @SuppressWarnings("checkstyle:parameterName")
        public boolean offer(final Runnable o) {
            // ThreadPoolExecutor will spawn a new thread after core size is reached only
            // if the queue.offer returns false.
            return false;
        }
    }
}
