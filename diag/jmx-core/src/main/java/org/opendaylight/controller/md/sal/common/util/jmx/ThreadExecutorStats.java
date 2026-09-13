/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.common.util.jmx;

import javax.management.ConstructorParameters;

/**
 * A bean class that holds various thread executor statistic metrics. This class is suitable for
 * mapping to the MXBean CompositeDataSupport type;
 *
 * @author Thomas Pantelis
 * @see ThreadExecutorStatsMXBeanImpl
 */
public class ThreadExecutorStats {

    private final long activeThreadCount;
    private final long completedTaskCount;
    private final long currentQueueSize;
    private final long maxThreadPoolSize;
    private final long totalTaskCount;
    private final long largestThreadPoolSize;
    private final long maxQueueSize;
    private final long currentThreadPoolSize;

    // The following fields are defined as Long because they may be null if we can't a value
    // from the underlying executor.
    private final Long largestQueueSize;
    private final Long rejectedTaskCount;

    @ConstructorParameters({"activeThreadCount","currentThreadPoolSize","largestThreadPoolSize",
        "maxThreadPoolSize","currentQueueSize","largestQueueSize","maxQueueSize",
        "completedTaskCount","totalTaskCount","rejectedTaskCount"})
    public ThreadExecutorStats(long activeThreadCount, long currentThreadPoolSize,
            long largestThreadPoolSize, long maxThreadPoolSize, long currentQueueSize,
            Long largestQueueSize, long maxQueueSize, long completedTaskCount,
            long totalTaskCount, Long rejectedTaskCount) {
        this.activeThreadCount = activeThreadCount;
        this.currentThreadPoolSize = currentThreadPoolSize;
        this.largestQueueSize = largestQueueSize;
        this.largestThreadPoolSize = largestThreadPoolSize;
        this.maxThreadPoolSize = maxThreadPoolSize;
        this.currentQueueSize = currentQueueSize;
        this.maxQueueSize = maxQueueSize;
        this.completedTaskCount = completedTaskCount;
        this.totalTaskCount = totalTaskCount;
        this.rejectedTaskCount = rejectedTaskCount;
    }

    public long getActiveThreadCount() {
        return activeThreadCount;
    }

    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    public Long getRejectedTaskCount() {
        return rejectedTaskCount;
    }

    public long getCurrentQueueSize() {
        return currentQueueSize;
    }

    public Long getLargestQueueSize() {
        return largestQueueSize;
    }

    public long getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }

    public long getTotalTaskCount() {
        return totalTaskCount;
    }

    public long getLargestThreadPoolSize() {
        return largestThreadPoolSize;
    }

    public long getMaxQueueSize() {
        return maxQueueSize;
    }

    public long getCurrentThreadPoolSize() {
        return currentThreadPoolSize;
    }
}
