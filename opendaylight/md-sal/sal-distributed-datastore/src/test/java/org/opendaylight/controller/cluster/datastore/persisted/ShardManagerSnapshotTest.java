/*
 * Copyright (c) 2017 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

/**
 * Unit tests for ShardManagerSnapshot.
 *
 * @author Thomas Pantelis
 */
public class ShardManagerSnapshotTest {

    @Test
    public void testSerialization() {
        ShardManagerSnapshot expected =
                new ShardManagerSnapshot(Arrays.asList("shard1", "shard2"));
        ShardManagerSnapshot cloned = (ShardManagerSnapshot) SerializationUtils.clone(expected);

        assertEquals("getShardList", expected.getShardList(), cloned.getShardList());
    }
}
