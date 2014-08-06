package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import org.opendaylight.yangtools.util.jmx.ThreadExecutorStats;

/**
 * @author: syedbahm
 */
public interface ShardStatsMXBean {
   String getShardName();
   long getCommittedTransactionsCount();
   long getJournalMessagesCount();
   String getLeader();
   String getRaftState();
   ThreadExecutorStats getDataStoreExecutorStats();
   ThreadExecutorStats getNotificationExecutorStats();
}
