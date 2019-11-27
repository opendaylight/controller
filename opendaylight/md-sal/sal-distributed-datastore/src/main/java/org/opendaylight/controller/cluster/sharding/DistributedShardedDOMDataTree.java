/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static akka.actor.ActorRef.noSender;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Throwables;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ForwardingObject;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.SimpleDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ModuleShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.dom.api.CDSDataTreeProducer;
import org.opendaylight.controller.cluster.dom.api.CDSShardAccess;
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
import org.opendaylight.mdsal.dom.api.DOMDataTreeServiceExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShard;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.mdsal.dom.broker.DOMDataTreeShardRegistration;
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataTree;
import org.opendaylight.mdsal.dom.spi.DOMDataTreePrefixTable;
import org.opendaylight.mdsal.dom.spi.DOMDataTreePrefixTableEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.prefix.shard.configuration.rev170110.PrefixShards;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.shard.persistence.Persistence;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.FiniteDuration;

/**
 * A layer on top of DOMDataTreeService that distributes producer/shard registrations to remote nodes via
 * {@link ShardedDataTreeActor}. Also provides QoL method for addition of prefix based clustered shard into the system.
 */
public class DistributedShardedDOMDataTree implements DOMDataTreeService, DOMDataTreeShardingService,
        DistributedShardFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedShardedDOMDataTree.class);

    private static final int MAX_ACTOR_CREATION_RETRIES = 100;
    private static final int ACTOR_RETRY_DELAY = 100;
    private static final TimeUnit ACTOR_RETRY_TIME_UNIT = TimeUnit.MILLISECONDS;
    private static final int LOOKUP_TASK_MAX_RETRIES = 100;
    private static final boolean PERSISTENT = true;
    static final FiniteDuration SHARD_FUTURE_TIMEOUT_DURATION =
            new FiniteDuration(LOOKUP_TASK_MAX_RETRIES * LOOKUP_TASK_MAX_RETRIES * 3, TimeUnit.SECONDS);
    static final Timeout SHARD_FUTURE_TIMEOUT = new Timeout(SHARD_FUTURE_TIMEOUT_DURATION);


    static final String ACTOR_ID = "ShardedDOMDataTreeFrontend";

    private final ShardedDOMDataTree shardedDOMDataTree;
    private final ActorSystem actorSystem;
    private final AbstractDataStore distributedOperDatastore;
    private final AbstractDataStore distributedConfigDatastore;

    private final ActorRef shardedDataTreeActor;
    private final MemberName memberName;

    @GuardedBy("shards")
    private final DOMDataTreePrefixTable<DOMDataTreeShardRegistration<DOMDataTreeShard>> shards =
            DOMDataTreePrefixTable.create();

    private final EnumMap<LogicalDatastoreType, Entry<DataStoreClient, ActorRef>> configurationShardMap =
            new EnumMap<>(LogicalDatastoreType.class);

    private final EnumMap<LogicalDatastoreType, PrefixedShardConfigWriter> writerMap =
            new EnumMap<>(LogicalDatastoreType.class);

    private final PrefixedShardConfigUpdateHandler updateHandler;

    public DistributedShardedDOMDataTree(final ActorSystemProvider actorSystemProvider,
                                         final AbstractDataStore distributedOperDatastore,
                                         final AbstractDataStore distributedConfigDatastore) {
        this.actorSystem = requireNonNull(actorSystemProvider).getActorSystem();
        this.distributedOperDatastore = requireNonNull(distributedOperDatastore);
        this.distributedConfigDatastore = requireNonNull(distributedConfigDatastore);
        shardedDOMDataTree = new ShardedDOMDataTree();

        shardedDataTreeActor = createShardedDataTreeActor(actorSystem,
                new ShardedDataTreeActorCreator()
                        .setShardingService(this)
                        .setActorSystem(actorSystem)
                        .setClusterWrapper(distributedConfigDatastore.getActorUtils().getClusterWrapper())
                        .setDistributedConfigDatastore(distributedConfigDatastore)
                        .setDistributedOperDatastore(distributedOperDatastore)
                        .setLookupTaskMaxRetries(LOOKUP_TASK_MAX_RETRIES),
                ACTOR_ID);

        this.memberName = distributedConfigDatastore.getActorUtils().getCurrentMemberName();

        updateHandler = new PrefixedShardConfigUpdateHandler(shardedDataTreeActor,
                distributedConfigDatastore.getActorUtils().getCurrentMemberName());

        LOG.debug("{} - Starting prefix configuration shards", memberName);
        createPrefixConfigShard(distributedConfigDatastore);
        createPrefixConfigShard(distributedOperDatastore);
    }

    private static void createPrefixConfigShard(final AbstractDataStore dataStore) {
        Configuration configuration = dataStore.getActorUtils().getConfiguration();
        Collection<MemberName> memberNames = configuration.getUniqueMemberNamesForAllShards();
        CreateShard createShardMessage =
                new CreateShard(new ModuleShardConfiguration(PrefixShards.QNAME.getNamespace(),
                        "prefix-shard-configuration", ClusterUtils.PREFIX_CONFIG_SHARD_ID, PERSISTENT,
                        ModuleShardStrategy.NAME, memberNames), Shard.builder(),
                        dataStore.getActorUtils().getDatastoreContext());

        dataStore.getActorUtils().getShardManager().tell(createShardMessage, noSender());
    }

    /**
     * This will try to initialize prefix configuration shards upon their
     * successful start. We need to create writers to these shards, so we can
     * satisfy future {@link #createDistributedShard} and
     * {@link #resolveShardAdditions} requests and update prefix configuration
     * shards accordingly.
     *
     * <p>
     * We also need to initialize listeners on these shards, so we can react
     * on changes made on them by other cluster members or even by ourselves.
     *
     * <p>
     * Finally, we need to be sure that default shards for both operational and
     * configuration data stores are up and running and we have distributed
     * shards frontend created for them.
     *
     * <p>
     * This is intended to be invoked by blueprint as initialization method.
     */
    public void init() {
        // create our writers to the configuration
        try {
            LOG.debug("{} - starting config shard lookup.", memberName);

            // We have to wait for prefix config shards to be up and running
            // so we can create datastore clients for them
            handleConfigShardLookup().get(SHARD_FUTURE_TIMEOUT_DURATION.length(), SHARD_FUTURE_TIMEOUT_DURATION.unit());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Prefix config shards not found", e);
        }

        try {
            LOG.debug("{}: Prefix configuration shards ready - creating clients", memberName);
            configurationShardMap.put(LogicalDatastoreType.CONFIGURATION,
                    createDatastoreClient(ClusterUtils.PREFIX_CONFIG_SHARD_ID,
                            distributedConfigDatastore.getActorUtils()));
        } catch (final DOMDataTreeShardCreationFailedException e) {
            throw new IllegalStateException(
                    "Unable to create datastoreClient for config DS prefix configuration shard.", e);
        }

        try {
            configurationShardMap.put(LogicalDatastoreType.OPERATIONAL,
                    createDatastoreClient(ClusterUtils.PREFIX_CONFIG_SHARD_ID,
                            distributedOperDatastore.getActorUtils()));

        } catch (final DOMDataTreeShardCreationFailedException e) {
            throw new IllegalStateException(
                        "Unable to create datastoreClient for oper DS prefix configuration shard.", e);
        }

        writerMap.put(LogicalDatastoreType.CONFIGURATION, new PrefixedShardConfigWriter(
                configurationShardMap.get(LogicalDatastoreType.CONFIGURATION).getKey()));

        writerMap.put(LogicalDatastoreType.OPERATIONAL, new PrefixedShardConfigWriter(
                configurationShardMap.get(LogicalDatastoreType.OPERATIONAL).getKey()));

        updateHandler.initListener(distributedConfigDatastore, LogicalDatastoreType.CONFIGURATION);
        updateHandler.initListener(distributedOperDatastore, LogicalDatastoreType.OPERATIONAL);

        distributedConfigDatastore.getActorUtils().getShardManager().tell(InitConfigListener.INSTANCE, noSender());
        distributedOperDatastore.getActorUtils().getShardManager().tell(InitConfigListener.INSTANCE, noSender());


        //create shard registration for DEFAULT_SHARD
        initDefaultShard(LogicalDatastoreType.CONFIGURATION);
        initDefaultShard(LogicalDatastoreType.OPERATIONAL);
    }

    private ListenableFuture<List<Void>> handleConfigShardLookup() {

        final ListenableFuture<Void> configFuture = lookupConfigShard(LogicalDatastoreType.CONFIGURATION);
        final ListenableFuture<Void> operFuture = lookupConfigShard(LogicalDatastoreType.OPERATIONAL);

        return Futures.allAsList(configFuture, operFuture);
    }

    private ListenableFuture<Void> lookupConfigShard(final LogicalDatastoreType type) {
        final SettableFuture<Void> future = SettableFuture.create();

        final Future<Object> ask =
                Patterns.ask(shardedDataTreeActor, new StartConfigShardLookup(type), SHARD_FUTURE_TIMEOUT);

        ask.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable throwable, final Object result) {
                if (throwable != null) {
                    future.setException(throwable);
                } else {
                    future.set(null);
                }
            }
        }, actorSystem.dispatcher());

        return future;
    }

    @Override
    public <T extends DOMDataTreeListener> ListenerRegistration<T> registerListener(
            final T listener, final Collection<DOMDataTreeIdentifier> subtrees,
            final boolean allowRxMerges, final Collection<DOMDataTreeProducer> producers)
            throws DOMDataTreeLoopException {
        return shardedDOMDataTree.registerListener(listener, subtrees, allowRxMerges, producers);
    }

    @Override
    public ClassToInstanceMap<DOMDataTreeServiceExtension> getExtensions() {
        return ImmutableClassToInstanceMap.of();
    }

    @Override
    public DOMDataTreeProducer createProducer(final Collection<DOMDataTreeIdentifier> subtrees) {
        LOG.debug("{} - Creating producer for {}", memberName, subtrees);
        final DOMDataTreeProducer producer = shardedDOMDataTree.createProducer(subtrees);

        final Object response = distributedConfigDatastore.getActorUtils()
                .executeOperation(shardedDataTreeActor, new ProducerCreated(subtrees));
        if (response == null) {
            LOG.debug("{} - Received success from remote nodes, creating producer:{}", memberName, subtrees);
            return new ProxyProducer(producer, subtrees, shardedDataTreeActor,
                    distributedConfigDatastore.getActorUtils(), shards);
        }

        closeProducer(producer);

        if (response instanceof Throwable) {
            Throwables.throwIfUnchecked((Throwable) response);
            throw new RuntimeException((Throwable) response);
        }
        throw new RuntimeException("Unexpected response to create producer received." + response);
    }

    @Override
    public CompletionStage<DistributedShardRegistration> createDistributedShard(
            final DOMDataTreeIdentifier prefix, final Persistence persistence,
            final Collection<MemberName> replicaMembers) throws DOMDataTreeShardingConflictException {

        synchronized (shards) {
            final DOMDataTreePrefixTableEntry<DOMDataTreeShardRegistration<DOMDataTreeShard>> lookup =
                    shards.lookup(prefix);
            if (lookup != null && lookup.getValue().getPrefix().equals(prefix)) {
                throw new DOMDataTreeShardingConflictException(
                        "Prefix " + prefix + " is already occupied by another shard.");
            }
        }

        final PrefixedShardConfigWriter writer = writerMap.get(prefix.getDatastoreType());

        final ListenableFuture<Void> writeFuture =
                writer.writeConfig(prefix.getRootIdentifier(), persistence, replicaMembers);

        final Promise<DistributedShardRegistration> shardRegistrationPromise = akka.dispatch.Futures.promise();
        Futures.addCallback(writeFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {

                final Future<Object> ask =
                        Patterns.ask(shardedDataTreeActor, new LookupPrefixShard(prefix), SHARD_FUTURE_TIMEOUT);

                shardRegistrationPromise.completeWith(ask.transform(
                        new Mapper<Object, DistributedShardRegistration>() {
                            @Override
                            public DistributedShardRegistration apply(final Object parameter) {
                                return new DistributedShardRegistrationImpl(
                                        prefix, shardedDataTreeActor, DistributedShardedDOMDataTree.this);
                            }
                        },
                        new Mapper<Throwable, Throwable>() {
                            @Override
                            public Throwable apply(final Throwable throwable) {
                                return new DOMDataTreeShardCreationFailedException(
                                        "Unable to create a cds shard.", throwable);
                            }
                        }, actorSystem.dispatcher()));
            }

            @Override
            public void onFailure(final Throwable throwable) {
                shardRegistrationPromise.failure(
                        new DOMDataTreeShardCreationFailedException("Unable to create a cds shard.", throwable));
            }
        }, MoreExecutors.directExecutor());

        return FutureConverters.toJava(shardRegistrationPromise.future());
    }

    void resolveShardAdditions(final Set<DOMDataTreeIdentifier> additions) {
        LOG.debug("{}: Resolving additions : {}", memberName, additions);
        // we need to register the shards from top to bottom, so we need to atleast make sure the ordering reflects that
        additions
            .stream()
            .sorted(Comparator.comparingInt(o -> o.getRootIdentifier().getPathArguments().size()))
            .forEachOrdered(this::createShardFrontend);
    }

    void resolveShardRemovals(final Set<DOMDataTreeIdentifier> removals) {
        LOG.debug("{}: Resolving removals : {}", memberName, removals);

        // do we need to go from bottom to top?
        removals.forEach(this::despawnShardFrontend);
    }

    private void createShardFrontend(final DOMDataTreeIdentifier prefix) {
        LOG.debug("{}: Creating CDS shard for prefix: {}", memberName, prefix);
        final String shardName = ClusterUtils.getCleanShardName(prefix.getRootIdentifier());
        final AbstractDataStore distributedDataStore =
                prefix.getDatastoreType().equals(org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION)
                        ? distributedConfigDatastore : distributedOperDatastore;

        try (DOMDataTreeProducer producer = localCreateProducer(Collections.singletonList(prefix))) {
            final Entry<DataStoreClient, ActorRef> entry =
                    createDatastoreClient(shardName, distributedDataStore.getActorUtils());

            final DistributedShardFrontend shard =
                    new DistributedShardFrontend(distributedDataStore, entry.getKey(), prefix);

            final DOMDataTreeShardRegistration<DOMDataTreeShard> reg =
                    shardedDOMDataTree.registerDataTreeShard(prefix, shard, producer);

            synchronized (shards) {
                shards.store(prefix, reg);
            }

        } catch (final DOMDataTreeShardingConflictException e) {
            LOG.error("{}: Prefix {} is already occupied by another shard",
                    distributedConfigDatastore.getActorUtils().getClusterWrapper().getCurrentMemberName(), prefix, e);
        } catch (DOMDataTreeProducerException e) {
            LOG.error("Unable to close producer", e);
        } catch (DOMDataTreeShardCreationFailedException e) {
            LOG.error("Unable to create datastore client for shard {}", prefix, e);
        }
    }

    private void despawnShardFrontend(final DOMDataTreeIdentifier prefix) {
        LOG.debug("{}: Removing CDS shard for prefix: {}", memberName, prefix);
        final DOMDataTreePrefixTableEntry<DOMDataTreeShardRegistration<DOMDataTreeShard>> lookup;
        synchronized (shards) {
            lookup = shards.lookup(prefix);
        }

        if (lookup == null || !lookup.getValue().getPrefix().equals(prefix)) {
            LOG.debug("{}: Received despawn for non-existing CDS shard frontend, prefix: {}, ignoring..",
                    memberName, prefix);
            return;
        }

        lookup.getValue().close();
        // need to remove from our local table thats used for tracking
        synchronized (shards) {
            shards.remove(prefix);
        }

        final PrefixedShardConfigWriter writer = writerMap.get(prefix.getDatastoreType());
        final ListenableFuture<Void> future = writer.removeConfig(prefix.getRootIdentifier());

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("{} - Succesfuly removed shard for {}", memberName, prefix);
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.error("Removal of shard {} from configuration failed.", prefix, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    DOMDataTreePrefixTableEntry<DOMDataTreeShardRegistration<DOMDataTreeShard>> lookupShardFrontend(
            final DOMDataTreeIdentifier prefix) {
        synchronized (shards) {
            return shards.lookup(prefix);
        }
    }

    DOMDataTreeProducer localCreateProducer(final Collection<DOMDataTreeIdentifier> prefix) {
        return shardedDOMDataTree.createProducer(prefix);
    }

    @Override
    public <T extends DOMDataTreeShard> ListenerRegistration<T> registerDataTreeShard(
            final DOMDataTreeIdentifier prefix, final T shard, final DOMDataTreeProducer producer)
                    throws DOMDataTreeShardingConflictException {

        LOG.debug("Registering shard[{}] at prefix: {}", shard, prefix);

        if (producer instanceof ProxyProducer) {
            return shardedDOMDataTree.registerDataTreeShard(prefix, shard, ((ProxyProducer) producer).delegate());
        }

        return shardedDOMDataTree.registerDataTreeShard(prefix, shard, producer);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private Entry<DataStoreClient, ActorRef> createDatastoreClient(final String shardName, final ActorUtils actorUtils)
            throws DOMDataTreeShardCreationFailedException {

        LOG.debug("{}: Creating distributed datastore client for shard {}", memberName, shardName);
        final Props distributedDataStoreClientProps =
                SimpleDataStoreClientActor.props(memberName, "Shard-" + shardName, actorUtils, shardName);

        final ActorRef clientActor = actorSystem.actorOf(distributedDataStoreClientProps);
        try {
            return new SimpleEntry<>(SimpleDataStoreClientActor
                    .getDistributedDataStoreClient(clientActor, 30, TimeUnit.SECONDS), clientActor);
        } catch (final Exception e) {
            LOG.error("{}: Failed to get actor for {}", distributedDataStoreClientProps, memberName, e);
            clientActor.tell(PoisonPill.getInstance(), noSender());
            throw new DOMDataTreeShardCreationFailedException(
                    "Unable to create datastore client for shard{" + shardName + "}", e);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void initDefaultShard(final LogicalDatastoreType logicalDatastoreType) {

        final PrefixedShardConfigWriter writer = writerMap.get(logicalDatastoreType);

        if (writer.checkDefaultIsPresent()) {
            LOG.debug("{}: Default shard for {} is already present in the config. Possibly saved in snapshot.",
                    memberName, logicalDatastoreType);
        } else {
            try {
                // Currently the default shard configuration is present in the out-of-box modules.conf and is
                // expected to be present. So look up the local default shard here and create the frontend.

                // TODO we don't have to do it for config and operational default shard separately. Just one of them
                // should be enough
                final ActorUtils actorUtils = logicalDatastoreType == LogicalDatastoreType.CONFIGURATION
                        ? distributedConfigDatastore.getActorUtils() : distributedOperDatastore.getActorUtils();

                final Optional<ActorRef> defaultLocalShardOptional =
                        actorUtils.findLocalShard(ClusterUtils.getCleanShardName(YangInstanceIdentifier.empty()));

                if (defaultLocalShardOptional.isPresent()) {
                    LOG.debug("{}: Default shard for {} is already started, creating just frontend", memberName,
                            logicalDatastoreType);
                    createShardFrontend(new DOMDataTreeIdentifier(logicalDatastoreType,
                                YangInstanceIdentifier.empty()));
                }

                // The local shard isn't present - we assume that means the local member isn't in the replica list
                // and will be dynamically created later via an explicit add-shard-replica request. This is the
                // bootstrapping mechanism to add a new node into an existing cluster. The following code to create
                // the default shard as a prefix shard is problematic in this scenario so it is commented out. Since
                // the default shard is a module-based shard by default, it makes sense to always treat it as such,
                // ie bootstrap it in the same manner as the special prefix-configuration and EOS shards.
//                final Collection<MemberName> names = distributedConfigDatastore.getActorUtils().getConfiguration()
//                        .getUniqueMemberNamesForAllShards();
//                Await.result(FutureConverters.toScala(createDistributedShard(
//                        new DOMDataTreeIdentifier(logicalDatastoreType, YangInstanceIdentifier.empty()), names)),
//                        SHARD_FUTURE_TIMEOUT_DURATION);
//            } catch (DOMDataTreeShardingConflictException e) {
//                LOG.debug("{}: Default shard for {} already registered, possibly due to other node doing it faster",
//                        memberName, logicalDatastoreType);
            } catch (Exception e) {
                LOG.error("{}: Default shard initialization for {} failed", memberName, logicalDatastoreType, e);
                throw new RuntimeException(e);
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
        public CompletionStage<Void> close() {
            // first despawn on the local node
            distributedShardedDOMDataTree.despawnShardFrontend(prefix);
            // update the config so the remote nodes are updated
            final Future<Object> ask =
                    Patterns.ask(shardedDataTreeActor, new PrefixShardRemovalLookup(prefix), SHARD_FUTURE_TIMEOUT);

            final Future<Void> closeFuture = ask.transform(
                    new Mapper<Object, Void>() {
                        @Override
                        public Void apply(final Object parameter) {
                            return null;
                        }
                    },
                    new Mapper<Throwable, Throwable>() {
                        @Override
                        public Throwable apply(final Throwable throwable) {
                            return throwable;
                        }
                    }, actorSystem.dispatcher());

            return FutureConverters.toJava(closeFuture);
        }
    }

    // TODO what about producers created by this producer?
    // They should also be CDSProducers
    private static final class ProxyProducer extends ForwardingObject implements CDSDataTreeProducer {

        private final DOMDataTreeProducer delegate;
        private final Collection<DOMDataTreeIdentifier> subtrees;
        private final ActorRef shardDataTreeActor;
        private final ActorUtils actorUtils;
        @GuardedBy("shardAccessMap")
        private final Map<DOMDataTreeIdentifier, CDSShardAccessImpl> shardAccessMap = new HashMap<>();

        // We don't have to guard access to shardTable in ProxyProducer.
        // ShardTable's entries relevant to this ProxyProducer shouldn't
        // change during producer's lifetime.
        private final DOMDataTreePrefixTable<DOMDataTreeShardRegistration<DOMDataTreeShard>> shardTable;

        ProxyProducer(final DOMDataTreeProducer delegate,
                      final Collection<DOMDataTreeIdentifier> subtrees,
                      final ActorRef shardDataTreeActor,
                      final ActorUtils actorUtils,
                      final DOMDataTreePrefixTable<DOMDataTreeShardRegistration<DOMDataTreeShard>> shardLayout) {
            this.delegate = requireNonNull(delegate);
            this.subtrees = requireNonNull(subtrees);
            this.shardDataTreeActor = requireNonNull(shardDataTreeActor);
            this.actorUtils = requireNonNull(actorUtils);
            this.shardTable = requireNonNull(shardLayout);
        }

        @Override
        public DOMDataTreeCursorAwareTransaction createTransaction(final boolean isolated) {
            return delegate.createTransaction(isolated);
        }

        @Override
        @SuppressWarnings("checkstyle:hiddenField")
        public DOMDataTreeProducer createProducer(final Collection<DOMDataTreeIdentifier> subtrees) {
            // TODO we probably don't need to distribute this on the remote nodes since once we have this producer
            // open we surely have the rights to all the subtrees.
            return delegate.createProducer(subtrees);
        }

        @Override
        @SuppressWarnings("checkstyle:IllegalCatch")
        public void close() throws DOMDataTreeProducerException {
            delegate.close();

            synchronized (shardAccessMap) {
                shardAccessMap.values().forEach(CDSShardAccessImpl::close);
            }

            final Object o = actorUtils.executeOperation(shardDataTreeActor, new ProducerRemoved(subtrees));
            if (o instanceof DOMDataTreeProducerException) {
                throw (DOMDataTreeProducerException) o;
            } else if (o instanceof Throwable) {
                throw new DOMDataTreeProducerException("Unable to close producer", (Throwable) o);
            }
        }

        @Override
        protected DOMDataTreeProducer delegate() {
            return delegate;
        }

        @Override
        public CDSShardAccess getShardAccess(final DOMDataTreeIdentifier subtree) {
            checkArgument(subtrees.stream().anyMatch(dataTreeIdentifier -> dataTreeIdentifier.contains(subtree)),
                "Subtree %s is not controlled by this producer %s", subtree, this);

            final DOMDataTreePrefixTableEntry<DOMDataTreeShardRegistration<DOMDataTreeShard>> lookup =
                    shardTable.lookup(subtree);
            checkState(lookup != null, "Subtree %s is not contained in any registered shard.", subtree);

            final DOMDataTreeIdentifier lookupId = lookup.getValue().getPrefix();

            synchronized (shardAccessMap) {
                if (shardAccessMap.get(lookupId) != null) {
                    return shardAccessMap.get(lookupId);
                }

                // TODO Maybe we can have static factory method and return the same instance
                // for same subtrees. But maybe it is not needed since there can be only one
                // producer attached to some subtree at a time. And also how we can close ShardAccess
                // then
                final CDSShardAccessImpl shardAccess = new CDSShardAccessImpl(lookupId, actorUtils);
                shardAccessMap.put(lookupId, shardAccess);
                return shardAccess;
            }
        }
    }
}
