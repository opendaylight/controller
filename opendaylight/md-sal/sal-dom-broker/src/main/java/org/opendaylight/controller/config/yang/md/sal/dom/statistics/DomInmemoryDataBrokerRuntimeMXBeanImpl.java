/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.md.sal.dom.statistics;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.config.yang.md.sal.dom.impl.CommitExecutorStats;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.CommitFutureExecutorStats;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.CommitStats;
import org.opendaylight.controller.config.yang.md.sal.dom.impl.DomInmemoryDataBrokerRuntimeMXBean;
import org.opendaylight.controller.sal.core.api.statistics.ThreadExecutorStatsHelper;

/**
 * Bean class that provides JMX information pertaining to the data broker.
 *
 * @author Thomas Pantelis
 */
public class DomInmemoryDataBrokerRuntimeMXBeanImpl implements DomInmemoryDataBrokerRuntimeMXBean {

    private final ExecutorService commitExecutor;
    private final ExecutorService commitFutureExecutor;
    private final CommitStatsTracker commitStatsTracker;

    public DomInmemoryDataBrokerRuntimeMXBeanImpl(ExecutorService commitExecutor,
            ExecutorService commitFutureExecutor,
            CommitStatsTracker commitStatTracker) {
        this.commitExecutor = commitExecutor;
        this.commitFutureExecutor = commitFutureExecutor;
        this.commitStatsTracker = commitStatTracker;
    }

    @Override
    public CommitExecutorStats getCommitExecutorStats() {
        return ThreadExecutorStatsHelper.newStatsInstance(
                commitExecutor, CommitExecutorStats.class);
    }

    @Override
    public CommitFutureExecutorStats getCommitFutureExecutorStats() {
        return ThreadExecutorStatsHelper.newStatsInstance(
                commitFutureExecutor, CommitFutureExecutorStats.class);
    }

    @Override
    public CommitStats getCommitStats() {

        CommitStats stats = new CommitStats();
        stats.setTotalCommits(commitStatsTracker.getTotalCommits());
        stats.setAverageCommitTime(formatDuration(commitStatsTracker.getAverageCommitTime(), 0));
        stats.setLongestCommitTime(formatDuration(commitStatsTracker.getLongestCommitTime(),
                commitStatsTracker.getTimeOfLongestCommit()));
        stats.setShortestCommitTime(formatDuration(commitStatsTracker.getShortestCommitTime(),
                commitStatsTracker.getTimeOfShortestCommit()));
        return stats;
    }

    @Override
    public void clearCommitStats() {
        commitStatsTracker.clear();
    }

    private String formatDuration(double duration, long timeStamp) {
        TimeUnit unit = chooseUnit((long)duration);
        double value = duration / NANOSECONDS.convert(1, unit);
        return timeStamp > 0 ?
                String.format("%.4g %s at %3$tD %3$tT", value, abbreviate(unit), new Date(timeStamp)) :
                String.format("%.4g %s", value, abbreviate(unit));
    }

    private static TimeUnit chooseUnit(long nanos) {
        if(SECONDS.convert(nanos, NANOSECONDS) > 0) {
            return SECONDS;
        }

        if(MILLISECONDS.convert(nanos, NANOSECONDS) > 0) {
            return MILLISECONDS;
        }

        if(MICROSECONDS.convert(nanos, NANOSECONDS) > 0) {
            return MICROSECONDS;
        }

        return NANOSECONDS;
    }

    private static String abbreviate(TimeUnit unit) {
        switch(unit) {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "\u03bcs"; // μs
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            default:
                return "";
        }
    }
}
