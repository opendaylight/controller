/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.jmx;

/**
 * MXBean interface for retrieving write Tx commit statistics.
 *
 * @author Thomas Pantelis
 */
public interface CommitStatsMXBean {

    /**
     * Returns the total number of commits that have occurred.
     */
    long getTotalCommits();

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
     * Clears the current stats to their defaults.
     */
    void clearStats();
}
