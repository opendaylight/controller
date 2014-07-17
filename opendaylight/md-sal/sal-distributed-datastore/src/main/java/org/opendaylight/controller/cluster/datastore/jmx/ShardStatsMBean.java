package org.opendaylight.controller.cluster.datastore.jmx;

/**
 * @author: syedbahm
 */
public interface ShardStatsMBean {
   String getShardName();
   Long getCommittedTransactionsCount();
   Long getJournalMessagesCount();
   Long incrementCommittedTransactionCount();

  void updateCommittedTransactionsCount(long count);
  void updateJournalMessagesCount(long count);
  void updateMailBoxSize(int mailboxSize);
}
