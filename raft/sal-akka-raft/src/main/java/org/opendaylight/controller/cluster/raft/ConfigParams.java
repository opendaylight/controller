/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.time.Duration;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.opendaylight.raft.spi.SnapshotFileFormat;

/**
 * Configuration Parameter interface for configuring the Raft consensus system. Any component using this implementation
 * might want to provide an implementation of this interface to configure. A default implementation will be used if none
 * is provided.
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
     * Disabled when direct threshold is enabled.
     *
     * @return the percentage.
     */
    int getSnapshotDataThresholdPercentage();

    /**
     * Returns the max size of memory used in the in-memory Raft log before a snapshot should be taken. 0 means that
     * direct threshold is disabled and percentage is used instead.
     *
     * @return maximum journal size (in MiB).
     */
    int getSnapshotDataThreshold();

    /**
     * Returns the interval(in seconds) after which a snapshot should be taken during recovery. Negative value means
     * do not take snapshots.
     *
     * @return the interval of recovery snapshot in seconds
     */
    int getRecoverySnapshotIntervalSeconds();

    /**
     * Returns the interval at which a heart beat message should be sent to remote followers.
     *
     * @return the interval as a {@link Duration}.
     */
    Duration getHeartBeatInterval();

    /**
     * Returns the interval after which a new election should be triggered if no leader is available.
     *
     * @return the interval as a {@link Duration}.
     */
    Duration getElectionTimeOutInterval();

    /**
     * Returns the number by which a candidate should divide the election timeout it has calculated. This serves
     * to speed up retries when elections result in a stalemate.
     *
     * @return the interval as a FiniteDuration.
     */
    long getCandidateElectionTimeoutDivisor();

    /**
     * Returns the maximum election time variance. The election is scheduled using both the election timeout
     * and variance.
     *
     * @return the election time variance.
     */
    int getElectionTimeVariance();

    /**
     * Returns the maximum size (in bytes) for the snapshot chunk sent from a Leader.
     *
     * @return the maximum size (in bytes).
     */
    int getMaximumMessageSliceSize();

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
     * @return an instance of RaftPolicy, if set, or an instance of the DefaultRaftPolicy.
     */
    @NonNull RaftPolicy getRaftPolicy();

    /**
     * Returns the PeerAddressResolver.
     *
     * @return the PeerAddressResolver instance.
     */
    @NonNull PeerAddressResolver getPeerAddressResolver();

    /**
     * Returns the custom RaftPolicy class name.
     *
     * @return the RaftPolicy class name or null if none set.
     */
    String getCustomRaftPolicyImplementationClass();

    /**
     * Returns the directory in which to create temp files.
     *
     * @return the directory in which to create temp files.
     */
    @NonNull String getTempFileDirectory();

    /**
     * Returns the threshold in terms of number of bytes when streaming data before it should switch from storing in
     * memory to buffering to a file.
     *
     * @return the threshold in terms of number of bytes.
     */
    int getFileBackedStreamingThreshold();

    /**
     * Returns the threshold in terms of number journal entries that we can lag behind a leader until we raise a
     * 'not synced' transition.
     *
     * @return the threshold in terms of number of journal entries.
     */
    long getSyncIndexThreshold();

    /**
     * Retuns the preferred {@link SnapshotFileFormat}.
     *
     * @return the preferred {@link SnapshotFileFormat}
     */
    @NonNull SnapshotFileFormat getPreferredFileFormat();
}
