package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import org.opendaylight.controller.cluster.datastore.jmx.mbeans.AbstractBaseMBean;

/**
 * @author: syedbahm
 */
public class ShardStats extends AbstractBaseMBean implements ShardStatsMBean {

    private final String shardName;

    private Long committedTransactionsCount = 0L;

    private Long readOnlyTransactionCount = 0L;

    private Long writeOnlyTransactionCount = 0L;

    private Long readWriteTransactionCount = 0L;

    private String leader;

    private String raftState;

    private Long lastLogTerm = -1L;

    private Long lastLogIndex = -1L;

    private Long currentTerm = -1L;

    private Long commitIndex = -1L;

    private Long lastApplied = -1L;

    ShardStats(String shardName) {
        this.shardName = shardName;
    }


    @Override
    public String getShardName() {
        return shardName;
    }

    @Override
    public Long getCommittedTransactionsCount() {
        return committedTransactionsCount;
    }

    @Override public String getLeader() {
        return leader;
    }

    @Override public String getRaftState() {
        return raftState;
    }

    @Override public Long getReadOnlyTransactionCount() {
        return readOnlyTransactionCount;
    }

    @Override public Long getWriteOnlyTransactionCount() {
        return writeOnlyTransactionCount;
    }

    @Override public Long getReadWriteTransactionCount() {
        return readWriteTransactionCount;
    }

    @Override public Long getLastLogIndex() {
        return lastLogIndex;
    }

    @Override public Long getLastLogTerm() {
        return lastLogTerm;
    }

    @Override public Long getCurrentTerm() {
        return currentTerm;
    }

    @Override public Long getCommitIndex() {
        return commitIndex;
    }

    @Override public Long getLastApplied() {
        return lastApplied;
    }

    public Long incrementCommittedTransactionCount() {
        return committedTransactionsCount++;
    }

    public Long incrementReadOnlyTransactionCount() {
        return readOnlyTransactionCount++;
    }

    public Long incrementWriteOnlyTransactionCount() {
        return writeOnlyTransactionCount++;
    }

    public Long incrementReadWriteTransactionCount() {
        return readWriteTransactionCount++;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public void setRaftState(String raftState) {
        this.raftState = raftState;
    }

    public void setLastLogTerm(Long lastLogTerm) {
        this.lastLogTerm = lastLogTerm;
    }

    public void setLastLogIndex(Long lastLogIndex) {
        this.lastLogIndex = lastLogIndex;
    }

    public void setCurrentTerm(Long currentTerm) {
        this.currentTerm = currentTerm;
    }

    public void setCommitIndex(Long commitIndex) {
        this.commitIndex = commitIndex;
    }

    public void setLastApplied(Long lastApplied) {
        this.lastApplied = lastApplied;
    }

    @Override
    protected String getMBeanName() {
        return shardName;
    }

    @Override
    protected String getMBeanType() {
        return JMX_TYPE_DISTRIBUTED_DATASTORE;
    }

    @Override
    protected String getMBeanCategory() {
        return JMX_CATEGORY_SHARD;
    }


}
