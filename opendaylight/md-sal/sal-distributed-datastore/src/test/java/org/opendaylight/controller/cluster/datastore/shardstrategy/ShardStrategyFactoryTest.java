package org.opendaylight.controller.cluster.datastore.shardstrategy;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.cluster.datastore.ConfigurationImpl;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class ShardStrategyFactoryTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @BeforeClass
    public static void setUpClass(){
        ShardStrategyFactory.setConfiguration(new ConfigurationImpl("module-shards.conf", "modules.conf"));
    }

    @Test
    public void testGetStrategy() {
        ShardStrategy strategy =
            ShardStrategyFactory.getStrategy(TestModel.TEST_PATH);
        assertNotNull(strategy);
    }

    @Test
    public void testGetStrategyForKnownModuleName() {
        ShardStrategy strategy =
            ShardStrategyFactory.getStrategy(
                YangInstanceIdentifier.of(CarsModel.BASE_QNAME));
        assertTrue(strategy instanceof ModuleShardStrategy);
    }


    @Test
    public void testGetStrategyNullPointerExceptionWhenPathIsNull() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("path should not be null");

        ShardStrategyFactory.getStrategy(null);
    }

}
