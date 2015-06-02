/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import akka.actor.ActorSelection;
import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Future;

/**
 * Unit tests for PrimaryShardInfoFutureCache.
 *
 * @author Thomas Pantelis
 */
public class PrimaryShardInfoFutureCacheTest {

    @Test
    public void testOperations() {
        PrimaryShardInfoFutureCache cache = new PrimaryShardInfoFutureCache();

        assertEquals("getIfPresent", null, cache.getIfPresent("foo"));

        PrimaryShardInfo shardInfo = new PrimaryShardInfo(mock(ActorSelection.class), DataStoreVersions.CURRENT_VERSION,
                Optional.<DataTree>absent());
        cache.putSuccessful("foo", shardInfo);

        Future<PrimaryShardInfo> future = cache.getIfPresent("foo");
        assertNotNull("Null future", future);
        assertEquals("getIfPresent", shardInfo, future.value().get().get());

        cache.remove("foo");

        assertEquals("getIfPresent", null, cache.getIfPresent("foo"));
    }
}
