/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.jmx;

/**
 * MXBean interface for retrieving transaction statistics.
 *
 * @author Thomas Pantelis
 */
public interface TransactionStatsMXBean {

    /**
     * Returns the total number of commits that have occurred.
     */
    long getTotalCommits();

    /**
     * Returns the total number of commits that have failed during the can-commit phaes.
     */
    long getCanCommitPhaseFailures();

    /**
     * Returns the total number of commits that have failed during the pre-commit phaes.
     */
    long getPreCommitPhaseFailures();


    /**
     * Returns the total number of commits that have failed during the commit phaes.
     */
    long getCommitPhaseFailures();

    /**
     * Returns the total number of commits that have failed due to optimistic lock error.
     */
    long getOptimisticLockFailures();

    /**
     * Returns a string representing the time duration of the longest commit, in the appropriate
     * scaled units, along with the date/time that it occurred.
     */
    String getLongestCommitTime();

    /**
     * Returns a string representing the time duration of the shortest commit, in the appropriate
     * scaled units, along with the date/time that it occurred.
     */
    String getShortestCommitTime();

    /**
     * Returns a string representing average commit time duration, in the appropriate
     * scaled units.
     */
    String getAverageCommitTime();

    /**
     * Returns the total number of delete operations that have failed.
     */
    long getFailedDeletes();

    /**
     * Returns the total number of delete operations that have succeeded.
     */
    long getSuccessfulDeletes();

    /**
     * Returns the total number of write operations that have failed.
     */
    long getFailedWrites();

    /**
     * Returns the total number of write operations that have succeeded.
     */
    long getSuccessfulWrites();

    /**
     * Returns the total number of read operations that have failed.
     */
    long getFailedReads();

    /**
     * Returns the total number of read operations that have succeeded.
     */
    long getSuccessfulReads();

    /**
     * Clears the current stats to their defaults.
     */
    void clearStatistics();
}
