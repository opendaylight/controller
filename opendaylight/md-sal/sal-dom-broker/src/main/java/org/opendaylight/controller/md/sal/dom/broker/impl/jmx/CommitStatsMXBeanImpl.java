/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.jmx;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.sal.core.api.jmx.AbstractMXBean;

/**
 * Implementation of the CommitStatsMXBean interface.
 *
 * @author Thomas Pantelis
 */
public class CommitStatsMXBeanImpl extends AbstractMXBean implements CommitStatsMXBean {

    private final CommitStatsTracker commitStatsTracker;

    public CommitStatsMXBeanImpl(CommitStatsTracker commitStatsTracker, String mBeanType) {
        super("CommitStats", mBeanType, null);
        this.commitStatsTracker = commitStatsTracker;
    }

    @Override
    public long getTotalCommits() {
        return commitStatsTracker.getTotalCommits();
    }

    @Override
    public String getLongestCommitTime() {
        return formatDuration(commitStatsTracker.getLongestCommitTime(),
                commitStatsTracker.getTimeOfLongestCommit());
    }

    @Override
    public String getShortestCommitTime() {
        return formatDuration(commitStatsTracker.getShortestCommitTime(),
                commitStatsTracker.getTimeOfShortestCommit());
    }

    @Override
    public String getAverageCommitTime() {
        return formatDuration(commitStatsTracker.getAverageCommitTime(), 0);
    }

    @Override
    public void clearStats() {
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
