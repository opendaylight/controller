/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.api.jmx;

import java.beans.ConstructorProperties;

public class ThreadExecutorStats {

    private final Long activeThreadCount;
    private final Long completedTaskCount;
    private final Long currentQueueSize;
    private final Long maxThreadPoolSize;
    private final Long totalTaskCount;
    private final Long largestThreadPoolSize;
    private final Long maxQueueSize;
    private final Long currentThreadPoolSize;

    @ConstructorProperties({"activeThreadCount","currentThreadPoolSize","largestThreadPoolSize",
        "maxThreadPoolSize","currentQueueSize","maxQueueSize","completedTaskCount","totalTaskCount"})
    public ThreadExecutorStats(Long activeThreadCount, Long currentThreadPoolSize,
            Long largestThreadPoolSize, Long maxThreadPoolSize, Long currentQueueSize,
            Long maxQueueSize, Long completedTaskCount, Long totalTaskCount) {
        this.activeThreadCount = activeThreadCount;
        this.currentThreadPoolSize = currentThreadPoolSize;
        this.largestThreadPoolSize = largestThreadPoolSize;
        this.maxThreadPoolSize = maxThreadPoolSize;
        this.currentQueueSize = currentQueueSize;
        this.maxQueueSize = maxQueueSize;
        this.completedTaskCount = completedTaskCount;
        this.totalTaskCount = totalTaskCount;
    }

    public Long getActiveThreadCount() {
        return activeThreadCount;
    }

    public Long getCompletedTaskCount() {
        return completedTaskCount;
    }

    public Long getCurrentQueueSize() {
        return currentQueueSize;
    }

    public Long getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }

    public Long getTotalTaskCount() {
        return totalTaskCount;
    }

    public Long getLargestThreadPoolSize() {
        return largestThreadPoolSize;
    }

    public Long getMaxQueueSize() {
        return maxQueueSize;
    }

    public Long getCurrentThreadPoolSize() {
        return currentThreadPoolSize;
    }

//    public void setCompletedTaskCount(Long completedTaskCount) {
//        this.completedTaskCount = completedTaskCount;
//    }
//
//    public void setCurrentQueueSize(Long currentQueueSize) {
//        this.currentQueueSize = currentQueueSize;
//    }
//
//    public void setMaxThreadPoolSize(Long maxThreadPoolSize) {
//        this.maxThreadPoolSize = maxThreadPoolSize;
//    }
//
//    public void setTotalTaskCount(Long totalTaskCount) {
//        this.totalTaskCount = totalTaskCount;
//    }
//
//    public void setLargestThreadPoolSize(Long largestThreadPoolSize) {
//        this.largestThreadPoolSize = largestThreadPoolSize;
//    }
//
//    public void setMaxQueueSize(Long maxQueueSize) {
//        this.maxQueueSize = maxQueueSize;
//    }
//
//    public void setCurrentThreadPoolSize(Long currentThreadPoolSize) {
//        this.currentThreadPoolSize = currentThreadPoolSize;
//    }
//
//    public void setActiveThreadCount(Long activeThreadCount) {
//        this.activeThreadCount = activeThreadCount;
//    }
}
