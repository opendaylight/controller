/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.util.jmx;

import com.google.common.base.Preconditions;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.util.concurrent.CountingRejectedExecutionHandler;
import org.opendaylight.yangtools.util.concurrent.TrackingLinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MXBean implementation of the ThreadExecutorStatsMXBean interface that retrieves statistics
 * from a backing {@link java.util.concurrent.ExecutorService}.
 *
 * @author Thomas Pantelis
 */
public class ThreadExecutorStatsMXBeanImpl extends AbstractMXBean
                                           implements ThreadExecutorStatsMXBean {
    private static final Logger LOG = LoggerFactory.getLogger(ThreadExecutorStatsMXBeanImpl.class);
    private final ThreadPoolExecutor executor;

    /**
     * Constructs an instance for the given {@link Executor}.
     *
     * @param executor the backing {@link Executor}
     * @param mBeanName Used as the <code>name</code> property in the bean's ObjectName.
     * @param mBeanType Used as the <code>type</code> property in the bean's ObjectName.
     * @param mBeanCategory Used as the <code>Category</code> property in the bean's ObjectName.
     */
    public ThreadExecutorStatsMXBeanImpl(final ThreadPoolExecutor executor, final String mBeanName,
            final String mBeanType, @Nullable final String mBeanCategory) {
        super(mBeanName, mBeanType, mBeanCategory);
        this.executor = Preconditions.checkNotNull(executor);
    }

    private static ThreadExecutorStatsMXBeanImpl createInternal(final Executor executor,
            final String mBeanName, final String mBeanType, final String mBeanCategory) {
        if (executor instanceof ThreadPoolExecutor) {
            final ThreadExecutorStatsMXBeanImpl ret = new ThreadExecutorStatsMXBeanImpl(
                    (ThreadPoolExecutor) executor, mBeanName, mBeanType, mBeanCategory);
            return ret;
        }

        LOG.info("Executor {} is not supported", executor);
        return null;
    }

    /**
     * Creates a new bean if the backing executor is a ThreadPoolExecutor and registers it.
     *
     * @param executor the backing {@link Executor}
     * @param mBeanName Used as the <code>name</code> property in the bean's ObjectName.
     * @param mBeanType Used as the <code>type</code> property in the bean's ObjectName.
     * @param mBeanCategory Used as the <code>Category</code> property in the bean's ObjectName.
     * @return a registered ThreadExecutorStatsMXBeanImpl instance if the backing executor
     *         is a ThreadPoolExecutor, otherwise null.
     */
    public static ThreadExecutorStatsMXBeanImpl create(final Executor executor, final String mBeanName,
            final String mBeanType, @Nullable final String mBeanCategory) {
        ThreadExecutorStatsMXBeanImpl ret = createInternal(executor, mBeanName, mBeanType, mBeanCategory);
        if(ret != null) {
            ret.registerMBean();
        }

        return ret;
    }

    /**
     * Creates a new bean if the backing executor is a ThreadPoolExecutor.
     *
     * @param executor the backing {@link Executor}
     * @return a ThreadExecutorStatsMXBeanImpl instance if the backing executor
     *         is a ThreadPoolExecutor, otherwise null.
     */
    public static ThreadExecutorStatsMXBeanImpl create(final Executor executor) {
        return createInternal(executor, "", "", null);
    }

    @Override
    public long getCurrentThreadPoolSize() {
        return executor.getPoolSize();
    }

    @Override
    public long getLargestThreadPoolSize() {
        return  executor.getLargestPoolSize();
    }

    @Override
    public long getMaxThreadPoolSize() {
        return executor.getMaximumPoolSize();
    }

    @Override
    public long getCurrentQueueSize() {
        return executor.getQueue().size();
    }

    @Override
    public Long getLargestQueueSize() {
        BlockingQueue<Runnable> queue = executor.getQueue();
        if(queue instanceof TrackingLinkedBlockingQueue) {
            return Long.valueOf(((TrackingLinkedBlockingQueue<?>)queue).getLargestQueueSize());
        }

        return null;
    }

    @Override
    public long getMaxQueueSize() {
        long queueSize = executor.getQueue().size();
        return executor.getQueue().remainingCapacity() + queueSize;
    }

    @Override
    public long getActiveThreadCount() {
        return executor.getActiveCount();
    }

    @Override
    public long getCompletedTaskCount() {
        return  executor.getCompletedTaskCount();
    }

    @Override
    public long getTotalTaskCount() {
        return executor.getTaskCount();
    }

    @Override
    public Long getRejectedTaskCount() {
        RejectedExecutionHandler rejectedHandler = executor.getRejectedExecutionHandler();
        if(rejectedHandler instanceof CountingRejectedExecutionHandler) {
            return Long.valueOf(((CountingRejectedExecutionHandler)rejectedHandler)
                                                                     .getRejectedTaskCount());
        }

        return null;
    }

    /**
     * Returns a {@link ThreadExecutorStats} instance containing a snapshot of the statistic
     * metrics.
     */
    public ThreadExecutorStats toThreadExecutorStats() {
        return new ThreadExecutorStats(getActiveThreadCount(), getCurrentThreadPoolSize(),
                getLargestThreadPoolSize(), getMaxThreadPoolSize(), getCurrentQueueSize(),
                getLargestQueueSize(), getMaxQueueSize(), getCompletedTaskCount(),
                getTotalTaskCount(), getRejectedTaskCount());
    }
}
