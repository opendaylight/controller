/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardManagerIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.Dispatchers;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains ShardManager actor instances.
 *
 * @author Thomas Pantelis
 */
class ShardManagerActors {
    private static final Logger LOG = LoggerFactory.getLogger(ShardManagerActors.class);

    private static final ShardManagerActors INSTANCE = new ShardManagerActors();

    private final Collection<ActorRef> shardManagerActors = new CopyOnWriteArraySet<>();

    private ShardManagerActors() {
    }

    static ShardManagerActors getInstance() {
        return INSTANCE;
    }

    ActorRef newInstance(ActorSystem actorSystem, ClusterWrapper cluster, Configuration configuration,
            DatastoreContext datastoreContext, PrimaryShardInfoFutureCache primaryShardInfoCache,
            CountDownLatch waitTillReadyCountDownLatch) {

        String shardManagerId = ShardManagerIdentifier.builder().type(datastoreContext.getDataStoreType()).
                build().toString();
        String shardDispatcher = new Dispatchers(actorSystem.dispatchers()).getDispatcherPath(
                Dispatchers.DispatcherType.Shard);

        LOG.info("Creating ShardManager : {}", shardManagerId);

        Exception lastException = null;
        for (int i = 0; i < 100; i++) {
            try {
                ActorRef actorRef = actorSystem.actorOf(ShardManager.props(cluster, configuration, datastoreContext,
                        waitTillReadyCountDownLatch,primaryShardInfoCache).withDispatcher(shardDispatcher).withMailbox(
                                        ActorContext.MAILBOX), shardManagerId);
                shardManagerActors.add(actorRef);
                return actorRef;
            } catch (Exception e){
                lastException = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                LOG.debug(String.format("Could not create actor %s because of %s - waiting for sometime before retrying (retry count = %d)",
                        shardManagerId, e.getMessage(), i));
            }
        }

        throw new IllegalStateException("Failed to create Shard Manager", lastException);

    }

    void remove(ActorRef shardManagerActor) {
        shardManagerActors.remove(shardManagerActor);
    }

    void sendMessage(Object message) {
        for(ActorRef actorRef: shardManagerActors) {
            actorRef.tell(message, ActorRef.noSender());
        }
    }
}
