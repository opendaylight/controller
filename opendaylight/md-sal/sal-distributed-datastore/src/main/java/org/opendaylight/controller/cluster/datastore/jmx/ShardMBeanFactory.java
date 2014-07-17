package org.opendaylight.controller.cluster.datastore.jmx;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: syedbahm
 * Date: 7/16/14
 */
public class ShardMBeanFactory {
  private static Map<String,ShardStatsMBean> shardMBeans= new HashMap<String,ShardStatsMBean>();

  public static ShardStatsMBean getShardStatsMBean(String shardName){
       if(shardMBeans.containsKey(shardName)){
            return shardMBeans.get(shardName);
       }else {
         ShardStats shardStatsMBeanImpl = new ShardStats(shardName);
         shardStatsMBeanImpl.updateCommittedTransactionsCount(0);
         shardStatsMBeanImpl.updateJournalMessagesCount(0);
         shardStatsMBeanImpl.updateMailBoxSize(0);
         if(shardStatsMBeanImpl.registerMBean()) {
           shardMBeans.put(shardName, shardStatsMBeanImpl);
         }
         return shardStatsMBeanImpl;
       }
  }

}
