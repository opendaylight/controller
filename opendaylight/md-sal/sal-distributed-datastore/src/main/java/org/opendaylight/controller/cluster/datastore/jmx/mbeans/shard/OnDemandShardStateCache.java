/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.messages.OnDemandShardState;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import scala.concurrent.Await;

/**
 * Maintains a short-lived shared cache of OnDemandShardState.
 *
 * @author Thomas Pantelis
 */
class OnDemandShardStateCache {
    private static final Cache<String, OnDemandShardState> ONDEMAND_SHARD_STATE_CACHE =
            CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS).build();

    private final ActorRef shardActor;
    private final String shardName;
    private volatile String stateRetrievalTime;

    OnDemandShardStateCache(String shardName, ActorRef shardActor) {
        this.shardName = Preconditions.checkNotNull(shardName);
        this.shardActor = shardActor;
    }

    OnDemandShardState get() throws Exception {
        if (shardActor == null) {
            return OnDemandShardState.newBuilder().build();
        }

        try {
            return ONDEMAND_SHARD_STATE_CACHE.get(shardName, this::retrieveState);
        } catch (ExecutionException | UncheckedExecutionException | ExecutionError e) {
            if (e.getCause() != null) {
                Throwables.propagateIfPossible(e.getCause(), Exception.class);
                throw new RuntimeException("unexpected", e.getCause());
            }

            throw e;
        }
    }

    String getStatRetrievaelTime() {
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
