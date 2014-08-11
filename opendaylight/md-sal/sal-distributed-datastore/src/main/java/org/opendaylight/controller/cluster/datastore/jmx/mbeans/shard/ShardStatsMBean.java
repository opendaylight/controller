package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

/**
 * @author: syedbahm
 */
public interface ShardStatsMBean {
    String getShardName();

    Long getCommittedTransactionsCount();

    String getLeader();

    String getRaftState();

    Long getReadOnlyTransactionCount();

    Long getWriteOnlyTransactionCount();

    Long getReadWriteTransactionCount();

    Long getLastLogIndex();

    Long getLastLogTerm();

    Long getCurrentTerm();

    Long getCommitIndex();

    Long getLastApplied();

    String getLastCommittedTransactionTime();

    Long getFailedTransactionsCount();

}
