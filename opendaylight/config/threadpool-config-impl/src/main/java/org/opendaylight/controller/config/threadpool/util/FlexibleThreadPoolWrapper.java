/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.threadpool.util;

import com.google.common.base.Optional;
import java.io.Closeable;
import java.io.IOException;
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

    public FlexibleThreadPoolWrapper(int minThreadCount, int maxThreadCount, long keepAlive, TimeUnit timeUnit,
            ThreadFactory threadFactory) {
        this(minThreadCount, maxThreadCount, keepAlive, timeUnit, threadFactory, getQueue(Optional.<Integer>absent()));
    }

    public FlexibleThreadPoolWrapper(int minThreadCount, int maxThreadCount, long keepAlive, TimeUnit timeUnit,
            ThreadFactory threadFactory, Optional<Integer> queueCapacity) {
        this(minThreadCount, maxThreadCount, keepAlive, timeUnit, threadFactory, getQueue(queueCapacity));
    }

    private FlexibleThreadPoolWrapper(int minThreadCount, int maxThreadCount, long keepAlive, TimeUnit timeUnit,
            ThreadFactory threadFactory, BlockingQueue<Runnable> queue) {

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
    private static ForwardingBlockingQueue getQueue(Optional<Integer> capacity) {
        final BlockingQueue<Runnable> delegate = capacity.isPresent() ? new LinkedBlockingQueue<>(capacity.get()) : new LinkedBlockingQueue<>();
        return new ForwardingBlockingQueue(delegate);
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

    /**
     * if the max threads are met, then it will raise a rejectedExecution. We then push to the queue.
     */
    private static class FlexibleRejectionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                throw new RejectedExecutionException("Interrupted while waiting on the queue", e);
            }
        }
    }

    private static class ForwardingBlockingQueue extends com.google.common.util.concurrent.ForwardingBlockingQueue<Runnable> {
        private final BlockingQueue<Runnable> delegate;

        public ForwardingBlockingQueue(BlockingQueue<Runnable> delegate) {
            this.delegate = delegate;
        }

        @Override
        protected BlockingQueue<Runnable> delegate() {
            return delegate;
        }

        @Override
        public boolean offer(final Runnable r) {
            // ThreadPoolExecutor will spawn a new thread after core size is reached only
            // if the queue.offer returns false.
            return false;
        }
    }
}
