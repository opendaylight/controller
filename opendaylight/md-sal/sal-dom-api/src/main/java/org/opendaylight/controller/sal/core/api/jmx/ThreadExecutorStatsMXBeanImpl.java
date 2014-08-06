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

import com.google.common.base.Preconditions;

public class ThreadExecutorStatsMXBeanImpl extends AbstractMXBean
                                           implements ThreadExecutorStatsMXBean {

    private final ThreadPoolExecutor executor;

    public ThreadExecutorStatsMXBeanImpl(ExecutorService executor, String mBeanName,
            String mBeanType, String mBeanCategory) {
        super(mBeanName, mBeanType, mBeanCategory);

        Preconditions.checkArgument(executor instanceof ThreadPoolExecutor,
                "The ExecutorService of type {} is not an instanceof ThreadPoolExecutor",
                executor.getClass());
        this.executor = (ThreadPoolExecutor)executor;
    }

    @Override
    public Long getCurrentThreadPoolSize() {
        return Long.valueOf(executor.getPoolSize());
    }

    @Override
    public Long getLargestThreadPoolSize() {
        return  Long.valueOf(executor.getLargestPoolSize());
    }

    @Override
    public Long getMaxThreadPoolSize() {
        return Long.valueOf(executor.getMaximumPoolSize());
    }

    @Override
    public Long getCurrentQueueSize() {
        return Long.valueOf(executor.getQueue().size());
    }

    @Override
    public Long getMaxQueueSize() {
        long queueSize = executor.getQueue().size();
        Long maxQueueSize = Long.valueOf(executor.getQueue().remainingCapacity() + queueSize);
        return maxQueueSize;
    }

    @Override
    public Long getActiveThreadCount() {
        return Long.valueOf(executor.getActiveCount());
    }

    @Override
    public Long getCompletedTaskCount() {
        return  Long.valueOf(executor.getCompletedTaskCount());
    }

    @Override
    public Long getTotalTaskCount() {
        return Long.valueOf(executor.getTaskCount());
    }

    public ThreadExecutorStats toThreadExecutorStats() {
        return new ThreadExecutorStats(getActiveThreadCount(), getCurrentThreadPoolSize(),
                getLargestThreadPoolSize(), getMaxThreadPoolSize(), getCurrentQueueSize(),
                getMaxQueueSize(), getCompletedTaskCount(), getTotalTaskCount());
    }
}
