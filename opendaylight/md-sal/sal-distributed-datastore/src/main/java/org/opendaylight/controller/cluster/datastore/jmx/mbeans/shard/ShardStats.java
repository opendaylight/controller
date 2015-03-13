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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.cluster.raft.client.messages.FollowerInfo;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.md.sal.common.util.jmx.QueuedNotificationManagerMXBeanImpl;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStats;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStatsMXBeanImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.yangtools.util.concurrent.ListenerNotificationQueueStats;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;

/**
 * Maintains statistics for a shard.
 *
 * @author  Basheeruddin syedbahm@cisco.com
 */
public class ShardStats extends AbstractMXBean implements ShardStatsMXBean {
    public static String JMX_CATEGORY_SHARD = "Shards";

    private static final Logger LOG = LoggerFactory.getLogger(ShardStats.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Cache<String, OnDemandRaftState> onDemandRaftStatsCache =
            CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS).build();

    private long committedTransactionsCount;

    private long readOnlyTransactionCount;

    private long writeOnlyTransactionCount;

    private long readWriteTransactionCount;

    private long lastCommittedTransactionTime;

    private long failedTransactionsCount;

    private final AtomicLong failedReadTransactionsCount = new AtomicLong();

    private long abortTransactionsCount;

    private ThreadExecutorStatsMXBeanImpl notificationExecutorStatsBean;

    private QueuedNotificationManagerMXBeanImpl notificationManagerStatsBean;

    private boolean followerInitialSyncStatus = false;

    private ActorRef shardActor;

    public ShardStats(final String shardName, final String mxBeanType) {
        super(shardName, mxBeanType, JMX_CATEGORY_SHARD);
    }

    public void setNotificationManager(final QueuedNotificationManager<?, ?> manager) {
        this.notificationManagerStatsBean = new QueuedNotificationManagerMXBeanImpl(manager,
                "notification-manager", getMBeanType(), getMBeanCategory());

        this.notificationExecutorStatsBean = ThreadExecutorStatsMXBeanImpl.create(manager.getExecutor());
    }

    public void setShardActor(ActorRef shardActor) {
        this.shardActor = shardActor;
    }

    private OnDemandRaftState getOnDemandRaftStats() {
        String name = getShardName();
        OnDemandRaftState stats = onDemandRaftStatsCache.getIfPresent(name);
        if(stats == null) {
            if(shardActor != null) {
                Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
                try {
                    stats = (OnDemandRaftState) Await.result(Patterns.ask(shardActor,
                            GetOnDemandRaftState.INSTANCE, timeout), timeout.duration());
                    onDemandRaftStatsCache.put(name, stats);
                } catch (Exception e) {
                    LOG.warn("Could not retrieve follower info: {}", e);
                }
            }

            stats = stats != null ? stats : OnDemandRaftState.builder().build();
        }

        return stats;
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
        return getOnDemandRaftStats().getLeader();
    }

    @Override
    public String getRaftState() {
        return getOnDemandRaftStats().getRaftState();
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
        return getOnDemandRaftStats().getLastLogIndex();
    }

    @Override
    public long getLastLogTerm() {
        return getOnDemandRaftStats().getLastLogTerm();
    }

    @Override
    public long getCurrentTerm() {
        return getOnDemandRaftStats().getCurrentTerm();
    }

    @Override
    public long getCommitIndex() {
        return getOnDemandRaftStats().getCommitIndex();
    }

    @Override
    public long getLastApplied() {
        return getOnDemandRaftStats().getLastApplied();
    }

    @Override
    public long getLastIndex() {
        return getOnDemandRaftStats().getLastIndex();
    }

    @Override
    public long getLastTerm() {
        return getOnDemandRaftStats().getLastTerm();
    }

    @Override
    public long getSnapshotIndex() {
        return getOnDemandRaftStats().getSnapshotIndex();
    }

    @Override
    public long getSnapshotTerm() {
        return getOnDemandRaftStats().getSnapshotTerm();
    }

    @Override
    public long getReplicatedToAllIndex() {
        return getOnDemandRaftStats().getReplicatedToAllIndex();
    }

    @Override
    public String getVotedFor() {
        // TODO Auto-generated method stub
        return getOnDemandRaftStats().getVotedFor();
    }

    @Override
    public boolean isSnapshotCaptureInitiated() {
        return getOnDemandRaftStats().isSnapshotCaptureInitiated();
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
        return getOnDemandRaftStats().getInMemoryJournalDataSize();
    }

    @Override
    public long getInMemoryJournalLogSize() {
        return getOnDemandRaftStats().getInMemoryJournalLogSize();
    }

    @Override
    public ThreadExecutorStats getDataStoreExecutorStats() {
        // FIXME: this particular thing does not work, as it really is DS-specific
        return null;
    }

    @Override
    public ThreadExecutorStats getNotificationMgrExecutorStats() {
        return notificationExecutorStatsBean.toThreadExecutorStats();
    }

    @Override
    public List<ListenerNotificationQueueStats> getCurrentNotificationMgrListenerQueueStats() {
        return notificationManagerStatsBean.getCurrentListenerQueueStats();
    }

    @Override
    public int getMaxNotificationMgrListenerQueueSize() {
        return notificationManagerStatsBean.getMaxListenerQueueSize();
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

    public void setDataStore(final InMemoryDOMDataStore store) {
        setNotificationManager(store.getDataChangeListenerNotificationManager());
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
        return getOnDemandRaftStats().getFollowerInfoList();
    }

    @Override
    public String getPeerAddresses() {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for(Map.Entry<String, String> e: getOnDemandRaftStats().getPeerAddresses().entrySet()) {
            if(i++ > 0) {
                builder.append(", ");
            }

            builder.append(e.getKey()).append(": ").append(e.getValue());
        }

        return builder.toString();
    }
}
