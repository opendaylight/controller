/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import akka.actor.ActorRef;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.sal.core.spi.entityownership.EntityOwnershipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * The distributed implementation of the EntityOwnershipService.
 *
 * @author Thomas Pantelis
 */
public class DistributedEntityOwnershipService implements EntityOwnershipService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedEntityOwnershipService.class);
    private static final String ENTITY_OWNERSHIP_SHARD_NAME = "entity-ownership";

    private final DistributedDataStore datastore;
    private volatile Future<ActorRef> entityOwnershipShardFuture;

    public DistributedEntityOwnershipService(DistributedDataStore datastore) {
        this.datastore = datastore;
    }

    public void start() {
        ActorRef shardManagerActor = datastore.getActorContext().getShardManager();

        CreateShard createShard = new CreateShard(ENTITY_OWNERSHIP_SHARD_NAME,
                datastore.getActorContext().getConfiguration().getUniqueMemberNamesForAllShards(),
                new EntityOwnershipShardPropsCreator(), null);

        Future<Object> createFuture = datastore.getActorContext().executeOperationAsync(shardManagerActor, createShard,
                new Timeout(1, TimeUnit.MINUTES));
        createFuture.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) {
                onCreateShardComplete(failure, response);
            }
        }, datastore.getActorContext().getClientDispatcher());
    }

    private void onCreateShardComplete(Throwable failure, Object response) {
        if(failure != null) {
            entityOwnershipShardFuture = Futures.failed(failure);
            return;
        }

        LOG.info("DistributedEntityOwnershipService successfully created {} shard", ENTITY_OWNERSHIP_SHARD_NAME);

        entityOwnershipShardFuture = datastore.getActorContext().findLocalShardAsync(ENTITY_OWNERSHIP_SHARD_NAME);
    }

    @Override
    public void close() {
    }
}
