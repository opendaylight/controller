/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.md.sal.dom.statistics;

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * Class that calculates and tracks write Tx commit statistics.
 *
 * @author Thomas Pantelis
 */
public class CommitStatTracker {

    private final AtomicLong totalCommits = new AtomicLong();
    private final AtomicLong longestCommitTime = new AtomicLong();
    private final AtomicLong shortestCommitTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicDouble averageCommitTime = new AtomicDouble();

    /**
     * Add stats for a commit.
     *
     * @param elapsedTime the elapsed time in nanoseconds of the commit
     */
    public void addCommitStats(long elapsedTime) {

        double currentAve = averageCommitTime.get();
        long currentTotal = totalCommits.get();

        long newTotal = currentTotal + 1;

        // Calculate moving cumulative average.
        double newAve = currentAve * currentTotal / newTotal + elapsedTime / newTotal;

        averageCommitTime.compareAndSet(currentAve, newAve);
        totalCommits.compareAndSet(currentTotal, newTotal);

        long longest = longestCommitTime.get();
        if( elapsedTime > longest ) {
            longestCommitTime.compareAndSet( longest, elapsedTime );
        }

        long shortest = shortestCommitTime.get();
        if( elapsedTime < shortest ) {
            shortestCommitTime.compareAndSet( shortest, elapsedTime );
        }
    }

    /**
     * Returns the total number of commits.
     */
    public long getTotalCommits() {
        return totalCommits.get();
    }

    /**
     * Returns the longest commit time in nanoseconds.
     */
    public long getLongestCommitTime() {
        return longestCommitTime.get();
    }

    /**
     * Returns the shortest commit time in nanoseconds.
     */
    public long getShortestCommitTime() {
        return shortestCommitTime.get();
    }

    /**
     * Returns the average commit time in nanoseconds.
     */
    public double getAverageCommitTime() {
        return averageCommitTime.get();
    }
}
