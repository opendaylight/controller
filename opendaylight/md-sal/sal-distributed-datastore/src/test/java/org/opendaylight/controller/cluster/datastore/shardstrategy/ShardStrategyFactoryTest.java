/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.shardstrategy;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class ShardStrategyFactoryTest {

    ShardStrategyFactory factory;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() {
        factory = new ShardStrategyFactory(new ConfigurationImpl("module-shards.conf", "modules.conf"));
    }

    @Test
    public void testGetStrategy() {
        ShardStrategy strategy = factory.getStrategy(TestModel.TEST_PATH);
        assertNotNull(strategy);
    }

    @Test
    public void testGetStrategyForKnownModuleName() {
        ShardStrategy strategy = factory.getStrategy(YangInstanceIdentifier.of(CarsModel.BASE_QNAME));
        assertTrue(strategy instanceof ModuleShardStrategy);
    }


    @Test
    public void testGetStrategyNullPointerExceptionWhenPathIsNull() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("path should not be null");

        factory.getStrategy(null);
    }

}
