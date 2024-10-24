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
import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStatsMXBean;
import org.opendaylight.controller.cluster.mgmt.api.FollowerInfo;
import org.opendaylight.controller.cluster.raft.base.messages.InitiateCaptureSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;

/**
 * Maintains statistics for a shard.
 *
 * @author  Basheeruddin syedbahm@cisco.com
 */
final class ShardStats extends AbstractMXBean implements ShardStatsMXBean {
    public static final String JMX_CATEGORY_SHARD = "Shards";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault());

    private static final MapJoiner MAP_JOINER = Joiner.on(", ").withKeyValueSeparator(": ");

    private final Shard shard;

    private final OnDemandShardStateCache stateCache;

    private long committedTransactionsCount;

    private long readOnlyTransactionCount;

    private long readWriteTransactionCount;

    private long lastCommittedTransactionTime;

    private long failedTransactionsCount;

    private final AtomicLong failedReadTransactionsCount = new AtomicLong();

    private long abortTransactionsCount;

    private boolean followerInitialSyncStatus = false;

    private String statRetrievalError;

    private long leadershipChangeCount;

    private long lastLeadershipChangeTime;

    ShardStats(final String shardName, final String mxBeanType, final @Nullable Shard shard) {
        super(shardName, mxBeanType, JMX_CATEGORY_SHARD);
        this.shard = shard;
        stateCache = new OnDemandShardStateCache(shardName, shard != null ? shard.self() : null);
    }

    static ShardStats create(final String shardName, final String mxBeanType, final @NonNull Shard shard) {
        String finalMXBeanType = mxBeanType != null ? mxBeanType : "DistDataStore";
        ShardStats shardStatsMBeanImpl = new ShardStats(shardName, finalMXBeanType, shard);
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
            return OnDemandRaftState.builder().build();
        }
    }

    private static String formatMillis(final long timeMillis) {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timeMillis));
    }

    @Override
    public String getShardName() {
        return getMBeanName();
    }

    @Override
    public long getCommittedTransactionsCount() {
        return committedTransactionsCount;
    }

    @Override
    public String getLeader() {
        return getOnDemandRaftState().getLeader();
    }

    @Override
    public String getRaftState() {
        return getOnDemandRaftState().getRaftState();
    }

    @Override
    public long getReadOnlyTransactionCount() {
        return readOnlyTransactionCount;
    }

    @Override
    public long getReadWriteTransactionCount() {
        return readWriteTransactionCount;
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
    public String getLastCommittedTransactionTime() {
        return formatMillis(lastCommittedTransactionTime);
    }

    @Override
    public long getFailedTransactionsCount() {
        return failedTransactionsCount;
    }

    @Override
    public long getFailedReadTransactionsCount() {
        return failedReadTransactionsCount.get();
    }

    @Override
    public long getAbortTransactionsCount() {
        return abortTransactionsCount;
    }

    public long incrementCommittedTransactionCount() {
        return ++committedTransactionsCount;
    }

    public long incrementReadOnlyTransactionCount() {
        return ++readOnlyTransactionCount;
    }

    public long incrementReadWriteTransactionCount() {
        return ++readWriteTransactionCount;
    }

    public long incrementFailedTransactionsCount() {
        return ++failedTransactionsCount;
    }

    public long incrementFailedReadTransactionsCount() {
        return failedReadTransactionsCount.incrementAndGet();
    }

    public long incrementAbortTransactionsCount() {
        return ++abortTransactionsCount;
    }

    public void setLastCommittedTransactionTime(final long lastCommittedTransactionTime) {
        this.lastCommittedTransactionTime = lastCommittedTransactionTime;
    }

    @Override
    public long getInMemoryJournalDataSize() {
        return getOnDemandRaftState().getInMemoryJournalDataSize();
    }

    @Override
    public long getInMemoryJournalLogSize() {
        return getOnDemandRaftState().getInMemoryJournalLogSize();
    }

    /**
     * Resets the counters related to transactions.
     */
    @Override
    public void resetTransactionCounters() {
        committedTransactionsCount = 0;

        readOnlyTransactionCount = 0;

        readWriteTransactionCount = 0;

        lastCommittedTransactionTime = 0;

        failedTransactionsCount = 0;

        failedReadTransactionsCount.set(0);

        abortTransactionsCount = 0;

    }

    public void setFollowerInitialSyncStatus(final boolean followerInitialSyncStatus) {
        this.followerInitialSyncStatus = followerInitialSyncStatus;
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
        return stateCache.getStatRetrievaelTime();
    }

    @Override
    public String getStatRetrievalError() {
        getOnDemandRaftState();
        return statRetrievalError;
    }

    @Override
    public long getLeadershipChangeCount() {
        return leadershipChangeCount;
    }

    public void incrementLeadershipChangeCount() {
        leadershipChangeCount++;
        lastLeadershipChangeTime = System.currentTimeMillis();
    }

    @Override
    public String getLastLeadershipChangeTime() {
        return formatMillis(lastLeadershipChangeTime);
    }

    @Override
    public int getPendingTxCommitQueueSize() {
        return shard != null ? shard.getPendingTxCommitQueueSize() : -1;
    }

    @Override
    public int getTxCohortCacheSize() {
        return shard != null ? shard.getCohortCacheSize() : -1;
    }

    @Override
    public void captureSnapshot() {
        if (shard != null) {
            shard.getSelf().tell(new InitiateCaptureSnapshot(), ActorRef.noSender());
        }
    }
}
