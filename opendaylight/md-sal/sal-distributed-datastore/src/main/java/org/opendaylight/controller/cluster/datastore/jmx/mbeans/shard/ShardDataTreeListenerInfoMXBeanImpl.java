/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
public class ShardDataTreeListenerInfoMXBeanImpl extends AbstractMXBean implements ShardDataTreeListenerInfoMXBean {
    private static final String JMX_CATEGORY = "ShardDataTreeListenerInfo";

    private final OnDemandShardStateCache stateCache;

    public ShardDataTreeListenerInfoMXBeanImpl(final String shardName, final String mxBeanType,
            final ActorRef shardActor) {
        super(shardName, mxBeanType, JMX_CATEGORY);
        stateCache = new OnDemandShardStateCache(shardName, Preconditions.checkNotNull(shardActor));
    }

    @Override
    public List<DataTreeListenerInfo> getDataTreeChangeListenerInfo() {
        return getListenerActorsInfo(getState().getTreeChangeListenerActors());
    }

    @Override
    public List<DataTreeListenerInfo> getDataChangeListenerInfo() {
        return getListenerActorsInfo(getState().getDataChangeListenerActors());
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
    private List<DataTreeListenerInfo> getListenerActorsInfo(Collection<ActorSelection> actors) {
        final Timeout timeout = new Timeout(20, TimeUnit.SECONDS);
        final List<Future<Object>> futureList = new ArrayList<>(actors.size());
        for (ActorSelection actor: actors) {
            futureList.add(Patterns.ask(actor, GetInfo.INSTANCE, timeout));
        }

        try {
            final List<DataTreeListenerInfo> listenerInfoList = new ArrayList<>();
            Await.result(Futures.sequence(futureList, ExecutionContext.Implicits$.MODULE$.global()),
                    timeout.duration()).forEach(obj -> listenerInfoList.add((DataTreeListenerInfo) obj));
            return listenerInfoList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
