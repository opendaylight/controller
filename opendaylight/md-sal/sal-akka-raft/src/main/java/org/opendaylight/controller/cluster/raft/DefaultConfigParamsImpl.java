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

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.policy.DefaultRaftPolicy;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Default implementation of the ConfigParams.
 */
public class DefaultConfigParamsImpl implements ConfigParams {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultConfigParamsImpl.class);

    private static final int SNAPSHOT_BATCH_COUNT = 20000;
    /**
     * Number of entries recovered before snapshot is taken during the incremental recovery process.
     * 0 means the incremental recovery won't be used.
     */
    private static final int INCREMENTAL_RECOVERY_CHUNK = 0;

    private static final int JOURNAL_RECOVERY_LOG_BATCH_SIZE = 1000;

    /**
     * The maximum election time variance.
     */
    private static final int ELECTION_TIME_MAX_VARIANCE = 100;

    private static final int SNAPSHOT_CHUNK_SIZE = 2048 * 1000; //2MB


    /**
     * The interval at which a heart beat message will be sent to the remote
     * RaftActor.
     *
     * <p>
     * Since this is set to 100 milliseconds the Election timeout should be
     * at least 200 milliseconds
     */
    public static final FiniteDuration HEART_BEAT_INTERVAL =
        new FiniteDuration(100, TimeUnit.MILLISECONDS);

    private final Supplier<RaftPolicy> policySupplier = Suppliers.memoize(this::getPolicy);

    private FiniteDuration heartBeatInterval = HEART_BEAT_INTERVAL;
    private long snapshotBatchCount = SNAPSHOT_BATCH_COUNT;
    private int journalRecoveryLogBatchSize = JOURNAL_RECOVERY_LOG_BATCH_SIZE;
    private int recoveredEntriesBeforeSnapshot = INCREMENTAL_RECOVERY_CHUNK;
    private long isolatedLeaderCheckInterval = HEART_BEAT_INTERVAL.$times(1000).toMillis();
    private FiniteDuration electionTimeOutInterval;

    // 12 is just an arbitrary percentage. This is the amount of the total memory that a raft actor's
    // in-memory journal can use before it needs to snapshot
    private int snapshotDataThresholdPercentage = 12;

    private int snapshotChunkSize = SNAPSHOT_CHUNK_SIZE;

    private long electionTimeoutFactor = 2;
    private long candidateElectionTimeoutDivisor = 1;
    private String customRaftPolicyImplementationClass;

    private PeerAddressResolver peerAddressResolver = NoopPeerAddressResolver.INSTANCE;

    private String tempFileDirectory = "";

    private int fileBackedStreamingThreshold = 128 * MEGABYTE;

    private long syncIndexThreshold = 10;

    public void setHeartBeatInterval(final FiniteDuration heartBeatInterval) {
        this.heartBeatInterval = heartBeatInterval;
        electionTimeOutInterval = null;
    }

    public void setSnapshotBatchCount(final long snapshotBatchCount) {
        this.snapshotBatchCount = snapshotBatchCount;
    }

    public void setRecoveredEntriesBeforeSnapshot(int recoveredEntriesBeforeSnapshot) {
        this.recoveredEntriesBeforeSnapshot = recoveredEntriesBeforeSnapshot;
    }

    public void setSnapshotDataThresholdPercentage(final int snapshotDataThresholdPercentage) {
        this.snapshotDataThresholdPercentage = snapshotDataThresholdPercentage;
    }

    public void setSnapshotChunkSize(final int snapshotChunkSize) {
        this.snapshotChunkSize = snapshotChunkSize;
    }

    public void setJournalRecoveryLogBatchSize(final int journalRecoveryLogBatchSize) {
        this.journalRecoveryLogBatchSize = journalRecoveryLogBatchSize;
    }

    public void setIsolatedLeaderCheckInterval(final FiniteDuration isolatedLeaderCheckInterval) {
        this.isolatedLeaderCheckInterval = isolatedLeaderCheckInterval.toMillis();
    }

    public void setElectionTimeoutFactor(final long electionTimeoutFactor) {
        this.electionTimeoutFactor = electionTimeoutFactor;
        electionTimeOutInterval = null;
    }

    public void setCandidateElectionTimeoutDivisor(final long candidateElectionTimeoutDivisor) {
        this.candidateElectionTimeoutDivisor = candidateElectionTimeoutDivisor;
    }

    public void setTempFileDirectory(final String tempFileDirectory) {
        this.tempFileDirectory = tempFileDirectory;
    }

    public void setFileBackedStreamingThreshold(final int fileBackedStreamingThreshold) {
        this.fileBackedStreamingThreshold = fileBackedStreamingThreshold;
    }

    public void setCustomRaftPolicyImplementationClass(final String customRaftPolicyImplementationClass) {
        this.customRaftPolicyImplementationClass = customRaftPolicyImplementationClass;
    }

    @Override
    public String getCustomRaftPolicyImplementationClass() {
        return customRaftPolicyImplementationClass;
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
    public int getRecoveredEntriesBeforeSnapshot() {
        return this.recoveredEntriesBeforeSnapshot;
    }

    @Override
    public FiniteDuration getHeartBeatInterval() {
        return heartBeatInterval;
    }

    @Override
    public FiniteDuration getElectionTimeOutInterval() {
        if (electionTimeOutInterval == null) {
            electionTimeOutInterval = getHeartBeatInterval().$times(electionTimeoutFactor);
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
    public int getSnapshotChunkSize() {
        return snapshotChunkSize;
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
        return policySupplier.get();
    }

    @Override
    public String getTempFileDirectory() {
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

    @SuppressWarnings("checkstyle:IllegalCatch")
    private RaftPolicy getPolicy() {
        if (Strings.isNullOrEmpty(DefaultConfigParamsImpl.this.customRaftPolicyImplementationClass)) {
            LOG.debug("No custom RaftPolicy specified. Using DefaultRaftPolicy");
            return DefaultRaftPolicy.INSTANCE;
        }

        try {
            String className = DefaultConfigParamsImpl.this.customRaftPolicyImplementationClass;
            LOG.info("Trying to use custom RaftPolicy {}", className);
            return (RaftPolicy)Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (ClassCastException | ReflectiveOperationException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("Could not create custom raft policy, will stick with default", e);
            } else {
                LOG.error("Could not create custom raft policy, will stick with default : cause = {}",
                    e.getMessage());
            }
        }
        return DefaultRaftPolicy.INSTANCE;
    }
}
