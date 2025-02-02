/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.common.util.concurrent.Futures;
import org.apache.pekko.actor.ActorSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.controller.cluster.datastore.DataStoreVersions;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;

/**
 * Unit tests for PrimaryShardInfoFutureCache.
 *
 * @author Thomas Pantelis
 */
@ExtendWith(MockitoExtension.class)
class PrimaryShardInfoFutureCacheTest {
    @Mock
    private ActorSelection actorSelection;

    private final PrimaryShardInfoFutureCache cache = new PrimaryShardInfoFutureCache();

    @Test
    void testOperations() throws Exception {
        assertNull(cache.getIfPresent("foo"));

        final var shardInfo = new PrimaryShardInfo(actorSelection, DataStoreVersions.CURRENT_VERSION);
        cache.putSuccessful("foo", shardInfo);

        final var future = cache.getIfPresent("foo");
        assertNotNull(future);
        assertEquals(shardInfo, Futures.getDone(future.toCompletableFuture()));

        cache.remove("foo");

        assertNull(cache.getIfPresent("foo"));
    }
}
