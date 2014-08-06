/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.api.jmx;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * MXBean implementation of the ThreadExecutorStatsMXBean interface that retrieves statistics
 * from a backing {@link ExecutorService}.
 *
 * @author Thomas Pantelis
 */
public class ThreadExecutorStatsMXBeanImpl extends AbstractMXBean
                                           implements ThreadExecutorStatsMXBean {

    private final ThreadPoolExecutor executor;

    /**
     * Constructs an instance for the given {@link ExecutorService}.
     *
     * @param executor the backing {@link ExecutorService}
     * @param mBeanName Used as the <code>name</code> property in the bean's ObjectName.
     * @param mBeanType Used as the <code>type</code> property in the bean's ObjectName.
     * @param mBeanCategory Used as the <code>Category</code> property in the bean's ObjectName.
     */
    public ThreadExecutorStatsMXBeanImpl(ExecutorService executor, String mBeanName,
            String mBeanType, @Nullable String mBeanCategory) {
        super(mBeanName, mBeanType, mBeanCategory);

        Preconditions.checkArgument(executor instanceof ThreadPoolExecutor,
                "The ExecutorService of type {} is not an instanceof ThreadPoolExecutor",
                executor.getClass());
        this.executor = (ThreadPoolExecutor)executor;
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

    /**
     * Returns a {@link ThreadExecutorStats} instance containing a snapshot of the statistic
     * metrics.
     */
    public ThreadExecutorStats toThreadExecutorStats() {
        return new ThreadExecutorStats(getActiveThreadCount(), getCurrentThreadPoolSize(),
                getLargestThreadPoolSize(), getMaxThreadPoolSize(), getCurrentQueueSize(),
                getMaxQueueSize(), getCompletedTaskCount(), getTotalTaskCount());
    }
}
