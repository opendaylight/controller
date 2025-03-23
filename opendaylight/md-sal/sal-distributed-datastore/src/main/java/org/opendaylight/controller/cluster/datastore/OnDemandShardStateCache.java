/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.util.Timeout;
import org.opendaylight.controller.cluster.datastore.messages.OnDemandShardState;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import scala.concurrent.Await;

/**
 * Maintains a short-lived shared cache of OnDemandShardState.
 *
 * @author Thomas Pantelis
 */
final class OnDemandShardStateCache {
    private static final Cache<String, OnDemandShardState> ONDEMAND_SHARD_STATE_CACHE =
            CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS).build();

    private final ActorRef shardActor;
    private final String shardName;
    private volatile String stateRetrievalTime;

    OnDemandShardStateCache(final String shardName, final ActorRef shardActor) {
        this.shardName = requireNonNull(shardName);
        this.shardActor = shardActor;
    }

    OnDemandShardState get() throws Exception {
        if (shardActor == null) {
            return new OnDemandShardState.Builder().build();
        }

        return ONDEMAND_SHARD_STATE_CACHE.get(shardName, this::retrieveState);
    }

    String stateRetrievalTime() {
        return stateRetrievalTime;
    }

    private OnDemandShardState retrieveState() throws Exception {
        stateRetrievalTime = null;
        Timeout timeout = new Timeout(10, TimeUnit.SECONDS);
        Stopwatch timer = Stopwatch.createStarted();

        OnDemandShardState state = (OnDemandShardState) Await.result(Patterns.ask(shardActor,
                GetOnDemandRaftState.INSTANCE, timeout), timeout.duration());

        stateRetrievalTime = timer.stop().toString();
        return state;
    }
}
