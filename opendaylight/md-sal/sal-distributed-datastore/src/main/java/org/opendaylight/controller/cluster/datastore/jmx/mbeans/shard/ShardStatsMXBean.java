package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import org.opendaylight.controller.sal.core.api.jmx.ThreadExecutorStats;

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
