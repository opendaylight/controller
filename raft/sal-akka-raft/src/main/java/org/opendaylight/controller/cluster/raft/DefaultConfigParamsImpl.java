/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.time.Duration;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.RaftPolicy;
import org.opendaylight.raft.spi.WellKnownRaftPolicy;

/**
 * Default implementation of the ConfigParams.
 */
public class DefaultConfigParamsImpl implements ConfigParams {
    private static final Path EMPTY_PATH = Path.of("");

    private static final int SNAPSHOT_BATCH_COUNT = 20000;
    /**
     * Interval after which a snapshot should be taken during the recovery process. 0 if never.
     */
    private static final int RECOVERY_SNAPSHOT_INTERVAL_SECONDS = 0;

    private static final int JOURNAL_RECOVERY_LOG_BATCH_SIZE = 1000;

    /**
     * The maximum election time variance.
     */
    private static final int ELECTION_TIME_MAX_VARIANCE = 100;

    private static final int MAXIMUM_MESSAGE_SLICE_SIZE = 480 * 1024; // 480KiB


    /**
     * The interval at which a heart beat message will be sent to the remote RaftActor.
     *
     * <p>Since this is set to 100 milliseconds the Election timeout should be at least 200 milliseconds.
     */
    public static final Duration HEART_BEAT_INTERVAL = Duration.ofMillis(100);

    private @NonNull RaftPolicy raftPolicy = WellKnownRaftPolicy.NORMAL;

    private Duration heartBeatInterval = HEART_BEAT_INTERVAL;
    private long snapshotBatchCount = SNAPSHOT_BATCH_COUNT;
    private int journalRecoveryLogBatchSize = JOURNAL_RECOVERY_LOG_BATCH_SIZE;
    private int recoverySnapshotIntervalSeconds = RECOVERY_SNAPSHOT_INTERVAL_SECONDS;
    private long isolatedLeaderCheckInterval = HEART_BEAT_INTERVAL.multipliedBy(1000).toMillis();
    private Duration electionTimeOutInterval;

    // 12 is just an arbitrary percentage. This is the amount of the total memory that a raft actor's
    // in-memory journal can use before it needs to snapshot
    private int snapshotDataThresholdPercentage = 12;

    // max size of in-memory journal in MB
    // 0 means direct threshold if disabled
    private int snapshotDataThreshold = 0;

    private int maximumMessageSliceSize = MAXIMUM_MESSAGE_SLICE_SIZE;

    private long electionTimeoutFactor = 2;
    private long candidateElectionTimeoutDivisor = 1;

    private PeerAddressResolver peerAddressResolver = NoopPeerAddressResolver.INSTANCE;

    private Path tempFileDirectory = EMPTY_PATH;

    private int fileBackedStreamingThreshold = 128 * 1_048_576;

    private long syncIndexThreshold = 10;

    private @NonNull CompressionType preferredCompression = CompressionType.NONE;

    public void setHeartBeatInterval(final Duration heartBeatInterval) {
        this.heartBeatInterval = requireNonNull(heartBeatInterval);
        electionTimeOutInterval = null;
    }

    public void setSnapshotBatchCount(final long snapshotBatchCount) {
        this.snapshotBatchCount = snapshotBatchCount;
    }

    public void setRecoverySnapshotIntervalSeconds(final int recoverySnapshotInterval) {
        checkArgument(recoverySnapshotInterval >= 0);
        recoverySnapshotIntervalSeconds = recoverySnapshotInterval;
    }

    public void setSnapshotDataThresholdPercentage(final int snapshotDataThresholdPercentage) {
        this.snapshotDataThresholdPercentage = snapshotDataThresholdPercentage;
    }

    public void setSnapshotDataThreshold(final int snapshotDataThreshold) {
        this.snapshotDataThreshold = snapshotDataThreshold;
    }

    public void setMaximumMessageSliceSize(final int maximumMessageSliceSize) {
        this.maximumMessageSliceSize = maximumMessageSliceSize;
    }

