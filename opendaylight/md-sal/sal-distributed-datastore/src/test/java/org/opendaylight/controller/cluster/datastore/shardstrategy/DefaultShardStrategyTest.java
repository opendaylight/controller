/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

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