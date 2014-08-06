package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.sal.core.spi.data.statistics.DOMStoreStatsTracker;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.opendaylight.yangtools.util.jmx.AbstractMXBean;
import org.opendaylight.yangtools.util.jmx.ListenerNotificationQueueStats;
import org.opendaylight.yangtools.util.jmx.QueuedNotificationManagerMXBeanImpl;
import org.opendaylight.yangtools.util.jmx.ThreadExecutorStats;
import org.opendaylight.yangtools.util.jmx.ThreadExecutorStatsMXBeanImpl;

/**
 * Maintains statistics for a shard.
 *
 * @author syedbahm
 */
public class ShardStats extends AbstractMXBean implements ShardStatsMXBean, DOMStoreStatsTracker {
    public static String JMX_CATEGORY_SHARD = "Shards";

    private final AtomicLong committedTransactionsCount = new AtomicLong();
    private final AtomicLong journalMessagesCount =new AtomicLong();
    private String leader;
    private String raftState;
    private ThreadExecutorStatsMXBeanImpl notificationExecutorStatsBean;
    private ThreadExecutorStatsMXBeanImpl dataStoreExecutorStatsBean;
    private QueuedNotificationManagerMXBeanImpl notificationManagerStatsBean;

    public ShardStats(String shardName, String mxBeanType) {
        super(shardName, mxBeanType, JMX_CATEGORY_SHARD);
    }

    @Override
    public void setDataChangeListenerExecutor(ExecutorService dclExecutor) {
        this.notificationExecutorStatsBean = new ThreadExecutorStatsMXBeanImpl(dclExecutor,
                "data-store-executor", getMBeanType(), getMBeanCategory());
    }

    @Override
    public void setDataStoreExecutor(ExecutorService dsExecutor) {
        this.dataStoreExecutorStatsBean = new ThreadExecutorStatsMXBeanImpl(dsExecutor,
                "notification-executor", getMBeanType(), getMBeanCategory());
    }

    @Override
    public void setNotificationManager(QueuedNotificationManager<?, ?> manager) {
        this.notificationManagerStatsBean = new QueuedNotificationManagerMXBeanImpl(manager,
                "notification-manager", getMBeanType(), getMBeanCategory());
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
    public long getJournalMessagesCount() {
        // FIXME: this will be populated once after integration with Raft stuff
        return journalMessagesCount.get();
    }

    @Override
    public String getLeader() {
        return leader;
    }

    @Override
    public String getRaftState() {
        return raftState;
    }

    public long incrementCommittedTransactionCount() {
        return committedTransactionsCount.incrementAndGet();
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public void setRaftState(String raftState) {
        this.raftState = raftState;
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
