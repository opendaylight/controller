/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStatsMXBean;
import org.opendaylight.controller.cluster.mgmt.api.FollowerInfo;
import org.opendaylight.controller.cluster.raft.base.messages.InitiateCaptureSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.raft.api.RaftRole;

/**
 * Maintains statistics for a shard.
 *
 * @author  Basheeruddin syedbahm@cisco.com
 */
final class DefaultShardStatsMXBean extends AbstractMXBean implements ShardStatsMXBean {
    public static final String JMX_CATEGORY_SHARD = "Shards";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault());
    private static final MapJoiner MAP_JOINER = Joiner.on(", ").withKeyValueSeparator(": ");

    private final AtomicReference<ShardStats> shardStats = new AtomicReference<>(new ShardStats());
    private final AtomicLong leadershipChangeCount = new AtomicLong();
    private final OnDemandShardStateCache stateCache;
    private final Shard shard;

    private volatile boolean followerInitialSyncStatus = false;
    private volatile String statRetrievalError;
    private volatile long lastLeadershipChangeTime;

    DefaultShardStatsMXBean(final String shardName, final String mxBeanType, final @Nullable Shard shard) {
        super(shardName, mxBeanType, JMX_CATEGORY_SHARD);
        this.shard = shard;
        stateCache = new OnDemandShardStateCache(shardName, shard != null ? shard.self() : null);
    }

    static DefaultShardStatsMXBean create(final String shardName, final String mxBeanType, final @NonNull Shard shard) {
        final var finalMXBeanType = mxBeanType != null ? mxBeanType : "DistDataStore";
        final var shardStatsMBeanImpl = new DefaultShardStatsMXBean(shardName, finalMXBeanType, shard);
        shardStatsMBeanImpl.registerMBean();
        return shardStatsMBeanImpl;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private OnDemandRaftState getOnDemandRaftState() {
        try {
            final OnDemandRaftState state = stateCache.get();
            statRetrievalError = null;
            return state;
        } catch (Exception e) {
            statRetrievalError = e.getCause().toString();
            return new OnDemandRaftState.Builder().build();
        }
    }

    @Override
    public String getShardName() {
        return getMBeanName();
    }

    @Override
    public String getLeader() {
        return getOnDemandRaftState().getLeader();
    }

    @Override
    public RaftRole getRaftState() {
        return getOnDemandRaftState().getRaftState();
    }

    @Override
    public long getLastLogIndex() {
        return getOnDemandRaftState().getLastLogIndex();
    }

    @Override
    public long getLastLogTerm() {
        return getOnDemandRaftState().getLastLogTerm();
    }

    @Override
    public long getCurrentTerm() {
        return getOnDemandRaftState().getCurrentTerm();
    }

    @Override
    public long getCommitIndex() {
        return getOnDemandRaftState().getCommitIndex();
    }

    @Override
    public long getLastApplied() {
        return getOnDemandRaftState().getLastApplied();
    }

    @Override
    public long getLastIndex() {
        return getOnDemandRaftState().getLastIndex();
    }

    @Override
    public long getLastTerm() {
        return getOnDemandRaftState().getLastTerm();
    }

    @Override
    public long getSnapshotIndex() {
        return getOnDemandRaftState().getSnapshotIndex();
    }

    @Override
    public long getSnapshotTerm() {
        return getOnDemandRaftState().getSnapshotTerm();
    }

    @Override
    public long getReplicatedToAllIndex() {
        return getOnDemandRaftState().getReplicatedToAllIndex();
    }

    @Override
    public String getVotedFor() {
        return getOnDemandRaftState().getVotedFor();
    }

    @Override
    public boolean isVoting() {
        return getOnDemandRaftState().isVoting();
    }

    @Override
    public String getPeerVotingStates() {
        return MAP_JOINER.join(getOnDemandRaftState().getPeerVotingStates());
    }

    @Override
    public boolean isSnapshotCaptureInitiated() {
        return getOnDemandRaftState().isSnapshotCaptureInitiated();
    }

    @Override
    public long getInMemoryJournalDataSize() {
        return getOnDemandRaftState().getInMemoryJournalDataSize();
    }

    @Override
    public long getInMemoryJournalLogSize() {
        return getOnDemandRaftState().getInMemoryJournalLogSize();
    }

    @Override
    public String getLastCommittedTransactionTime() {
        return DATE_FORMATTER.format(shardStats().lastCommittedTransactionTime());
    }

    @Override
    public long getCommittedTransactionsCount() {
        return shardStats().committedTransactionsCount();
    }

    @Override
    public long getReadOnlyTransactionCount() {
        return shardStats().readOnlyTransactionCount();
    }

    @Override
    public long getReadWriteTransactionCount() {
        return shardStats().readWriteTransactionCount();
    }

    @Override
    public long getFailedTransactionsCount() {
        return shardStats().failedTransactionsCount();
    }

    @Override
    public long getFailedReadTransactionsCount() {
        return shardStats().failedReadTransactionsCount();
    }

    @Override
    public long getAbortTransactionsCount() {
        return shardStats().abortTransactionsCount();
    }

    /**
     * Resets the counters related to transactions.
     */
    @Override
    public void resetTransactionCounters() {
        shardStats.setRelease(new ShardStats());
    }

    @Override
    public boolean getFollowerInitialSyncStatus() {
        return followerInitialSyncStatus;
    }

    @Override
    public List<FollowerInfo> getFollowerInfo() {
        return getOnDemandRaftState().getFollowerInfoList();
    }

    @Override
    public String getPeerAddresses() {
        return MAP_JOINER.join(getOnDemandRaftState().getPeerAddresses());
    }

    @Override
    public String getStatRetrievalTime() {
        getOnDemandRaftState();
        return stateCache.stateRetrievalTime();
    }

    @Override
    public String getStatRetrievalError() {
        getOnDemandRaftState();
        return statRetrievalError;
    }

    @Override
    public long getLeadershipChangeCount() {
        return leadershipChangeCount.get();
    }

    public void incrementLeadershipChangeCount() {
        leadershipChangeCount.getAndIncrement();
        lastLeadershipChangeTime = System.currentTimeMillis();
    }

    @Override
    public String getLastLeadershipChangeTime() {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(lastLeadershipChangeTime));
    }

    @Override
    public int getPendingTxCommitQueueSize() {
        return shard != null ? shard.getPendingTxCommitQueueSize() : -1;
    }

    @Override
    public int getTxCohortCacheSize() {
        // FIXME: deprecate this?
        return shard != null ? 0 : -1;
    }

    @Override
    public void captureSnapshot() {
        if (shard != null) {
            shard.self().tell(new InitiateCaptureSnapshot(), ActorRef.noSender());
        }
    }

    void setFollowerInitialSyncStatus(final boolean followerInitialSyncStatus) {
        this.followerInitialSyncStatus = followerInitialSyncStatus;
    }

    ShardStats shardStats() {
        return shardStats.getAcquire();
    }
}
