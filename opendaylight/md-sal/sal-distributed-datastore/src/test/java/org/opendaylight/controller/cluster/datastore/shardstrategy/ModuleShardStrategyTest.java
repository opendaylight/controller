/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardstrategy;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ModuleShardStrategyTest {
    private static Configuration configuration;

    @BeforeClass
    public static void setUpClass() {
        configuration = new ConfigurationImpl("module-shards.conf", "modules.conf");
    }

    @Test
    public void testFindShard() {
        ModuleShardStrategy moduleShardStrategy = new ModuleShardStrategy("cars", configuration);
        String shard = moduleShardStrategy.findShard(CarsModel.BASE_PATH);
        assertEquals("cars-1", shard);
    }

    @Test
    public void testFindShardWhenModuleConfigurationPresentInModulesButMissingInModuleShards() {
        final QName baseQName = QName.create(
                "urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test:missing", "2014-03-13",
                "missing");

        final YangInstanceIdentifier BASE_PATH = YangInstanceIdentifier.of(baseQName);

        ModuleShardStrategy moduleShardStrategy = new ModuleShardStrategy("missing", configuration);
        String shard = moduleShardStrategy.findShard(BASE_PATH);
        assertEquals(DefaultShardStrategy.DEFAULT_SHARD, shard);
    }
}
