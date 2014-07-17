package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import org.opendaylight.controller.cluster.datastore.jmx.mbeans.AbstractBaseMBean;

/**
 * @author: syedbahm
 */
public class ShardStats extends AbstractBaseMBean implements ShardStatsMBean {
  private  Long committedTransactionsCount;
  private Long journalMessagesCount;
  final private String shardName;

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


  public Long incrementCommittedTransactionCount() {
    return committedTransactionsCount++;
  }


  public void updateCommittedTransactionsCount(long currentCount){
     committedTransactionsCount = currentCount;

  }

  public void updateJournalMessagesCount(long currentCount){
    journalMessagesCount  = currentCount;

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
