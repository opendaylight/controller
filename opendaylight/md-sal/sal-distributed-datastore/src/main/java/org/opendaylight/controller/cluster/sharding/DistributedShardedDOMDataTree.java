/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import static akka.actor.ActorRef.noSender;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.SimpleDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ModuleShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.sharding.ShardedDataTreeActor.ShardedDataTreeActorCreator;
import org.opendaylight.controller.cluster.sharding.messages.InitConfigListener;
import org.opendaylight.controller.cluster.sharding.messages.LookupPrefixShard;
import org.opendaylight.controller.cluster.sharding.messages.PrefixShardRemovalLookup;
import org.opendaylight.controller.cluster.sharding.messages.ProducerCreated;
import org.opendaylight.controller.cluster.sharding.messages.ProducerRemoved;
import org.opendaylight.controller.cluster.sharding.messages.StartConfigShardLookup;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCursorAwareTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeLoopException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShard;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.mdsal.dom.broker.DOMDataTreeShardRegistration;
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataTree;
import org.opendaylight.mdsal.dom.spi.DOMDataTreePrefixTable;
import org.opendaylight.mdsal.dom.spi.DOMDataTreePrefixTableEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.prefix.shard.configuration.rev170110.PrefixShards;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * A layer on top of DOMDataTreeService that distributes producer/shard registrations to remote nodes via
 * {@link ShardedDataTreeActor}. Also provides QoL method for addition of prefix based clustered shard into the system.
 */
