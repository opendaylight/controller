package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

/**
 * @author: syedbahm
 */
public interface ShardStatsMBean {
   String getShardName();
   Long getCommittedTransactionsCount();
   Long getJournalMessagesCount();

}
