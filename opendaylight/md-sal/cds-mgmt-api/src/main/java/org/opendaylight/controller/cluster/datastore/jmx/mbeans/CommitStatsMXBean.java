/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

/**
 * MXBean interface for retrieving write Tx commit statistics.
 *
 * @author Thomas Pantelis
 */
public interface CommitStatsMXBean {
    /**
     * Returns the total number of commits that have occurred.
     *
     * @return Returns the total number of commits that have occurred
     *
     */
    long getTotalCommits();

    /**
     * Returns a string representing the time duration of the longest commit, in the appropriate
     * scaled units, along with the date/time that it occurred.
     *
     * @return string
     */
    String getLongestCommitTime();

    /**
     * Returns a string representing the time duration of the shortest commit, in the appropriate
     * scaled units, along with the date/time that it occurred.
     *
     * @return string
     */
    String getShortestCommitTime();

    /**
     * Returns a string representing average commit time duration, in the appropriate
     * scaled units.
     *
     * @return string
     */
    String getAverageCommitTime();

    /**
     * Clears the current stats to their defaults.
     */
    void clearStats();
}
