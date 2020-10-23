/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;

/**
 * Unit tests for CommitStatsMXBeanImpl.
 *
 * @author Thomas Pantelis
 */
public class CommitStatsMXBeanImplTest {
    @Test
    public void test() {
        DurationStatisticsTracker commitStatsTracker = DurationStatisticsTracker.createConcurrent();
        CommitStatsMXBeanImpl bean = new CommitStatsMXBeanImpl(commitStatsTracker, "Test");

        commitStatsTracker.addDuration(100);

        assertEquals("getTotalCommits", 1L, bean.getTotalCommits());
        assertNotNull(bean.getLongestCommitTime());
        assertNotNull(bean.getShortestCommitTime());
        assertNotNull(bean.getAverageCommitTime());
    }
}
