package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import org.opendaylight.controller.cluster.datastore.jmx.mbeans.AbstractBaseMBean;

/**
 * @author: syedbahm
 */
public class ShardStats extends AbstractBaseMBean implements ShardStatsMBean {
  private  Long committedTransactionsCount;
  private Long journalMessagesCount;
  final private String shardName;
  private String leader;
  private String raftState;

  ShardStats(String shardName){
    this.shardName = shardName;
    committedTransactionsCount =0L;
    journalMessagesCount = 0L;
  };


  @Override
  public String getShardName() {
    return shardName;
  }

  @Override
  public Long getCommittedTransactionsCount() {
    return committedTransactionsCount;
  }

  @Override
  public Long getJournalMessagesCount() {
    //FIXME: this will be populated once after integration with Raft stuff
    return journalMessagesCount;
  }

  @Override public String getLeader() {
    return leader;
  }

  @Override public String getRaftState() {
    return raftState;
  }

  public Long incrementCommittedTransactionCount() {
    return committedTransactionsCount++;
  }


  public void updateCommittedTransactionsCount(long currentCount){
     committedTransactionsCount = currentCount;

  }

  public void updateJournalMessagesCount(long currentCount){
    journalMessagesCount  = currentCount;

  }

  public void setLeader(String leader){
    this.leader = leader;
  }

  public void setRaftState(String raftState){
    this.raftState = raftState;
  }


  @Override
  protected String getMBeanName() {
    return  shardName;
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
