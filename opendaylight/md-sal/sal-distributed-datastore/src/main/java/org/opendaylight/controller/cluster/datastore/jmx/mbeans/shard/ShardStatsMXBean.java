package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import java.util.List;
import org.opendaylight.controller.cluster.raft.client.messages.FollowerInfo;

/**
 * @author: syedbahm
 */
public interface ShardStatsMXBean {

   String getShardName();

   String getStatRetrievalTime();

   String getStatRetrievalError();

   long getCommittedTransactionsCount();

   long getReadOnlyTransactionCount();

   long getWriteOnlyTransactionCount();

   long getReadWriteTransactionCount();

   long getLastLogIndex();

   long getLastLogTerm();

   long getCurrentTerm();

   long getCommitIndex();

   long getLastApplied();

   long getLastIndex();

   long getLastTerm();

   long getSnapshotIndex();

   long getSnapshotTerm();

   long getReplicatedToAllIndex();

   String getLastCommittedTransactionTime();

   long getFailedTransactionsCount();

   long getAbortTransactionsCount();

   long getFailedReadTransactionsCount();

   String getLeader();

   String getRaftState();

   String getVotedFor();

   boolean isSnapshotCaptureInitiated();

   void resetTransactionCounters();

   long getInMemoryJournalDataSize();

   long getInMemoryJournalLogSize();

   boolean getFollowerInitialSyncStatus();

   List<FollowerInfo> getFollowerInfo();

   String getPeerAddresses();

   long getLeadershipChangeCount();

   String getLastLeadershipChangeTime();
}
