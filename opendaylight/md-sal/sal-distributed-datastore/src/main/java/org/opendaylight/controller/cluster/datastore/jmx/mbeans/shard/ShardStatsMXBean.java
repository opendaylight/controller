package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import java.util.List;

import org.opendaylight.yangtools.util.jmx.ListenerNotificationQueueStats;
import org.opendaylight.yangtools.util.jmx.ThreadExecutorStats;

/**
 * @author: syedbahm
 */
public interface ShardStatsMXBean {

   String getShardName();

   long getCommittedTransactionsCount();

   long getReadOnlyTransactionCount();

   long getWriteOnlyTransactionCount();

   long getReadWriteTransactionCount();

   long getLastLogIndex();

   long getLastLogTerm();

   long getCurrentTerm();

   long getCommitIndex();

   long getLastApplied();

   String getLastCommittedTransactionTime();

   long getFailedTransactionsCount();

   String getLeader();

   String getRaftState();

   ThreadExecutorStats getDataStoreExecutorStats();

   ThreadExecutorStats getNotificationMgrExecutorStats();

   List<ListenerNotificationQueueStats> getCurrentNotificationMgrListenerQueueStats();

   int getMaxNotificationMgrListenerQueueSize();
}
