/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Throwables;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.pattern.Patterns;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardDataTreeListenerInfoMXBean;
import org.opendaylight.controller.cluster.datastore.messages.GetInfo;
import org.opendaylight.controller.cluster.datastore.messages.OnDemandShardState;
import org.opendaylight.controller.cluster.mgmt.api.DataTreeListenerInfo;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;

/**
 * Implementation of ShardDataTreeListenerInfoMXBean.
 *
 * @author Thomas Pantelis
 */
final class ShardDataTreeListenerInfoMXBeanImpl extends AbstractMXBean implements ShardDataTreeListenerInfoMXBean {
    private static final String JMX_CATEGORY = "ShardDataTreeListenerInfo";
    // FIXME: why 20 seconds?
    private static final long TIMEOUT_SECONDS = 20;
    private static final Duration TIMEOUT = Duration.ofSeconds(TIMEOUT_SECONDS);

    private final OnDemandShardStateCache stateCache;

    ShardDataTreeListenerInfoMXBeanImpl(final String shardName, final String mxBeanType, final ActorRef shardActor) {
        super(shardName, mxBeanType, JMX_CATEGORY);
        stateCache = new OnDemandShardStateCache(shardName, requireNonNull(shardActor));
    }

    @Override
    public List<DataTreeListenerInfo> getDataTreeChangeListenerInfo() {
        return getListenerActorsInfo(getState().getTreeChangeListenerActors());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private OnDemandShardState getState() {
        try {
            return stateCache.get();
        } catch (Exception e) {
            Throwables.throwIfUnchecked(e);
            throw new IllegalStateException(e);
        }
    }

    private static List<DataTreeListenerInfo> getListenerActorsInfo(final Collection<ActorSelection> actors) {
        final var futures = actors.stream()
            .map(actor -> Patterns.ask(actor, GetInfo.INSTANCE, TIMEOUT))
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new);

        try {
            CompletableFuture.allOf(futures).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new IllegalStateException("Failed to acquire listeners", e);
        }

        return Arrays.stream(futures)
            .map(future -> (DataTreeListenerInfo) future.resultNow())
            .collect(Collectors.toList());
    }
}