    public void setJournalRecoveryLogBatchSize(final int journalRecoveryLogBatchSize) {
        this.journalRecoveryLogBatchSize = journalRecoveryLogBatchSize;
    }

    public void setIsolatedLeaderCheckInterval(final Duration isolatedLeaderCheckInterval) {
        this.isolatedLeaderCheckInterval = isolatedLeaderCheckInterval.toMillis();
    }

    public void setElectionTimeoutFactor(final long electionTimeoutFactor) {
        this.electionTimeoutFactor = electionTimeoutFactor;
        electionTimeOutInterval = null;
    }

    public void setCandidateElectionTimeoutDivisor(final long candidateElectionTimeoutDivisor) {
        this.candidateElectionTimeoutDivisor = candidateElectionTimeoutDivisor;
    }

    public void setTempFileDirectory(final Path tempFileDirectory) {
        this.tempFileDirectory = requireNonNull(tempFileDirectory);
    }

    public void setFileBackedStreamingThreshold(final int fileBackedStreamingThreshold) {
        this.fileBackedStreamingThreshold = fileBackedStreamingThreshold;
    }

    public void setRaftPolicy(final RaftPolicy newRaftPolicy) {
        raftPolicy = requireNonNull(newRaftPolicy);
    }

    @Override
    public long getSnapshotBatchCount() {
        return snapshotBatchCount;
    }

    @Override
    public int getSnapshotDataThresholdPercentage() {
        return snapshotDataThresholdPercentage;
    }

    @Override
    public int getSnapshotDataThreshold() {
        return snapshotDataThreshold;
    }

    @Override
    public int getRecoverySnapshotIntervalSeconds() {
        return recoverySnapshotIntervalSeconds;
    }

    @Override
    public Duration getHeartBeatInterval() {
        return heartBeatInterval;
    }

    @Override
    public Duration getElectionTimeOutInterval() {
        if (electionTimeOutInterval == null) {
            electionTimeOutInterval = getHeartBeatInterval().multipliedBy(electionTimeoutFactor);
        }
        return electionTimeOutInterval;
    }

    @Override
    public long getCandidateElectionTimeoutDivisor() {
        return candidateElectionTimeoutDivisor;
    }

    @Override
    public int getElectionTimeVariance() {
        return ELECTION_TIME_MAX_VARIANCE;
    }

    @Override
    public int getMaximumMessageSliceSize() {
        return maximumMessageSliceSize;
    }

    @Override
    public int getJournalRecoveryLogBatchSize() {
        return journalRecoveryLogBatchSize;
    }

    @Override
    public long getIsolatedCheckIntervalInMillis() {
        return isolatedLeaderCheckInterval;
    }

    @Override
    public long getElectionTimeoutFactor() {
        return electionTimeoutFactor;
    }

    @Override
    public RaftPolicy getRaftPolicy() {
        return raftPolicy;
    }

    @Override
    public Path getTempFileDirectory() {
        return tempFileDirectory;
    }

    @Override
    public int getFileBackedStreamingThreshold() {
        return fileBackedStreamingThreshold;
    }


    @Override
    public PeerAddressResolver getPeerAddressResolver() {
        return peerAddressResolver;
    }

    public void setPeerAddressResolver(final @NonNull PeerAddressResolver peerAddressResolver) {
        this.peerAddressResolver = requireNonNull(peerAddressResolver);
    }

    @Override
    public long getSyncIndexThreshold() {
        return syncIndexThreshold;
    }

    public void setSyncIndexThreshold(final long syncIndexThreshold) {
        checkArgument(syncIndexThreshold >= 0);
        this.syncIndexThreshold = syncIndexThreshold;
    }

    @Override
    public CompressionType getPreferredCompression() {
        return preferredCompression;
    }

    public void setPreferredCompression(final CompressionType preferredFileFormat) {
        preferredCompression = requireNonNull(preferredFileFormat);
    }
}
