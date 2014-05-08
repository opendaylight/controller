package org.opendaylight.controller.cluster.datastore.shardstrategy;

import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;

public class DefaultShardStrategyTest {

  @Test
  public void testFindShard() throws Exception {
    String shard = new DefaultShardStrategy().findShard(TestModel.TEST_PATH);
    Assert.assertEquals(DefaultShardStrategy.DEFAULT_SHARD, shard);
  }
}