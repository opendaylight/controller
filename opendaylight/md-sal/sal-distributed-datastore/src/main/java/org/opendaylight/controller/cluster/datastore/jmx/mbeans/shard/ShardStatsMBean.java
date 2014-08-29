package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

/**
 * @author: syedbahm
 */
public interface ShardStatsMBean {
    String getShardName();

    long getCommittedTransactionsCount();

    String getLeader();

    String getRaftState();

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

    long getFailedReadTransactionsCount();

    long getAbortTransactionsCount();

    void resetTransactionCounters();

}
