package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: syedbahm
 * Date: 7/16/14
 */
public class ShardMBeanFactory {
    private static Map<String, ShardStats> shardMBeans =
        new HashMap<String, ShardStats>();

    public static ShardStats getShardStatsMBean(String shardName) {
        if (shardMBeans.containsKey(shardName)) {
            return shardMBeans.get(shardName);
        } else {
            ShardStats shardStatsMBeanImpl = new ShardStats(shardName);

            if (shardStatsMBeanImpl.registerMBean()) {
                shardMBeans.put(shardName, shardStatsMBeanImpl);
            }
            return shardStatsMBeanImpl;
        }
    }

}
