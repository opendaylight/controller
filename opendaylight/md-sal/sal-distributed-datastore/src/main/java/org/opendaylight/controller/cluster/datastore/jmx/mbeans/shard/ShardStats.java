/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.md.sal.common.util.jmx.QueuedNotificationManagerMXBeanImpl;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStats;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStatsMXBeanImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.yangtools.util.concurrent.ListenerNotificationQueueStats;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;

/**
 * Maintains statistics for a shard.
 *
 * @author  Basheeruddin syedbahm@cisco.com
 */
public class ShardStats extends AbstractMXBean implements ShardStatsMXBean {
    public static String JMX_CATEGORY_SHARD = "Shards";

    private final AtomicLong committedTransactionsCount = new AtomicLong();

    private final AtomicLong readOnlyTransactionCount = new AtomicLong();

    private final AtomicLong writeOnlyTransactionCount = new AtomicLong();

    private final AtomicLong readWriteTransactionCount = new AtomicLong();

    private String leader;

    private String raftState;

    private volatile long lastLogTerm = -1L;

    private volatile long lastLogIndex = -1L;

    private volatile long currentTerm = -1L;

    private volatile long commitIndex = -1L;

    private volatile long lastApplied = -1L;

    private volatile long lastCommittedTransactionTime;

    private final AtomicLong failedTransactionsCount = new AtomicLong();

    private final AtomicLong failedReadTransactionsCount = new AtomicLong();

    private final AtomicLong abortTransactionsCount = new AtomicLong();

    private ThreadExecutorStatsMXBeanImpl notificationExecutorStatsBean;

    private QueuedNotificationManagerMXBeanImpl notificationManagerStatsBean;

    private volatile long dataSize = 0;

    private final SimpleDateFormat sdf =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public ShardStats(final String shardName, final String mxBeanType) {
        super(shardName, mxBeanType, JMX_CATEGORY_SHARD);
    }

    public void setNotificationManager(final QueuedNotificationManager<?, ?> manager) {
        this.notificationManagerStatsBean = new QueuedNotificationManagerMXBeanImpl(manager,
                "notification-manager", getMBeanType(), getMBeanCategory());

        this.notificationExecutorStatsBean = ThreadExecutorStatsMXBeanImpl.create(manager.getExecutor());
    }

    @Override
    public String getShardName() {
        return getMBeanName();
    }

    @Override
    public long getCommittedTransactionsCount() {
        return committedTransactionsCount.get();
    }

    @Override
    public String getLeader() {
        return leader;
    }

    @Override
    public String getRaftState() {
        return raftState;
    }

    @Override
    public long getReadOnlyTransactionCount() {
        return readOnlyTransactionCount.get();
    }

    @Override
    public long getWriteOnlyTransactionCount() {
        return writeOnlyTransactionCount.get();
    }

    @Override
    public long getReadWriteTransactionCount() {
        return readWriteTransactionCount.get();
    }

    @Override
    public long getLastLogIndex() {
        return lastLogIndex;
    }

    @Override
    public long getLastLogTerm() {
        return lastLogTerm;
    }

    @Override
    public long getCurrentTerm() {
        return currentTerm;
    }

    @Override
    public long getCommitIndex() {
        return commitIndex;
    }

    @Override
    public long getLastApplied() {
        return lastApplied;
    }

    @Override
    public String getLastCommittedTransactionTime() {

        return sdf.format(new Date(lastCommittedTransactionTime));
    }

    @Override
    public long getFailedTransactionsCount() {
        return failedTransactionsCount.get();
    }

    @Override
    public long getFailedReadTransactionsCount() {
        return failedReadTransactionsCount.get();
    }

    @Override
    public long getAbortTransactionsCount() {
        return abortTransactionsCount.get();
    }

    public long incrementCommittedTransactionCount() {
        return committedTransactionsCount.incrementAndGet();
    }

    public long incrementReadOnlyTransactionCount() {
        return readOnlyTransactionCount.incrementAndGet();
    }

    public long incrementWriteOnlyTransactionCount() {
        return writeOnlyTransactionCount.incrementAndGet();
    }

    public long incrementReadWriteTransactionCount() {
        return readWriteTransactionCount.incrementAndGet();
    }

    public long incrementFailedTransactionsCount() {
        return failedTransactionsCount.incrementAndGet();
    }

    public long incrementFailedReadTransactionsCount() {
        return failedReadTransactionsCount.incrementAndGet();
    }

    public long incrementAbortTransactionsCount ()
    {
        return abortTransactionsCount.incrementAndGet();
    }

    public void setLeader(final String leader) {
        this.leader = leader;
    }

    public void setRaftState(final String raftState) {
        this.raftState = raftState;
    }

    public void setLastLogTerm(final long lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    public void setLastLogIndex(final long lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public void setCurrentTerm(final long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public void setCommitIndex(final long commitIndex) {
        this.commitIndex = commitIndex;
    }

    public void setLastApplied(final long lastApplied) {
        this.lastApplied = lastApplied;
    }

    public void setLastCommittedTransactionTime(final long lastCommittedTransactionTime) {
        this.lastCommittedTransactionTime = lastCommittedTransactionTime;
    }

    public void setDataSize(long dataSize){
        this.dataSize = dataSize;
    }

    @Override
    public long getDataSize(){
        return dataSize;
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
        committedTransactionsCount.set(0);

        readOnlyTransactionCount.set(0);

        writeOnlyTransactionCount.set(0);

        readWriteTransactionCount.set(0);

        lastCommittedTransactionTime = 0;

        failedTransactionsCount.set(0);

        failedReadTransactionsCount.set(0);

        abortTransactionsCount.set(0);

    }

    public void setDataStore(final InMemoryDOMDataStore store) {
        setNotificationManager(store.getDataChangeListenerNotificationManager());
    }
}
