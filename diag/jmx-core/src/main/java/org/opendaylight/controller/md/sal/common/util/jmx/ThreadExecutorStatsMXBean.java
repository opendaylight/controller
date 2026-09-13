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
     * Returns the current thread pool size.
     */
    long getCurrentThreadPoolSize();

    /**
     * Returns the largest thread pool size.
     */
    long getLargestThreadPoolSize();

    /**
     * Returns the maximum thread pool size.
     */
    long getMaxThreadPoolSize();

    /**
     * Returns the current queue size.
     */
    long getCurrentQueueSize();

    /**
     * Returns the largest queue size, if available.
     */
    Long getLargestQueueSize();

    /**
     * Returns the maximum queue size.
     */
    long getMaxQueueSize();

    /**
     * Returns the active thread count.
     */
    long getActiveThreadCount();

    /**
     * Returns the completed task count.
     */
    long getCompletedTaskCount();

    /**
     * Returns the total task count.
     */
    long getTotalTaskCount();

    /**
     * Returns the rejected task count, if available.
     */
    Long getRejectedTaskCount();
}
