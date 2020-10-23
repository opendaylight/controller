/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.CommitStatsMXBean;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;

/**
 * Implementation of the CommitStatsMXBean interface.
 *
 * @author Thomas Pantelis
 */
final class CommitStatsMXBeanImpl extends AbstractMXBean implements CommitStatsMXBean {
    private final DurationStatisticsTracker commitStatsTracker;

    /**
     * Constructor.
     *
     * @param commitStatsTracker the DurationStatsTracker used to obtain the stats.
     * @param mbeantype mBeanType Used as the <code>type</code> property in the bean's ObjectName.
     */
    CommitStatsMXBeanImpl(final @NonNull DurationStatisticsTracker commitStatsTracker,
            final @NonNull String mbeantype) {
        super("CommitStats", mbeantype, null);
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
