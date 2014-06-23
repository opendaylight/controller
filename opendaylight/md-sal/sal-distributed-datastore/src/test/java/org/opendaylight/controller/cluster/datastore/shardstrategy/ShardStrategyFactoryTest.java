package org.opendaylight.controller.cluster.datastore.shardstrategy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;

import static junit.framework.Assert.assertNotNull;

public class ShardStrategyFactoryTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test
  public void testGetStrategy(){
    ShardStrategy strategy = ShardStrategyFactory.getStrategy(TestModel.TEST_PATH);
    assertNotNull(strategy);
  }

  @Test
  public void testGetStrategyNullPointerExceptionWhenPathIsNull(){
    expectedEx.expect(NullPointerException.class);
    expectedEx.expectMessage("path should not be null");

    ShardStrategyFactory.getStrategy(null);
  }

}