/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.md.sal.dom.statistics;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for CommitStatTracker.
 *
 * @author Thomas Pantelis
 */
public class CommitStatsTrackerTest {

    @Test
    public void test() {

        CommitStatsTracker tracker = new CommitStatsTracker();

        tracker.addCommitStats(10000);
        assertEquals("getTotalCommits", 1, tracker.getTotalCommits());
        assertEquals("getAverageCommitTime", 10000.0, tracker.getAverageCommitTime(), 0.1);
        assertEquals("getLongestCommitTime", 10000, tracker.getLongestCommitTime());
        assertEquals("getShortestCommitTime", 10000, tracker.getShortestCommitTime());

        tracker.addCommitStats(30000);
        assertEquals("getTotalCommits", 2, tracker.getTotalCommits());
        assertEquals("getAverageCommitTime", 20000.0, tracker.getAverageCommitTime(), 0.1);
        assertEquals("getLongestCommitTime", 30000, tracker.getLongestCommitTime());
        assertEquals("getShortestCommitTime", 10000, tracker.getShortestCommitTime());

        tracker.addCommitStats(10000);
        assertEquals("getTotalCommits", 3, tracker.getTotalCommits());
        assertEquals("getAverageCommitTime", 16666.0, tracker.getAverageCommitTime(), 1.0);
        assertEquals("getLongestCommitTime", 30000, tracker.getLongestCommitTime());
        assertEquals("getShortestCommitTime", 10000, tracker.getShortestCommitTime());

        tracker.addCommitStats(5000);
        assertEquals("getTotalCommits", 4, tracker.getTotalCommits());
        assertEquals("getAverageCommitTime", 13750.0, tracker.getAverageCommitTime(), 1.0);
        assertEquals("getLongestCommitTime", 30000, tracker.getLongestCommitTime());
        assertEquals("getShortestCommitTime", 5000, tracker.getShortestCommitTime());
    }
}
