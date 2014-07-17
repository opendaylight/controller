package org.opendaylight.controller.cluster.datastore.jmx;

/**
 * @author: syedbahm
 */
public class ShardStats extends AbstractBaseMBean implements ShardStatsMBean {
  private  Long committedTransactionsCount;
  private Long journalMessagesCount;
  final private String shardName;
  private Integer mailboxSize;

  ShardStats(String shardName){this.shardName = shardName;};


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
    return journalMessagesCount;
  }

  @Override
  public Long incrementCommittedTransactionCount() {
    return committedTransactionsCount++;
  }


  public int getMailboxSize() {
    return mailboxSize;
  }

  public String getMemberInfo() {
    return "Member Info yet to be implemented";
  }

  public void updateCommittedTransactionsCount(long currentCount){
     committedTransactionsCount = currentCount;

  }

  public void updateJournalMessagesCount(long currentCount){
    journalMessagesCount  = currentCount;

  }

  public void updateMailBoxSize(int mailboxSize){
    this.mailboxSize = mailboxSize;

  }


  @Override
  protected String getMBeanName() {
    return shardName + ":type=ShardStatsMBean";
  }


}
