/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.jmx;

import org.opendaylight.yangtools.util.DurationStatsTracker;
import org.opendaylight.yangtools.util.jmx.AbstractMXBean;

/**
 * Implementation of the CommitStatsMXBean interface.
 *
 * @author Thomas Pantelis
 */
public class CommitStatsMXBeanImpl extends AbstractMXBean implements CommitStatsMXBean {

    private final DurationStatsTracker commitStatsTracker;

    public CommitStatsMXBeanImpl(DurationStatsTracker commitStatsTracker, String mBeanType) {
        super("CommitStats", mBeanType, null);
        this.commitStatsTracker = commitStatsTracker;
    }

    @Override
    public long getTotalCommits() {
        return commitStatsTracker.getTotalDurations();
    }

    @Override
    public String getLongestCommitTime() {
        return commitStatsTracker.getDisplayableLongestDuration();
    }

    @Override
    public String getShortestCommitTime() {
        return commitStatsTracker.getDisplayableShortestDuration();
    }

    @Override
    public String getAverageCommitTime() {
        return commitStatsTracker.getDisplayableAverageDuration();
    }

    @Override
    public void clearStats() {
        commitStatsTracker.reset();
    }
}
