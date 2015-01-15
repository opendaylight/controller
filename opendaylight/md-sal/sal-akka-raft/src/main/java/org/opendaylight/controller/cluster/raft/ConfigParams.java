/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import scala.concurrent.duration.FiniteDuration;

/**
 * Configuration Parameter interface for configuring the Raft consensus system
 * <p/>
 * Any component using this implementation might want to provide an implementation of
 * this interface to configure
 *
 * A default implementation will be used if none is provided.
 *
 * @author Kamal Rameshan
 */
public interface ConfigParams {
    /**
     * The minimum number of entries to be present in the in-memory Raft log
     * for a snapshot to be taken
     *
     * @return long
     */
    long getSnapshotBatchCount();

    /**
     * The percentage of total memory in the in-memory Raft log before a snapshot
     * is to be taken
     *
     * @return int
     */
    int getSnapshotDataThresholdPercentage();

    /**
     * The interval at which a heart beat message will be sent to the remote
     * RaftActor
     *
     * @return FiniteDuration
     */
    FiniteDuration getHeartBeatInterval();

    /**
     * The interval in which a new election would get triggered if no leader is found
     *
     * Normally its set to atleast twice the heart beat interval
     *
     * @return FiniteDuration
     */
    FiniteDuration getElectionTimeOutInterval();

    /**
     * The maximum election time variance. The election is scheduled using both
     * the Election Timeout and Variance
     *
     * @return int
     */
    int getElectionTimeVariance();

    /**
     * The size (in bytes) of the snapshot chunk sent from Leader
     */
    int getSnapshotChunkSize();

    /**
     * The number of journal log entries to batch on recovery before applying.
     */
    int getJournalRecoveryLogBatchSize();

    /**
     * The interval in which the leader needs to check itself if its isolated
     * @return FiniteDuration
     */
    FiniteDuration getIsolatedCheckInterval();


    /**
     * The multiplication factor to be used to determine shard election timeout. The election timeout
     * is determined by multiplying the election timeout factor with the heartbeat duration.
     */
    long getElectionTimeoutFactor();

}
