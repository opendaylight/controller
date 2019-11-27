/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.CANDIDATE_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNER_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityPath;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.messages.GetShardDataTree;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ModuleShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.EntityOwners;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * The distributed implementation of the EntityOwnershipService.
 *
 * @author Thomas Pantelis
 */
public class DistributedEntityOwnershipService implements DOMEntityOwnershipService, AutoCloseable {
    @VisibleForTesting
    static final String ENTITY_OWNERSHIP_SHARD_NAME = "entity-ownership";

    private static final Logger LOG = LoggerFactory.getLogger(DistributedEntityOwnershipService.class);
    private static final Timeout MESSAGE_TIMEOUT = new Timeout(1, TimeUnit.MINUTES);

    private final ConcurrentMap<DOMEntity, DOMEntity> registeredEntities = new ConcurrentHashMap<>();
    private final ActorUtils context;

    private volatile ActorRef localEntityOwnershipShard;
    private volatile DataTree localEntityOwnershipShardDataTree;

    DistributedEntityOwnershipService(final ActorUtils context) {
        this.context = requireNonNull(context);
    }

    public static DistributedEntityOwnershipService start(final ActorUtils context,
            final EntityOwnerSelectionStrategyConfig strategyConfig) {
        ActorRef shardManagerActor = context.getShardManager();

        Configuration configuration = context.getConfiguration();
        Collection<MemberName> entityOwnersMemberNames = configuration.getUniqueMemberNamesForAllShards();
        CreateShard createShard = new CreateShard(new ModuleShardConfiguration(EntityOwners.QNAME.getNamespace(),
                "entity-owners", ENTITY_OWNERSHIP_SHARD_NAME, null, ModuleShardStrategy.NAME,
                entityOwnersMemberNames), newShardBuilder(context, strategyConfig), null);

        Future<Object> createFuture = context.executeOperationAsync(shardManagerActor, createShard, MESSAGE_TIMEOUT);
        createFuture.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                if (failure != null) {
                    LOG.error("Failed to create {} shard", ENTITY_OWNERSHIP_SHARD_NAME, failure);
                } else {
                    LOG.info("Successfully created {} shard", ENTITY_OWNERSHIP_SHARD_NAME);
                }
            }
        }, context.getClientDispatcher());

        return new DistributedEntityOwnershipService(context);
    }

    private void executeEntityOwnershipShardOperation(final ActorRef shardActor, final Object message) {
        Future<Object> future = context.executeOperationAsync(shardActor, message, MESSAGE_TIMEOUT);
        future.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                if (failure != null) {
                    // FIXME: CONTROLLER-1904: reduce the severity to info once we have a retry mechanism
                    LOG.error("Error sending message {} to {}", message, shardActor, failure);
                } else {
                    LOG.debug("{} message to {} succeeded", message, shardActor);
                }
            }
        }, context.getClientDispatcher());
    }

    @VisibleForTesting
    void executeLocalEntityOwnershipShardOperation(final Object message) {
        if (localEntityOwnershipShard == null) {
            Future<ActorRef> future = context.findLocalShardAsync(ENTITY_OWNERSHIP_SHARD_NAME);
            future.onComplete(new OnComplete<ActorRef>() {
                @Override
                public void onComplete(final Throwable failure, final ActorRef shardActor) {
                    if (failure != null) {
                        // FIXME: CONTROLLER-1904: reduce the severity to info once we have a retry mechanism
                        LOG.error("Failed to find local {} shard", ENTITY_OWNERSHIP_SHARD_NAME, failure);
                    } else {
                        localEntityOwnershipShard = shardActor;
                        executeEntityOwnershipShardOperation(localEntityOwnershipShard, message);
                    }
                }
            }, context.getClientDispatcher());

        } else {
            executeEntityOwnershipShardOperation(localEntityOwnershipShard, message);
        }
    }

    @Override
    public DOMEntityOwnershipCandidateRegistration registerCandidate(final DOMEntity entity)
            throws CandidateAlreadyRegisteredException {
        requireNonNull(entity, "entity cannot be null");

        if (registeredEntities.putIfAbsent(entity, entity) != null) {
            throw new CandidateAlreadyRegisteredException(entity);
        }

        RegisterCandidateLocal registerCandidate = new RegisterCandidateLocal(entity);

        LOG.debug("Registering candidate with message: {}", registerCandidate);

        executeLocalEntityOwnershipShardOperation(registerCandidate);
        return new DistributedEntityOwnershipCandidateRegistration(entity, this);
    }

    void unregisterCandidate(final DOMEntity entity) {
        LOG.debug("Unregistering candidate for {}", entity);

        executeLocalEntityOwnershipShardOperation(new UnregisterCandidateLocal(entity));
        registeredEntities.remove(entity);
    }

    @Override
    public DOMEntityOwnershipListenerRegistration registerListener(final String entityType,
            final DOMEntityOwnershipListener listener) {
        RegisterListenerLocal registerListener = new RegisterListenerLocal(listener, entityType);

        LOG.debug("Registering listener with message: {}", registerListener);

        executeLocalEntityOwnershipShardOperation(registerListener);
        return new DistributedEntityOwnershipListenerRegistration(listener, entityType, this);
    }

    @Override
    public Optional<EntityOwnershipState> getOwnershipState(final DOMEntity forEntity) {
        requireNonNull(forEntity, "forEntity cannot be null");

        DataTree dataTree = getLocalEntityOwnershipShardDataTree();
        if (dataTree == null) {
            return Optional.empty();
        }

        Optional<NormalizedNode<?, ?>> entityNode = dataTree.takeSnapshot().readNode(
                entityPath(forEntity.getType(), forEntity.getIdentifier()));
        if (!entityNode.isPresent()) {
            return Optional.empty();
        }

        // Check if there are any candidates, if there are none we do not really have ownership state
        final MapEntryNode entity = (MapEntryNode) entityNode.get();
        final Optional<DataContainerChild<? extends PathArgument, ?>> optionalCandidates =
                entity.getChild(CANDIDATE_NODE_ID);
        final boolean hasCandidates = optionalCandidates.isPresent()
                && ((MapNode) optionalCandidates.get()).getValue().size() > 0;
        if (!hasCandidates) {
            return Optional.empty();
        }

        MemberName localMemberName = context.getCurrentMemberName();
        Optional<DataContainerChild<? extends PathArgument, ?>> ownerLeaf = entity.getChild(ENTITY_OWNER_NODE_ID);
        String owner = ownerLeaf.isPresent() ? ownerLeaf.get().getValue().toString() : null;
        boolean hasOwner = !Strings.isNullOrEmpty(owner);
        boolean isOwner = hasOwner && localMemberName.getName().equals(owner);

        return Optional.of(EntityOwnershipState.from(isOwner, hasOwner));
    }

    @Override
    public boolean isCandidateRegistered(final DOMEntity entity) {
        return registeredEntities.get(entity) != null;
    }

    @VisibleForTesting
    @SuppressWarnings("checkstyle:IllegalCatch")
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Akka's Await.result() API contract")
    DataTree getLocalEntityOwnershipShardDataTree() {
        final DataTree local = localEntityOwnershipShardDataTree;
        if (local != null) {
            return local;
        }

        if (localEntityOwnershipShard == null) {
            try {
                localEntityOwnershipShard = Await.result(context.findLocalShardAsync(
                    ENTITY_OWNERSHIP_SHARD_NAME), Duration.Inf());
            } catch (TimeoutException | InterruptedException e) {
                LOG.error("Failed to find local {} shard", ENTITY_OWNERSHIP_SHARD_NAME, e);
                return null;
            }
        }

        try {
            return localEntityOwnershipShardDataTree = (DataTree) Await.result(Patterns.ask(localEntityOwnershipShard,
                GetShardDataTree.INSTANCE, MESSAGE_TIMEOUT), Duration.Inf());
        } catch (TimeoutException | InterruptedException e) {
            LOG.error("Failed to find local {} shard", ENTITY_OWNERSHIP_SHARD_NAME, e);
            return null;
        }
    }

    void unregisterListener(final String entityType, final DOMEntityOwnershipListener listener) {
        LOG.debug("Unregistering listener {} for entity type {}", listener, entityType);

        executeLocalEntityOwnershipShardOperation(new UnregisterListenerLocal(listener, entityType));
    }

    @Override
    public void close() {
    }

    private static EntityOwnershipShard.Builder newShardBuilder(final ActorUtils context,
            final EntityOwnerSelectionStrategyConfig strategyConfig) {
        return EntityOwnershipShard.newBuilder().localMemberName(context.getCurrentMemberName())
                .ownerSelectionStrategyConfig(strategyConfig);
    }

    @VisibleForTesting
    ActorRef getLocalEntityOwnershipShard() {
        return localEntityOwnershipShard;
    }
}
