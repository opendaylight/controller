/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidate;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
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
    static final String ENTITY_OWNERSHIP_SHARD_NAME = "entity-ownership";
    private static final Timeout MESSAGE_TIMEOUT = new Timeout(1, TimeUnit.MINUTES);

    private final DistributedDataStore datastore;
    private final ConcurrentMap<Entity, EntityOwnershipCandidate> registeredEntities = new ConcurrentHashMap<>();
    private volatile ActorRef localEntityOwnershipShard;

    public DistributedEntityOwnershipService(DistributedDataStore datastore) {
        this.datastore = datastore;
    }

    public void start() {
        ActorRef shardManagerActor = datastore.getActorContext().getShardManager();

        CreateShard createShard = new CreateShard(ENTITY_OWNERSHIP_SHARD_NAME,
                datastore.getActorContext().getConfiguration().getUniqueMemberNamesForAllShards(),
                newShardPropsCreator(), null);

        Future<Object> createFuture = datastore.getActorContext().executeOperationAsync(shardManagerActor,
                createShard, MESSAGE_TIMEOUT);

        createFuture.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) {
                if(failure != null) {
                    LOG.error("Failed to create {} shard", ENTITY_OWNERSHIP_SHARD_NAME);
                } else {
                    LOG.info("Successfully created {} shard", ENTITY_OWNERSHIP_SHARD_NAME);
                }
            }
        }, datastore.getActorContext().getClientDispatcher());
    }

    private void executeEntityOwnershipShardOperation(final ActorRef shardActor, final Object message) {
        Future<Object> future = datastore.getActorContext().executeOperationAsync(shardActor, message, MESSAGE_TIMEOUT);
        future.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) {
                if(failure != null) {
                    LOG.debug("Error sending message {} to {}", message, shardActor, failure);
                } else {
                    LOG.debug("{} message to {} succeeded", message, shardActor, failure);
                }
            }
        }, datastore.getActorContext().getClientDispatcher());
    }

    private void executeLocalEntityOwnershipShardOperation(final Object message) {
        if(localEntityOwnershipShard == null) {
            Future<ActorRef> future = datastore.getActorContext().findLocalShardAsync(ENTITY_OWNERSHIP_SHARD_NAME);
            future.onComplete(new OnComplete<ActorRef>() {
                @Override
                public void onComplete(Throwable failure, ActorRef shardActor) {
                    if(failure != null) {
                        LOG.error("Failed to find local {} shard", ENTITY_OWNERSHIP_SHARD_NAME, failure);
                    } else {
                        localEntityOwnershipShard = shardActor;
                        executeEntityOwnershipShardOperation(localEntityOwnershipShard, message);
                    }
                }
            }, datastore.getActorContext().getClientDispatcher());

        } else {
            executeEntityOwnershipShardOperation(localEntityOwnershipShard, message);
        }
    }

    @Override
    public EntityOwnershipCandidateRegistration registerCandidate(Entity entity, EntityOwnershipCandidate candidate)
            throws CandidateAlreadyRegisteredException {

        EntityOwnershipCandidate currentCandidate = registeredEntities.putIfAbsent(entity, candidate);
        if(currentCandidate != null) {
            throw new CandidateAlreadyRegisteredException(entity, currentCandidate);
        }

        RegisterCandidateLocal registerCandidate = new RegisterCandidateLocal(candidate, entity);

        LOG.debug("Registering candidate with message: {}", registerCandidate);

        executeLocalEntityOwnershipShardOperation(registerCandidate);
        return new DistributedEntityOwnershipCandidateRegistration(candidate, entity, this);
    }

    void unregisterCandidate(Entity entity, EntityOwnershipCandidate entityOwnershipCandidate) {
        LOG.debug("Unregistering candidate for {}", entity);

        executeLocalEntityOwnershipShardOperation(new UnregisterCandidateLocal(entityOwnershipCandidate, entity));
        registeredEntities.remove(entity);
    }

    @Override
    public EntityOwnershipListenerRegistration registerListener(Entity entity, EntityOwnershipListener listener) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() {
    }

    protected EntityOwnershipShardPropsCreator newShardPropsCreator() {
        return new EntityOwnershipShardPropsCreator(datastore.getActorContext().getCurrentMemberName());
    }

    @VisibleForTesting
    ActorRef getLocalEntityOwnershipShard() {
        return localEntityOwnershipShard;
    }
}