public class DistributedShardedDOMDataTree implements DOMDataTreeService, DOMDataTreeShardingService,
        DistributedShardFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedShardedDOMDataTree.class);

    private static final Timeout DEFAULT_ASK_TIMEOUT = new Timeout(15, TimeUnit.SECONDS);
    private static final int MAX_ACTOR_CREATION_RETRIES = 100;
    private static final int ACTOR_RETRY_DELAY = 100;
    private static final TimeUnit ACTOR_RETRY_TIME_UNIT = TimeUnit.MILLISECONDS;
    private static final int SHARD_FUTURE_TIMEOUT = LookupTask.LOOKUP_TASK_MAX_RETRIES
            * LookupTask.LOOKUP_TASK_MAX_RETRIES * 3;

    static final String ACTOR_ID = "ShardedDOMDataTreeFrontend";

    private final ShardedDOMDataTree shardedDOMDataTree;
    private final ActorSystem actorSystem;
    private final DistributedDataStore distributedOperDatastore;
    private final DistributedDataStore distributedConfigDatastore;

    private final ActorRef shardedDataTreeActor;
    private final MemberName memberName;

    private final DOMDataTreePrefixTable<DOMDataTreeShardRegistration<DOMDataTreeShard>> shards =
            DOMDataTreePrefixTable.create();

    private final EnumMap<LogicalDatastoreType, DistributedShardRegistration> defaultShardRegistrations =
            new EnumMap<>(LogicalDatastoreType.class);

    private final EnumMap<LogicalDatastoreType, Entry<DataStoreClient, ActorRef>> configurationShardMap =
            new EnumMap<>(LogicalDatastoreType.class);

    private final EnumMap<LogicalDatastoreType, ShardConfigWriter> writerMap =
            new EnumMap<>(LogicalDatastoreType.class);

    private final ShardConfigUpdateHandler updateHandler;

    public DistributedShardedDOMDataTree(final ActorSystemProvider actorSystemProvider,
                                         final DistributedDataStore distributedOperDatastore,
                                         final DistributedDataStore distributedConfigDatastore) {
        this.actorSystem = Preconditions.checkNotNull(actorSystemProvider).getActorSystem();
        this.distributedOperDatastore = Preconditions.checkNotNull(distributedOperDatastore);
        this.distributedConfigDatastore = Preconditions.checkNotNull(distributedConfigDatastore);
        shardedDOMDataTree = new ShardedDOMDataTree();

        shardedDataTreeActor = createShardedDataTreeActor(actorSystem,
                new ShardedDataTreeActorCreator()
                        .setShardingService(this)
                        .setActorSystem(actorSystem)
                        .setClusterWrapper(distributedConfigDatastore.getActorContext().getClusterWrapper())
                        .setDistributedConfigDatastore(distributedConfigDatastore)
                        .setDistributedOperDatastore(distributedOperDatastore),
                ACTOR_ID);

        this.memberName = distributedConfigDatastore.getActorContext().getCurrentMemberName();

        updateHandler = new ShardConfigUpdateHandler(shardedDataTreeActor,
                distributedConfigDatastore.getActorContext().getCurrentMemberName());

        LOG.debug("{} - Starting prefix configuration shards",
                distributedConfigDatastore.getActorContext().getCurrentMemberName());
        createPrefixConfigShard(distributedConfigDatastore);
        createPrefixConfigShard(distributedOperDatastore);
    }

    private void createPrefixConfigShard(final DistributedDataStore dataStore) {
        Configuration configuration = dataStore.getActorContext().getConfiguration();
        Collection<MemberName> memberNames = configuration.getUniqueMemberNamesForAllShards();
        CreateShard createShardMessage =
                new CreateShard(new ModuleShardConfiguration(PrefixShards.QNAME.getNamespace(),
                        "prefix-shard-configuration", ClusterUtils.PREFIX_CONFIG_SHARD_ID, ModuleShardStrategy.NAME,
                        memberNames),
                        Shard.builder(), dataStore.getActorContext().getDatastoreContext());

        dataStore.getActorContext().getShardManager().tell(createShardMessage, noSender());
    }

    void init() {
        // create our writers to the configuration
        try {
            LOG.debug("{} - starting config shard lookup.",
                    distributedConfigDatastore.getActorContext().getCurrentMemberName());

            handleConfigShardLookup().get();
            Thread.sleep(5000);

            writerMap.put(LogicalDatastoreType.CONFIGURATION, new ShardConfigWriter(
                    configurationShardMap.get(LogicalDatastoreType.CONFIGURATION).getKey()));

            writerMap.put(LogicalDatastoreType.OPERATIONAL, new ShardConfigWriter(
                    configurationShardMap.get(LogicalDatastoreType.OPERATIONAL).getKey()));

            updateHandler.initListener(distributedConfigDatastore, LogicalDatastoreType.CONFIGURATION);
            updateHandler.initListener(distributedOperDatastore, LogicalDatastoreType.OPERATIONAL);

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to create prefix config shard", e);
        }

        distributedConfigDatastore.getActorContext().getShardManager().tell(new InitConfigListener(), noSender());
        distributedOperDatastore.getActorContext().getShardManager().tell(new InitConfigListener(), noSender());


        //create shard registration for DEFAULT_SHARD
        try {
            defaultShardRegistrations.put(LogicalDatastoreType.CONFIGURATION,
                    initDefaultShard(LogicalDatastoreType.CONFIGURATION));
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Unable to create default shard frontend for config shard", e);
        }

        try {
            defaultShardRegistrations.put(LogicalDatastoreType.OPERATIONAL,
                    initDefaultShard(LogicalDatastoreType.OPERATIONAL));
        } catch (final InterruptedException | ExecutionException e) {
            LOG.error("Unable to create default shard frontend for operational shard", e);
        }
    }

    private ListenableFuture<List<Void>> handleConfigShardLookup() {

        final ListenableFuture<Void> configFuture = lookupConfigShard(LogicalDatastoreType.CONFIGURATION);
        Futures.addCallback(configFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
                try {
                    LOG.debug("Config ready, creating client");
                    configurationShardMap.put(LogicalDatastoreType.CONFIGURATION,
                            createDatastoreClient(ClusterUtils.PREFIX_CONFIG_SHARD_ID,
                                    distributedConfigDatastore.getActorContext()));

                } catch (final DOMDataTreeShardCreationFailedException e) {
                    LOG.error("Unable to create datastoreClient for PrefixConfiguration shard.", e);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Unable to find the prefix configuration shard.", throwable);
            }
        });

        final ListenableFuture<Void> operFuture = lookupConfigShard(LogicalDatastoreType.OPERATIONAL);
        Futures.addCallback(operFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
                try {
                    configurationShardMap.put(LogicalDatastoreType.OPERATIONAL,
                            createDatastoreClient(ClusterUtils.PREFIX_CONFIG_SHARD_ID,
                                    distributedOperDatastore.getActorContext()));

                } catch (final DOMDataTreeShardCreationFailedException e) {
                    LOG.error("Unable to create datastoreClient for PrefixConfiguration shard.", e);
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Unable to create datastoreClient for PrefixConfiguration shard.", throwable);
            }
        });

        return Futures.allAsList(configFuture, operFuture);
    }

    private ListenableFuture<Void> lookupConfigShard(final LogicalDatastoreType type) {
        final SettableFuture<Void> future = SettableFuture.create();

        final FiniteDuration duration = new FiniteDuration(SHARD_FUTURE_TIMEOUT, TimeUnit.SECONDS);
        final Future<Object> ask =
                Patterns.ask(shardedDataTreeActor, new StartConfigShardLookup(type), new Timeout(duration));

        ask.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable throwable, final Object result) throws Throwable {
                if (throwable != null) {
                    future.setException(throwable);
                } else {
                    future.set(null);
                }
            }
        }, actorSystem.dispatcher());

        return future;
    }

    @Nonnull
    @Override
    public <T extends DOMDataTreeListener> ListenerRegistration<T> registerListener(
            final T listener, final Collection<DOMDataTreeIdentifier> subtrees,
            final boolean allowRxMerges, final Collection<DOMDataTreeProducer> producers)
            throws DOMDataTreeLoopException {
        return shardedDOMDataTree.registerListener(listener, subtrees, allowRxMerges, producers);
    }

    @Nonnull
    @Override
    public DOMDataTreeProducer createProducer(@Nonnull final Collection<DOMDataTreeIdentifier> subtrees) {
        LOG.debug("{} - Creating producer for {}",
                distributedConfigDatastore.getActorContext().getClusterWrapper().getCurrentMemberName(), subtrees);
        final DOMDataTreeProducer producer = shardedDOMDataTree.createProducer(subtrees);

        final Object response = distributedConfigDatastore.getActorContext()
                .executeOperation(shardedDataTreeActor, new ProducerCreated(subtrees));
        if (response == null) {
            LOG.debug("{} - Received success from remote nodes, creating producer:{}",
                    distributedConfigDatastore.getActorContext().getClusterWrapper().getCurrentMemberName(), subtrees);
            return new ProxyProducer(producer, subtrees, shardedDataTreeActor,
                    distributedConfigDatastore.getActorContext());
        } else if (response instanceof Exception) {
            closeProducer(producer);
            throw Throwables.propagate((Exception) response);
        } else {
            closeProducer(producer);
            throw new RuntimeException("Unexpected response to create producer received." + response);
        }
    }

    @Override
    public synchronized CompletionStage<DistributedShardRegistration> createDistributedShard(
            final DOMDataTreeIdentifier prefix, final Collection<MemberName> replicaMembers)
            throws DOMDataTreeShardingConflictException {
        final DOMDataTreePrefixTableEntry<DOMDataTreeShardRegistration<DOMDataTreeShard>> lookup =
                shards.lookup(prefix);
        if (lookup != null && lookup.getValue().getPrefix().equals(prefix)) {
            throw new DOMDataTreeShardingConflictException(
                    "Prefix " + prefix + " is already occupied by another shard.");
        }


        final ShardConfigWriter writer = writerMap.get(prefix.getDatastoreType());

        final CompletableFuture<DistributedShardRegistration> future = new CompletableFuture<>();
        final ListenableFuture<Void> writeFuture =
                writer.writeConfig(prefix.getRootIdentifier(), replicaMembers);

        Futures.addCallback(writeFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
                final FiniteDuration duration = new FiniteDuration(SHARD_FUTURE_TIMEOUT, TimeUnit.SECONDS);
                final Future<Object> ask =
                        Patterns.ask(shardedDataTreeActor, new LookupPrefixShard(prefix), new Timeout(duration));

                ask.onComplete(new OnComplete<Object>() {
                    @Override
                    public void onComplete(final Throwable throwable, final Object result) throws Throwable {
                        if (throwable != null) {
                            future.completeExceptionally(
                                    new DOMDataTreeShardCreationFailedException(
                                            "Unable to create a cds shard.", throwable));
                        } else {
                            final DistributedShardRegistrationImpl registration =
                                    new DistributedShardRegistrationImpl(prefix,
                                            shardedDataTreeActor, DistributedShardedDOMDataTree.this);
                            future.complete(registration);
                        }
                    }
                }, actorSystem.dispatcher());
            }

            @Override
            public void onFailure(final Throwable throwable) {
                future.completeExceptionally(
                        new DOMDataTreeShardCreationFailedException("Unable to create a cds shard.", throwable));
            }
        });

        return future;
    }

    void resolveShardAdditions(final Set<DOMDataTreeIdentifier> additions) {
        LOG.debug("Member {}: Resolving additions : {}", memberName, additions);
        final ArrayList<DOMDataTreeIdentifier> list = new ArrayList<>(additions);
        // we need to register the shards from top to bottom, so we need to atleast make sure the ordering reflects that
        Collections.sort(list, (o1, o2) -> {
            if (o1.getRootIdentifier().getPathArguments().size() < o2.getRootIdentifier().getPathArguments().size()) {
                return -1;
            } else if (o1.getRootIdentifier().getPathArguments().size()
                    == o2.getRootIdentifier().getPathArguments().size()) {
                return 0;
            } else {
                return 1;
            }
        });
        list.forEach(this::createShardFrontend);
    }

    void resolveShardRemovals(final Set<DOMDataTreeIdentifier> removals) {
        LOG.debug("Member {}: Resolving removals : {}", memberName, removals);

        // do we need to go from bottom to top?
        removals.forEach(this::despawnShardFrontend);
    }

    private void createShardFrontend(final DOMDataTreeIdentifier prefix) {
        LOG.debug("Member {}: Creating CDS shard for prefix: {}", memberName, prefix);
        final String shardName = ClusterUtils.getCleanShardName(prefix.getRootIdentifier());
        final DistributedDataStore distributedDataStore =
                prefix.getDatastoreType().equals(org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION)
                        ? distributedConfigDatastore : distributedOperDatastore;

        try (final DOMDataTreeProducer producer = localCreateProducer(Collections.singletonList(prefix))) {
            final Entry<DataStoreClient, ActorRef> entry =
                    createDatastoreClient(shardName, distributedDataStore.getActorContext());

            final DistributedShardFrontend shard =
                    new DistributedShardFrontend(distributedDataStore, entry.getKey(), prefix);

            @SuppressWarnings("unchecked")
            final DOMDataTreeShardRegistration<DOMDataTreeShard> reg =
                    (DOMDataTreeShardRegistration) shardedDOMDataTree.registerDataTreeShard(prefix, shard, producer);
            shards.store(prefix, reg);
        } catch (final DOMDataTreeShardingConflictException e) {
            LOG.error("{}: Prefix {} is already occupied by another shard",
                    distributedConfigDatastore.getActorContext().getClusterWrapper().getCurrentMemberName(), prefix, e);
        } catch (DOMDataTreeProducerException e) {
            LOG.error("Unable to close producer", e);
        } catch (DOMDataTreeShardCreationFailedException e) {
            LOG.error("Unable to create datastore client for shard {}", prefix, e);
        }
    }

    private synchronized void despawnShardFrontend(final DOMDataTreeIdentifier prefix) {
        LOG.debug("Member {}: Removing CDS shard for prefix: {}", memberName, prefix);
        final DOMDataTreePrefixTableEntry<DOMDataTreeShardRegistration<DOMDataTreeShard>> lookup =
                shards.lookup(prefix);

        if (lookup == null || !lookup.getValue().getPrefix().equals(prefix)) {
            LOG.debug("Member {}: Received despawn for non-existing CDS shard frontend, prefix: {}, ignoring..",
                    memberName, prefix);
            return;
        }

        lookup.getValue().close();
        // need to remove from our local table thats used for tracking
        shards.remove(prefix);

        final ShardConfigWriter writer = writerMap.get(prefix.getDatastoreType());
        final ListenableFuture<Void> future = writer.removeConfig(prefix.getRootIdentifier());

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                LOG.debug("{} - Succesfuly removed shard for {}", memberName, prefix);
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOG.error("Removal of shard {} from configuration failed.", prefix, throwable);
            }
        });
    }

    DOMDataTreePrefixTableEntry<DOMDataTreeShardRegistration<DOMDataTreeShard>> lookupShardFrontend(
            final DOMDataTreeIdentifier prefix) {
        return shards.lookup(prefix);

    }

    DOMDataTreeProducer localCreateProducer(final Collection<DOMDataTreeIdentifier> prefix) {
        return shardedDOMDataTree.createProducer(prefix);
    }

    @Nonnull
    @Override
    public <T extends DOMDataTreeShard> ListenerRegistration<T> registerDataTreeShard(
            @Nonnull final DOMDataTreeIdentifier prefix,
            @Nonnull final T shard,
            @Nonnull final DOMDataTreeProducer producer)
            throws DOMDataTreeShardingConflictException {

        LOG.debug("Registering shard[{}] at prefix: {}", shard, prefix);

        if (producer instanceof ProxyProducer) {
            return shardedDOMDataTree.registerDataTreeShard(prefix, shard, ((ProxyProducer) producer).delegate());
        }

        return shardedDOMDataTree.registerDataTreeShard(prefix, shard, producer);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private Entry<DataStoreClient, ActorRef> createDatastoreClient(
            final String shardName, final ActorContext actorContext)
            throws DOMDataTreeShardCreationFailedException {

        LOG.debug("Creating distributed datastore client for shard {}", shardName);
        final Props distributedDataStoreClientProps =
                SimpleDataStoreClientActor.props(memberName, "Shard-" + shardName, actorContext, shardName);

        final ActorRef clientActor = actorSystem.actorOf(distributedDataStoreClientProps);
        try {
            return new SimpleEntry<>(SimpleDataStoreClientActor
                    .getDistributedDataStoreClient(clientActor, 30, TimeUnit.SECONDS), clientActor);
        } catch (final Exception e) {
            LOG.error("Failed to get actor for {}", distributedDataStoreClientProps, e);
            clientActor.tell(PoisonPill.getInstance(), noSender());
            throw new DOMDataTreeShardCreationFailedException(
                    "Unable to create datastore client for shard{" + shardName + "}", e);
        }
    }

    private DistributedShardRegistration initDefaultShard(final LogicalDatastoreType logicalDatastoreType)
            throws ExecutionException, InterruptedException {
        final Collection<MemberName> names =
                distributedConfigDatastore.getActorContext().getConfiguration().getUniqueMemberNamesForAllShards();

        final ShardConfigWriter writer = writerMap.get(logicalDatastoreType);

        if (writer.checkDefaultIsPresent()) {
            LOG.debug("Default shard for {} is already present in the config. Possibly saved in snapshot.",
                    logicalDatastoreType);
            return new DistributedShardRegistrationImpl(
                    new DOMDataTreeIdentifier(logicalDatastoreType, YangInstanceIdentifier.EMPTY),
                    shardedDataTreeActor, this);
        } else {
            try {
                // we should probably only have one node create the default shards
                return createDistributedShard(
                        new DOMDataTreeIdentifier(logicalDatastoreType, YangInstanceIdentifier.EMPTY), names)
                        .toCompletableFuture().get();
            } catch (DOMDataTreeShardingConflictException e) {
                LOG.debug("Default shard already registered, possibly due to other node doing it faster");
                return new DistributedShardRegistrationImpl(
                        new DOMDataTreeIdentifier(logicalDatastoreType, YangInstanceIdentifier.EMPTY),
                        shardedDataTreeActor, this);
            }
        }


    }

    private static void closeProducer(final DOMDataTreeProducer producer) {
        try {
            producer.close();
        } catch (final DOMDataTreeProducerException e) {
            LOG.error("Unable to close producer", e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static ActorRef createShardedDataTreeActor(final ActorSystem actorSystem,
                                                       final ShardedDataTreeActorCreator creator,
                                                       final String shardDataTreeActorId) {
        Exception lastException = null;

        for (int i = 0; i < MAX_ACTOR_CREATION_RETRIES; i++) {
            try {
                return actorSystem.actorOf(creator.props(), shardDataTreeActorId);
            } catch (final Exception e) {
                lastException = e;
                Uninterruptibles.sleepUninterruptibly(ACTOR_RETRY_DELAY, ACTOR_RETRY_TIME_UNIT);
                LOG.debug("Could not create actor {} because of {} -"
                                + " waiting for sometime before retrying (retry count = {})",
                        shardDataTreeActorId, e.getMessage(), i);
            }
        }

        throw new IllegalStateException("Failed to create actor for ShardedDOMDataTree", lastException);
    }

    private class DistributedShardRegistrationImpl implements DistributedShardRegistration {

        private final DOMDataTreeIdentifier prefix;
        private final ActorRef shardedDataTreeActor;
        private final DistributedShardedDOMDataTree distributedShardedDOMDataTree;

        DistributedShardRegistrationImpl(final DOMDataTreeIdentifier prefix,
                                         final ActorRef shardedDataTreeActor,
                                         final DistributedShardedDOMDataTree distributedShardedDOMDataTree) {
            this.prefix = prefix;
            this.shardedDataTreeActor = shardedDataTreeActor;
            this.distributedShardedDOMDataTree = distributedShardedDOMDataTree;
        }

        @Override
        public CompletableFuture<Void> close() {
            // first despawn on the local node
            distributedShardedDOMDataTree.despawnShardFrontend(prefix);
            // update the config so the remote nodes are updated
            final Future<Object> ask =
                    Patterns.ask(shardedDataTreeActor, new PrefixShardRemovalLookup(prefix), SHARD_FUTURE_TIMEOUT);

            final CompletableFuture<Void> future = new CompletableFuture<>();

            ask.onComplete(new OnComplete<Object>() {
                @Override
                @SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION",
                        justification = "https://github.com/findbugsproject/findbugs/issues/79")
                public void onComplete(final Throwable throwable, final Object result) throws Throwable {
                    LOG.debug("CDS shard registration for: [{}] closed", prefix);
                    if (throwable != null) {
                        LOG.warn("Removal of shard at prefix: {} finished with an error {}", prefix, throwable);
                        future.completeExceptionally(throwable);
                    } else {
                        future.complete(null);
                    }
                }
            }, actorSystem.dispatcher());

            return future;
        }
    }

    private static final class ProxyProducer extends ForwardingObject implements DOMDataTreeProducer {

        private final DOMDataTreeProducer delegate;
        private final Collection<DOMDataTreeIdentifier> subtrees;
        private final ActorRef shardDataTreeActor;
        private final ActorContext actorContext;

        ProxyProducer(final DOMDataTreeProducer delegate,
                      final Collection<DOMDataTreeIdentifier> subtrees,
                      final ActorRef shardDataTreeActor,
                      final ActorContext actorContext) {
            this.delegate = Preconditions.checkNotNull(delegate);
            this.subtrees = Preconditions.checkNotNull(subtrees);
            this.shardDataTreeActor = Preconditions.checkNotNull(shardDataTreeActor);
            this.actorContext = Preconditions.checkNotNull(actorContext);
        }

        @Nonnull
        @Override
        public DOMDataTreeCursorAwareTransaction createTransaction(final boolean isolated) {
            return delegate.createTransaction(isolated);
        }

        @Nonnull
        @Override
        public DOMDataTreeProducer createProducer(@Nonnull final Collection<DOMDataTreeIdentifier> subtrees) {
            // TODO we probably don't need to distribute this on the remote nodes since once we have this producer
            // open we surely have the rights to all the subtrees.
            return delegate.createProducer(subtrees);
        }

        @Override
        public void close() throws DOMDataTreeProducerException {
            delegate.close();

            final Object o = actorContext.executeOperation(shardDataTreeActor, new ProducerRemoved(subtrees));
            if (o instanceof DOMDataTreeProducerException) {
                throw ((DOMDataTreeProducerException) o);
            } else if (o instanceof Throwable) {
                throw new DOMDataTreeProducerException("Unable to close producer", (Throwable) o);
            }
        }

        @Override
        protected DOMDataTreeProducer delegate() {
            return delegate;
        }
    }
}
