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

import com.google.common.util.concurrent.Futures;
import java.util.Optional;
import org.apache.pekko.actor.ActorSelection;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;

/**
 * Unit tests for PrimaryShardInfoFutureCache.
 *
 * @author Thomas Pantelis
 */
public class PrimaryShardInfoFutureCacheTest {
    @Test
    public void testOperations() throws Exception {
        PrimaryShardInfoFutureCache cache = new PrimaryShardInfoFutureCache();

        assertEquals("getIfPresent", null, cache.getIfPresent("foo"));

        final var shardInfo = new PrimaryShardInfo(mock(ActorSelection.class), DataStoreVersions.CURRENT_VERSION);
        cache.putSuccessful("foo", shardInfo);

        final var future = cache.getIfPresent("foo");
        assertNotNull("Null future", future);
        assertEquals("getIfPresent", Optional.of(shardInfo), Futures.getDone(future.toCompletableFuture()));

        cache.remove("foo");

        assertEquals("getIfPresent", null, cache.getIfPresent("foo"));
    }
}
