/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.jmx;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.yangtools.util.DurationStatsTracker;

/**
 * Unit tests for CommitStatsMXBeanImpl.
 *
 * @author Thomas Pantelis
 */
public class CommitStatsMXBeanImplTest {

    @Test
    public void test() {

        DurationStatsTracker commitStatsTracker = new DurationStatsTracker();
        CommitStatsMXBeanImpl bean =
                new CommitStatsMXBeanImpl(commitStatsTracker, "Test");

        commitStatsTracker.addDuration(100);

        String prefix = "100.0 ns";
        assertEquals("getTotalCommits", 1L, bean.getTotalCommits());
        assertEquals("getLongestCommitTime starts with \"" + prefix + "\"", true,
                     bean.getLongestCommitTime().startsWith("100.0 ns"));
        assertEquals("getShortestCommitTime starts with \"" + prefix + "\"", true,
                     bean.getShortestCommitTime().startsWith(prefix));
        assertEquals("getAverageCommitTime starts with \"" + prefix + "\"", true,
                     bean.getAverageCommitTime().startsWith(prefix));
    }
}
