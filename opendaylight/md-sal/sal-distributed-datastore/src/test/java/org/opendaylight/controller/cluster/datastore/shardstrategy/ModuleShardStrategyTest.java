package org.opendaylight.controller.cluster.datastore.shardstrategy;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.ConfigurationImpl;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;

public class ModuleShardStrategyTest {
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private static Configuration configuration;

    @BeforeClass
    public static void setUpClass(){
        configuration = new ConfigurationImpl("module-shards.conf", "modules.conf");
    }


    @Test
    public void testFindShard() throws Exception {
        ModuleShardStrategy moduleShardStrategy =
            new ModuleShardStrategy("cars", configuration);

        String shard = moduleShardStrategy.findShard(CarsModel.BASE_PATH);

        Assert.assertEquals("cars-1", shard);
    }
}
