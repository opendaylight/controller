/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
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
     * Returns the minimum number of entries to be present in the in-memory Raft log for a snapshot to be taken.
     *
     * @return the minimum number of entries.
     */
    long getSnapshotBatchCount();

    /**
     * Returns the percentage of total memory used in the in-memory Raft log before a snapshot should be taken.
     *
     * @return the percentage.
     */
    int getSnapshotDataThresholdPercentage();

    /**
     * Returns the interval at which a heart beat message should be sent to remote followers.
     *
     * @return the interval as a FiniteDuration.
     */
    FiniteDuration getHeartBeatInterval();

    /**
     * Returns the interval after which a new election should be triggered if no leader is available.
     *
     * @return the interval as a FiniteDuration.
     */
    FiniteDuration getElectionTimeOutInterval();

    /**
     * Returns the maximum election time variance. The election is scheduled using both the election timeout and variance.
     *
     * @return the election time variance.
     */
    int getElectionTimeVariance();

    /**
     * Returns the maximum size (in bytes) for the snapshot chunk sent from a Leader.
     *
     * @return the maximum size (in bytes).
     */
    int getSnapshotChunkSize();

    /**
     * Returns the maximum number of journal log entries to batch on recovery before applying.
     *
     * @return the maximum number of journal log entries.
     */
    int getJournalRecoveryLogBatchSize();

    /**
     * Returns the interval in which the leader needs to check if its isolated.
     *
     * @return the interval in ms.
     */
    long getIsolatedCheckIntervalInMillis();


    /**
     * Returns the multiplication factor to be used to determine the shard election timeout. The election timeout
     * is determined by multiplying the election timeout factor with the heart beat duration.
     *
     * @return the election timeout factor.
     */
    long getElectionTimeoutFactor();


    /**
     * Returns the RaftPolicy used to determine certain Raft behaviors.
     *
     * @return an instance of org.opendaylight.controller.cluster.raft.policy.RaftPolicy, if set,  or an instance of the
     * DefaultRaftPolicy.
     */
    @Nonnull
    RaftPolicy getRaftPolicy();

    /**
     * Returns the PeerAddressResolver.
     *
     * @return the PeerAddressResolver instance.
     */
    @Nonnull
    PeerAddressResolver getPeerAddressResolver();

    /**
     * Returns the custom RaftPolicy class name.
     *
     * @return the RaftPolicy class name or null if none set.
     */
    String getCustomRaftPolicyImplementationClass();

}
