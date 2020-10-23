/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Throwables;
import com.google.common.collect.Streams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardDataTreeListenerInfoMXBean;
import org.opendaylight.controller.cluster.datastore.messages.DataTreeListenerInfo;
import org.opendaylight.controller.cluster.datastore.messages.GetInfo;
import org.opendaylight.controller.cluster.datastore.messages.OnDemandShardState;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

/**
 * Implementation of ShardDataTreeListenerInfoMXBean.
 *
 * @author Thomas Pantelis
 */
final class ShardDataTreeListenerInfoMXBeanImpl extends AbstractMXBean implements ShardDataTreeListenerInfoMXBean {
    private static final String JMX_CATEGORY = "ShardDataTreeListenerInfo";

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
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Akka's Await.result() API contract")
    private static List<DataTreeListenerInfo> getListenerActorsInfo(final Collection<ActorSelection> actors) {
        final Timeout timeout = new Timeout(20, TimeUnit.SECONDS);
        final List<Future<Object>> futureList = new ArrayList<>(actors.size());
        for (ActorSelection actor: actors) {
            futureList.add(Patterns.ask(actor, GetInfo.INSTANCE, timeout));
        }

        final Iterable<Object> listenerInfos;
        try {
            listenerInfos = Await.result(Futures.sequence(futureList, ExecutionContext.Implicits$.MODULE$.global()),
                timeout.duration());
        } catch (TimeoutException | InterruptedException e) {
            throw new IllegalStateException("Failed to acquire listeners", e);
        }

        return Streams.stream(listenerInfos).map(DataTreeListenerInfo.class::cast).collect(Collectors.toList());
    }
}
