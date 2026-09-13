/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.util.jmx;

/**
 * MXBean interface for thread executor statistic metrics.
 *
 * @author Thomas Pantelis
 */
public interface ThreadExecutorStatsMXBean {
    /**
     * {@return the current thread pool size}
     */
    long getCurrentThreadPoolSize();

    /**
     * {@return the largest thread pool size}
     */
    long getLargestThreadPoolSize();

    /**
     * {@return the maximum thread pool size}
     */
    long getMaxThreadPoolSize();

    /**
     * {@return the current queue size}
     */
    long getCurrentQueueSize();

    /**
     * {@return the largest queue size, or {@code null} if not available}
     */
    Long getLargestQueueSize();

    /**
     * {@return the maximum queue size}
     */
    long getMaxQueueSize();

    /**
     * {@return the active thread count}
     */
    long getActiveThreadCount();

    /**
     * {@return the completed task count}
     */
    long getCompletedTaskCount();

    /**
     * {@return the total task count}
     */
    long getTotalTaskCount();

    /**
     * {@return the rejected task count, or {@code null} if not available}
     */
    Long getRejectedTaskCount();
}
