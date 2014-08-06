/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.md.sal.dom.statistics;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutorService;

import org.junit.Test;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.CommitExecutorStats;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.CommitFutureExecutorStats;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;

public class DomInmemoryDataBrokerRuntimeMXBeanImplTest {

    @Test
    public void testGetExecutorStats() {

        ExecutorService commitExecutor = SpecialExecutors.newBoundedSingleThreadExecutor(10, "test");
        ExecutorService commitFutureExecutor = SpecialExecutors.newBoundedCachedThreadPool(5, 20, "test");
        DomInmemoryDataBrokerRuntimeMXBeanImpl bean =
                new DomInmemoryDataBrokerRuntimeMXBeanImpl(commitExecutor, commitFutureExecutor, null);

        CommitExecutorStats commitStats = bean.getCommitExecutorStats();
        assertEquals("getActiveThreadCount", Long.valueOf(0), commitStats.getActiveThreadCount());
        assertEquals("getCompletedTaskCount", Long.valueOf(0), commitStats.getCompletedTaskCount());
        assertEquals("getCurrentQueueSize", Long.valueOf(0), commitStats.getCurrentQueueSize());
        assertEquals("getCurrentThreadPoolSize", Long.valueOf(0), commitStats.getCurrentThreadPoolSize());
        assertEquals("getLargestThreadPoolSize", Long.valueOf(0), commitStats.getLargestThreadPoolSize());
        assertEquals("getMaxQueueSize", Long.valueOf(10), commitStats.getMaxQueueSize());
        assertEquals("getMaxThreadPoolSize", Long.valueOf(1), commitStats.getMaxThreadPoolSize());
        assertEquals("getTotalTaskCount", Long.valueOf(0), commitStats.getTotalTaskCount());

        CommitFutureExecutorStats futureStats = bean.getCommitFutureExecutorStats();
        assertEquals("getActiveThreadCount", Long.valueOf(0), futureStats.getActiveThreadCount());
        assertEquals("getCompletedTaskCount", Long.valueOf(0), futureStats.getCompletedTaskCount());
        assertEquals("getCurrentQueueSize", Long.valueOf(0), futureStats.getCurrentQueueSize());
        assertEquals("getCurrentThreadPoolSize", Long.valueOf(0), futureStats.getCurrentThreadPoolSize());
        assertEquals("getLargestThreadPoolSize", Long.valueOf(0), futureStats.getLargestThreadPoolSize());
        assertEquals("getMaxQueueSize", Long.valueOf(20), futureStats.getMaxQueueSize());
        assertEquals("getMaxThreadPoolSize", Long.valueOf(5), futureStats.getMaxThreadPoolSize());
        assertEquals("getTotalTaskCount", Long.valueOf(0), futureStats.getTotalTaskCount());
    }

}
