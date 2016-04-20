/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.raft.base.messages.InitiateCaptureSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.FollowerInfo;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import scala.concurrent.Await;

/**
 * Maintains statistics for a shard.
 *
 * @author  Basheeruddin syedbahm@cisco.com
 */
public class ShardStats extends AbstractMXBean implements ShardStatsMXBean {
    public static String JMX_CATEGORY_SHARD = "Shards";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Cache<String, OnDemandRaftState> onDemandRaftStateCache =
            CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS).build();

    private long committedTransactionsCount;

    private long readOnlyTransactionCount;

    private long writeOnlyTransactionCount;

    private long readWriteTransactionCount;

    private long lastCommittedTransactionTime;

    private long failedTransactionsCount;

    private final AtomicLong failedReadTransactionsCount = new AtomicLong();

    private long abortTransactionsCount;

    private boolean followerInitialSyncStatus = false;

    private Shard shard;

    private String statRetrievalError;

    private String statRetrievalTime;

    private long leadershipChangeCount;

    private long lastLeadershipChangeTime;

    public ShardStats(final String shardName, final String mxBeanType) {
        super(shardName, mxBeanType, JMX_CATEGORY_SHARD);
    }

    public void setShard(Shard shard) {
        this.shard = shard;
    }

    private OnDemandRaftState getOnDemandRaftState() {
        String name = getShardName();
        OnDemandRaftState state = onDemandRaftStateCache.getIfPresent(name);
        if(state == null) {
            statRetrievalError = null;
            statRetrievalTime = null;

            if(shard != null) {
                Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
                try {
                    Stopwatch timer = Stopwatch.createStarted();

                    state = (OnDemandRaftState) Await.result(Patterns.ask(shard.getSelf(),
                            GetOnDemandRaftState.INSTANCE, timeout), timeout.duration());

                    statRetrievalTime = timer.stop().toString();
                    onDemandRaftStateCache.put(name, state);
                } catch (Exception e) {
                    statRetrievalError = e.toString();
                }
            }

            state = state != null ? state : OnDemandRaftState.builder().build();
        }

        return state;
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
    public long getWriteOnlyTransactionCount() {
        return writeOnlyTransactionCount;
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
        return toStringMap(getOnDemandRaftState().getPeerVotingStates());
    }

    @Override
    public boolean isSnapshotCaptureInitiated() {
        return getOnDemandRaftState().isSnapshotCaptureInitiated();
    }

    @Override
    public String getLastCommittedTransactionTime() {
        return DATE_FORMAT.format(new Date(lastCommittedTransactionTime));
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

    public long incrementWriteOnlyTransactionCount() {
        return ++writeOnlyTransactionCount;
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

    public long incrementAbortTransactionsCount ()
    {
        return ++abortTransactionsCount;
    }

    public void setLastCommittedTransactionTime(final long lastCommittedTransactionTime) {
        this.lastCommittedTransactionTime = lastCommittedTransactionTime;
    }

    @Override
    public long getInMemoryJournalDataSize(){
        return getOnDemandRaftState().getInMemoryJournalDataSize();
    }

    @Override
    public long getInMemoryJournalLogSize() {
        return getOnDemandRaftState().getInMemoryJournalLogSize();
    }

    /**
     * resets the counters related to transactions
     */
    @Override
    public void resetTransactionCounters(){
        committedTransactionsCount = 0;

        readOnlyTransactionCount = 0;

        writeOnlyTransactionCount = 0;

        readWriteTransactionCount = 0;

        lastCommittedTransactionTime = 0;

        failedTransactionsCount = 0;

        failedReadTransactionsCount.set(0);

        abortTransactionsCount = 0;

    }

    public void setFollowerInitialSyncStatus(boolean followerInitialSyncStatus) {
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
        return toStringMap(getOnDemandRaftState().getPeerAddresses());
    }

    private static String toStringMap(Map<?, ?> map) {
        return Joiner.on(", ").withKeyValueSeparator(": ").join(map);
    }

    @Override
    public String getStatRetrievalTime() {
        getOnDemandRaftState();
        return statRetrievalTime;
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
        return DATE_FORMAT.format(new Date(lastLeadershipChangeTime));
    }

    @Override
    public int getPendingTxCommitQueueSize() {
        return shard.getPendingTxCommitQueueSize();
    }

    @Override
    public int getTxCohortCacheSize() {
        return shard.getCohortCacheSize();
    }

    @Override
    public void captureSnapshot() {
        if(shard != null) {
            shard.getSelf().tell(new InitiateCaptureSnapshot(), ActorRef.noSender());
        }
    }
}
