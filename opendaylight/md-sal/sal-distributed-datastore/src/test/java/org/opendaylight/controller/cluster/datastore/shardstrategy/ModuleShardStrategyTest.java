package org.opendaylight.controller.cluster.datastore.shardstrategy;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.ConfigurationImpl;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

import static junit.framework.Assert.assertEquals;

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

        assertEquals("cars-1", shard);
    }

    @Test
    public void testFindShardWhenModuleConfigurationPresentInModulesButMissingInModuleShards() {

        final QName BASE_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test:missing", "2014-03-13",
            "missing");

        final YangInstanceIdentifier BASE_PATH = YangInstanceIdentifier.of(BASE_QNAME);

        ModuleShardStrategy moduleShardStrategy =
            new ModuleShardStrategy("missing", configuration);

        String shard = moduleShardStrategy.findShard(BASE_PATH);

        assertEquals(DefaultShardStrategy.DEFAULT_SHARD, shard);

    }
}
