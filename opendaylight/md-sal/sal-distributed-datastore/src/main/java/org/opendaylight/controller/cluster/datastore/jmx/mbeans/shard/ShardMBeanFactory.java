package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Factory for creating shard MXBean instances.
 *
 * @author: syedbahm
 * Date: 7/16/14
 */
public class ShardMBeanFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ShardMBeanFactory.class);

    private static Cache<String,ShardStats> shardMBeansCache =
                                      CacheBuilder.newBuilder().weakValues().build();

    public static ShardStats getShardStatsMBean(final String shardName, final String mxBeanType) {
        final String finalMXBeanType = mxBeanType != null ? mxBeanType : "DistDataStore";
        try {
            return shardMBeansCache.get(shardName, new Callable<ShardStats>() {
                @Override
                public ShardStats call() throws Exception {
                    ShardStats shardStatsMBeanImpl = new ShardStats(shardName, finalMXBeanType);
                    shardStatsMBeanImpl.registerMBean();
                    return shardStatsMBeanImpl;
                }
            });
        } catch(ExecutionException e) {
            LOG.error(String.format("Could not create MXBean for shard: %s", shardName), e);
            // Just return an instance that isn't registered.
            return new ShardStats(shardName, finalMXBeanType);
        }
    }
}
