package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.opendaylight.yangtools.util.jmx.AbstractMXBean;
import org.opendaylight.yangtools.util.jmx.ListenerNotificationQueueStats;
import org.opendaylight.yangtools.util.jmx.QueuedNotificationManagerMXBeanImpl;
import org.opendaylight.yangtools.util.jmx.ThreadExecutorStats;
import org.opendaylight.yangtools.util.jmx.ThreadExecutorStatsMXBeanImpl;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Maintains statistics for a shard.
 *
 * @author syedbahm
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

    private ThreadExecutorStatsMXBeanImpl notificationExecutorStatsBean;

    private ThreadExecutorStatsMXBeanImpl dataStoreExecutorStatsBean;

    private QueuedNotificationManagerMXBeanImpl notificationManagerStatsBean;

    private final SimpleDateFormat sdf =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public ShardStats(String shardName, String mxBeanType) {
        super(shardName, mxBeanType, JMX_CATEGORY_SHARD);
    }

    public void setDataStoreExecutor(ExecutorService dsExecutor) {
        this.dataStoreExecutorStatsBean = new ThreadExecutorStatsMXBeanImpl(dsExecutor,
                "notification-executor", getMBeanType(), getMBeanCategory());
    }

    public void setNotificationManager(QueuedNotificationManager<?, ?> manager) {
        this.notificationManagerStatsBean = new QueuedNotificationManagerMXBeanImpl(manager,
                "notification-manager", getMBeanType(), getMBeanCategory());

        this.notificationExecutorStatsBean = new ThreadExecutorStatsMXBeanImpl(manager.getExecutor(),
                "data-store-executor", getMBeanType(), getMBeanCategory());
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

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public void setRaftState(String raftState) {
        this.raftState = raftState;
    }

    public void setLastLogTerm(long lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    public void setLastLogIndex(long lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public void setCurrentTerm(long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public void setCommitIndex(long commitIndex) {
        this.commitIndex = commitIndex;
    }

    public void setLastApplied(long lastApplied) {
        this.lastApplied = lastApplied;
    }


    public void setLastCommittedTransactionTime(long lastCommittedTransactionTime) {
        this.lastCommittedTransactionTime = lastCommittedTransactionTime;
    }

    public void incrementFailedTransactionsCount() {
        this.failedTransactionsCount.incrementAndGet();
    }

    @Override
    public ThreadExecutorStats getDataStoreExecutorStats() {
        return dataStoreExecutorStatsBean.toThreadExecutorStats();
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
}
