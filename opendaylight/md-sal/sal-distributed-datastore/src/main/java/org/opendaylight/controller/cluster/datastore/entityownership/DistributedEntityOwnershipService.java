/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.CANDIDATE_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNER_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityPath;

import akka.actor.ActorRef;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategy;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.messages.GetShardDataTree;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ModuleShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
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
    private static final String ENTITY_TYPE_PREFIX = "entity.type.";

    private final ConcurrentMap<DOMEntity, DOMEntity> registeredEntities = new ConcurrentHashMap<>();
    private final ActorContext context;

    private volatile ActorRef localEntityOwnershipShard;
    private volatile DataTree localEntityOwnershipShardDataTree;

    private DistributedEntityOwnershipService(final ActorContext context) {
        this.context = Preconditions.checkNotNull(context);
    }

    public static DistributedEntityOwnershipService start(final ActorContext context,
            final EntityOwnerSelectionStrategyConfig strategyConfig) {
        final ActorRef shardManagerActor = context.getShardManager();

        final Configuration configuration = context.getConfiguration();
        final Collection<MemberName> entityOwnersMemberNames = configuration.getUniqueMemberNamesForAllShards();
        final CreateShard createShard = new CreateShard(new ModuleShardConfiguration(EntityOwners.QNAME.getNamespace(),
                "entity-owners", ENTITY_OWNERSHIP_SHARD_NAME, ModuleShardStrategy.NAME, entityOwnersMemberNames),
                        newShardBuilder(context, strategyConfig), null);

        final Future<Object> createFuture = context.executeOperationAsync(shardManagerActor,
                createShard, MESSAGE_TIMEOUT);

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

    /**
     * Create shard with specific properties. This method support access from OSGi.
     *
     * @param context
     *            - used by non-actors (like DistributedDataStore) to work with actors a little more easily
     * @param propsResolver
     *            - specific object which has to contain field "Dictionary(String, Object) properties" for create
     *            EntityOwnerSelectionStrategyConfig
     * @return distributed entity ownership service
     */
    public static DistributedEntityOwnershipService start(final ActorContext context, final Object propsResolver) {
        return start(context, prepareProps(propsResolver));
    }

    /**
     * Create shard with specific properties defined by dictionary.
     *
     * @param context
     *            - used by non-actors (like DistributedDataStore) to work with actors a little more easily
     * @param props
     *            - properties for shard
     * @return distributed entity ownership service
     */
    public static DistributedEntityOwnershipService start(final ActorContext context,
            final Dictionary<String, Object> props) {
        return start(context, prepareStrategy(props));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static Dictionary<String, Object> prepareProps(final Object propsResolver) {
        Field field;
        Dictionary<String, Object> props = null;
        try {
            field = propsResolver.getClass().getDeclaredField("properties");
            field.setAccessible(true);
            props = (Dictionary<String, Object>) field.get(propsResolver);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalArgumentException("Input object has to contain properties field.", e);
        }

        return props;
    }

    private static EntityOwnerSelectionStrategyConfig prepareStrategy(final Dictionary<String, Object> properties) {
        final EntityOwnerSelectionStrategyConfig.Builder builder = EntityOwnerSelectionStrategyConfig.newBuilder();

        if (properties != null && !properties.isEmpty()) {
            final Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                final String key = keys.nextElement();
                if (!key.startsWith(ENTITY_TYPE_PREFIX)) {
                    LOG.debug("Ignoring non-conforming property key : {}");
                    continue;
                }

                final String[] strategyClassAndDelay = ((String) properties.get(key)).split(",");
                final Class<? extends EntityOwnerSelectionStrategy> aClass;
                try {
                    aClass = loadClass(strategyClassAndDelay[0]);
                } catch (final ClassNotFoundException e) {
                    LOG.error("Failed to load class {}, ignoring it", strategyClassAndDelay[0], e);
                    continue;
                }

                final long delay;
                if (strategyClassAndDelay.length > 1) {
                    delay = Long.parseLong(strategyClassAndDelay[1]);
                } else {
                    delay = 0;
                }

                final String entityType = key.substring(key.lastIndexOf(".") + 1);
                builder.addStrategy(entityType, aClass, delay);
                LOG.debug("Entity Type '{}' using strategy {} delay {}", entityType, aClass, delay);
            }
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends EntityOwnerSelectionStrategy> loadClass(final String strategyClassAndDelay)
            throws ClassNotFoundException {
        final Class<?> clazz;
        clazz = DistributedEntityOwnershipService.class.getClassLoader().loadClass(strategyClassAndDelay);

        Preconditions.checkArgument(EntityOwnerSelectionStrategy.class.isAssignableFrom(clazz),
                "Selected implementation %s must implement EntityOwnerSelectionStrategy, clazz");

        return (Class<? extends EntityOwnerSelectionStrategy>) clazz;
    }

    private void executeEntityOwnershipShardOperation(final ActorRef shardActor, final Object message) {
        final Future<Object> future = context.executeOperationAsync(shardActor, message, MESSAGE_TIMEOUT);
        future.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                if (failure != null) {
                    LOG.debug("Error sending message {} to {}", message, shardActor, failure);
                } else {
                    LOG.debug("{} message to {} succeeded", message, shardActor);
                }
            }
        }, context.getClientDispatcher());
    }

    @VisibleForTesting
    void executeLocalEntityOwnershipShardOperation(final Object message) {
        if (localEntityOwnershipShard == null) {
            final Future<ActorRef> future = context.findLocalShardAsync(ENTITY_OWNERSHIP_SHARD_NAME);
            future.onComplete(new OnComplete<ActorRef>() {
                @Override
                public void onComplete(final Throwable failure, final ActorRef shardActor) {
                    if (failure != null) {
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
        Preconditions.checkNotNull(entity, "entity cannot be null");

        if (registeredEntities.putIfAbsent(entity, entity) != null) {
            throw new CandidateAlreadyRegisteredException(entity);
        }

        final RegisterCandidateLocal registerCandidate = new RegisterCandidateLocal(entity);

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
        Preconditions.checkNotNull(entityType, "entityType cannot be null");
        Preconditions.checkNotNull(listener, "listener cannot be null");

        final RegisterListenerLocal registerListener = new RegisterListenerLocal(listener, entityType);

        LOG.debug("Registering listener with message: {}", registerListener);

        executeLocalEntityOwnershipShardOperation(registerListener);
        return new DistributedEntityOwnershipListenerRegistration(listener, entityType, this);
    }

    @Override
    public Optional<EntityOwnershipState> getOwnershipState(final DOMEntity forEntity) {
        Preconditions.checkNotNull(forEntity, "forEntity cannot be null");

        final DataTree dataTree = getLocalEntityOwnershipShardDataTree();
        if (dataTree == null) {
            return Optional.absent();
        }

        final Optional<NormalizedNode<?, ?>> entityNode = dataTree.takeSnapshot().readNode(
                entityPath(forEntity.getType(), forEntity.getIdentifier()));
        if (!entityNode.isPresent()) {
            return Optional.absent();
        }

        // Check if there are any candidates, if there are none we do not really have ownership state
        final MapEntryNode entity = (MapEntryNode) entityNode.get();
        final Optional<DataContainerChild<? extends PathArgument, ?>> optionalCandidates =
                entity.getChild(CANDIDATE_NODE_ID);
        final boolean hasCandidates = optionalCandidates.isPresent()
                && ((MapNode) optionalCandidates.get()).getValue().size() > 0;
        if (!hasCandidates) {
            return Optional.absent();
        }

        final MemberName localMemberName = context.getCurrentMemberName();
        final Optional<DataContainerChild<? extends PathArgument, ?>> ownerLeaf = entity.getChild(ENTITY_OWNER_NODE_ID);
        final String owner = ownerLeaf.isPresent() ? ownerLeaf.get().getValue().toString() : null;
        final boolean hasOwner = !Strings.isNullOrEmpty(owner);
        final boolean isOwner = hasOwner && localMemberName.getName().equals(owner);

        return Optional.of(EntityOwnershipState.from(isOwner, hasOwner));
    }

    @Override
    public boolean isCandidateRegistered(@Nonnull final DOMEntity entity) {
        return registeredEntities.get(entity) != null;
    }

    @VisibleForTesting
    @SuppressWarnings("checkstyle:IllegalCatch")
    DataTree getLocalEntityOwnershipShardDataTree() {
        if (localEntityOwnershipShardDataTree == null) {
            try {
                if (localEntityOwnershipShard == null) {
                    localEntityOwnershipShard = Await.result(context.findLocalShardAsync(
                            ENTITY_OWNERSHIP_SHARD_NAME), Duration.Inf());
                }

                localEntityOwnershipShardDataTree = (DataTree) Await.result(Patterns.ask(localEntityOwnershipShard,
                        GetShardDataTree.INSTANCE, MESSAGE_TIMEOUT), Duration.Inf());
            } catch (final Exception e) {
                LOG.error("Failed to find local {} shard", ENTITY_OWNERSHIP_SHARD_NAME, e);
            }
        }

        return localEntityOwnershipShardDataTree;
    }

    void unregisterListener(final String entityType, final DOMEntityOwnershipListener listener) {
        LOG.debug("Unregistering listener {} for entity type {}", listener, entityType);

        executeLocalEntityOwnershipShardOperation(new UnregisterListenerLocal(listener, entityType));
    }

    @Override
    public void close() {
    }

    private static EntityOwnershipShard.Builder newShardBuilder(final ActorContext context,
            final EntityOwnerSelectionStrategyConfig strategyConfig) {
        return EntityOwnershipShard.newBuilder().localMemberName(context.getCurrentMemberName())
                .ownerSelectionStrategyConfig(strategyConfig);
    }

    @VisibleForTesting
    ActorRef getLocalEntityOwnershipShard() {
        return localEntityOwnershipShard;
    }
}
