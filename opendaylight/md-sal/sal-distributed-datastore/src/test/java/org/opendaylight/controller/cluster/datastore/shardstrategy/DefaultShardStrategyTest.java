package org.opendaylight.controller.cluster.datastore.shardstrategy;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;

public class DefaultShardStrategyTest {
    @Test
    public void testFindShard() throws Exception {
        String shard = DefaultShardStrategy.getInstance().findShard(TestModel.TEST_PATH);
        Assert.assertEquals(DefaultShardStrategy.DEFAULT_SHARD, shard);
    }
}