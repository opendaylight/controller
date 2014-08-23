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

    long getTotalCommits();

    String getLongestCommitTime();

    String getShortestCommitTime();

    String getAverageCommitTime();

    void clearStats();
}
