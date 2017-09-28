/*
 * Copyright (c) 2016, 2017 Cisco Systems, Inc. and others.  All rights reserved.
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
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.SimpleDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ModuleShardStrategy;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.dom.api.CDSDataTreeProducer;
import org.opendaylight.controller.cluster.dom.api.CDSShardAccess;
import org.opendaylight.controller.cluster.sharding.ShardedDataTreeActor.ShardedDataTreeActorCreator;
import org.opendaylight.controller.cluster.sharding.messages.InitConfigListener;
import org.opendaylight.controller.cluster.sharding.messages.LookupPrefixShard;
import org.opendaylight.controller.cluster.sharding.messages.PrefixShardRemovalLookup;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.producer.status.rev170718.Producers;
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

    private static final Timeout PRODUCER_TIMEOUT = new Timeout(5, TimeUnit.MINUTES);

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

    private final EnumMap<LogicalDatastoreType, Entry<DataStoreClient, ActorRef>> prefixConfigShardMap =
            new EnumMap<>(LogicalDatastoreType.class);

    private final EnumMap<LogicalDatastoreType, PrefixedShardConfigWriter> prefixConfigWriterMap =
            new EnumMap<>(LogicalDatastoreType.class);

    private final EnumMap<LogicalDatastoreType, Entry<DataStoreClient, ActorRef>> producerShardMap =
            new EnumMap<>(LogicalDatastoreType.class);

    private final EnumMap<LogicalDatastoreType, ProducerStatusWriter> producerConfigWriterMap =
            new EnumMap<>(LogicalDatastoreType.class);

    private final PrefixedShardConfigUpdateHandler shardConfigUpdateHandler;
    private final ProducerStatusUpdateHandler producerUpdateHandler;

    public DistributedShardedDOMDataTree(final ActorSystemProvider actorSystemProvider,
                                         final AbstractDataStore distributedOperDatastore,
                                         final AbstractDataStore distributedConfigDatastore) {
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
                        .setDistributedOperDatastore(distributedOperDatastore)
                        .setLookupTaskMaxRetries(LOOKUP_TASK_MAX_RETRIES),
                ACTOR_ID);

        this.memberName = distributedConfigDatastore.getActorContext().getCurrentMemberName();

        shardConfigUpdateHandler = new PrefixedShardConfigUpdateHandler(shardedDataTreeActor,
                distributedConfigDatastore.getActorContext().getCurrentMemberName());

        producerUpdateHandler = new ProducerStatusUpdateHandler(shardedDataTreeActor,
                distributedConfigDatastore.getActorContext().getCurrentMemberName());

        LOG.debug("{} - Starting prefix configuration shards", memberName);
        createPrefixConfigShard(distributedConfigDatastore);
        createPrefixConfigShard(distributedOperDatastore);

        createProducerShard(distributedConfigDatastore);
        createProducerShard(distributedOperDatastore);
    }

    private static void createPrefixConfigShard(final AbstractDataStore dataStore) {
        Configuration configuration = dataStore.getActorContext().getConfiguration();
        Collection<MemberName> memberNames = configuration.getUniqueMemberNamesForAllShards();
        CreateShard createShardMessage =
                new CreateShard(new ModuleShardConfiguration(PrefixShards.QNAME.getNamespace(),
                        "prefix-shard-configuration", ClusterUtils.PREFIX_CONFIG_SHARD_ID,
                        ModuleShardStrategy.NAME, memberNames),
                        Shard.builder(), dataStore.getActorContext().getDatastoreContext());

        dataStore.getActorContext().getShardManager().tell(createShardMessage, noSender());
    }

    private static void createProducerShard(final AbstractDataStore dataStore) {
        Configuration configuration = dataStore.getActorContext().getConfiguration();
        Collection<MemberName> memberNames = configuration.getUniqueMemberNamesForAllShards();

        final DatastoreContext context =
                DatastoreContext.newBuilderFrom(
                        dataStore.getActorContext().getDatastoreContext()).persistent(false).build();

        final CreateShard createShardMessage =
                new CreateShard(new ModuleShardConfiguration(Producers.QNAME.getNamespace(),
                        "producer-status", ClusterUtils.PRODUCER_STATUS_SHARD_ID, ModuleShardStrategy.NAME,
                        memberNames),
                        Shard.builder().datastoreContext(context), context);

        dataStore.getActorContext().getShardManager().tell(createShardMessage, noSender());
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

//        initPrefixConfigShards();

        initConfigShard(ClusterUtils.PREFIX_CONFIG_SHARD_ID, prefixConfigShardMap);
        initConfigShard(ClusterUtils.PRODUCER_STATUS_SHARD_ID, producerShardMap);

        //init writer/updateHandlers for prefix config shard
        prefixConfigWriterMap.put(LogicalDatastoreType.CONFIGURATION, new PrefixedShardConfigWriter(
                prefixConfigShardMap.get(LogicalDatastoreType.CONFIGURATION).getKey()));

        prefixConfigWriterMap.put(LogicalDatastoreType.OPERATIONAL, new PrefixedShardConfigWriter(
                prefixConfigShardMap.get(LogicalDatastoreType.OPERATIONAL).getKey()));

        //create shard registration for DEFAULT_SHARD
        try {
            initDefaultShard(LogicalDatastoreType.CONFIGURATION);
        } catch (final InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Unable to create default shard frontend for config shard", e);
        }

        try {
            initDefaultShard(LogicalDatastoreType.OPERATIONAL);
        } catch (final InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Unable to create default shard frontend for operational shard", e);
        }

        shardConfigUpdateHandler.initListener(distributedConfigDatastore, LogicalDatastoreType.CONFIGURATION);
        shardConfigUpdateHandler.initListener(distributedOperDatastore, LogicalDatastoreType.OPERATIONAL);

        distributedConfigDatastore.getActorContext().getShardManager().tell(InitConfigListener.INSTANCE, noSender());
        distributedOperDatastore.getActorContext().getShardManager().tell(InitConfigListener.INSTANCE, noSender());

        //init writer/updateHandlers for the producer shard
        producerConfigWriterMap.put(LogicalDatastoreType.CONFIGURATION, new ProducerStatusWriter(
                producerShardMap.get(LogicalDatastoreType.CONFIGURATION).getKey()));

        producerConfigWriterMap.put(LogicalDatastoreType.OPERATIONAL, new ProducerStatusWriter(
                producerShardMap.get(LogicalDatastoreType.OPERATIONAL).getKey()));

        producerUpdateHandler.initListener(distributedConfigDatastore, LogicalDatastoreType.CONFIGURATION);
        producerUpdateHandler.initListener(distributedOperDatastore, LogicalDatastoreType.OPERATIONAL);
    }

    private void initConfigShard(final String shardName,
                                 final EnumMap<LogicalDatastoreType, Entry<DataStoreClient, ActorRef>> shardMap) {

        // create our writers to the configuration
        try {
            LOG.debug("{} - starting config shard({}) lookup.", memberName, shardName);
            handleConfigShardLookup(shardName)
                    .get(SHARD_FUTURE_TIMEOUT_DURATION.length(), SHARD_FUTURE_TIMEOUT_DURATION.unit());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Backend config shards not found", e);
        }

        try {
            LOG.debug("{}: Prefix configuration shards ready - creating clients", memberName);
            shardMap.put(LogicalDatastoreType.CONFIGURATION,
                    createDatastoreClient(shardName, distributedConfigDatastore.getActorContext()));
        } catch (final DOMDataTreeShardCreationFailedException e) {
            throw new IllegalStateException(
                    "Unable to create datastoreClient for config ds configuration shard(" + shardName + ").", e);
        }

        try {
            shardMap.put(LogicalDatastoreType.OPERATIONAL,
                    createDatastoreClient(shardName, distributedOperDatastore.getActorContext()));
        } catch (final DOMDataTreeShardCreationFailedException e) {
            throw new IllegalStateException(
                    "Unable to create datastoreClient for oper ds configuration shard(" + shardName + ").", e);
        }
    }

    private ListenableFuture<List<Void>> handleConfigShardLookup(final String shardName) {

        final ListenableFuture<Void> configFuture =
                lookupConfigShard(LogicalDatastoreType.CONFIGURATION, shardName);
        final ListenableFuture<Void> operFuture =
                lookupConfigShard(LogicalDatastoreType.OPERATIONAL, shardName);

        return Futures.allAsList(configFuture, operFuture);
    }

    private ListenableFuture<Void> lookupConfigShard(final LogicalDatastoreType type, final String shardName) {
        final SettableFuture<Void> future = SettableFuture.create();

        final Future<Object> ask =
                Patterns.ask(shardedDataTreeActor,
                        new StartConfigShardLookup(type, shardName), SHARD_FUTURE_TIMEOUT);

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
        LOG.debug("{} - Creating producer for {}", memberName, subtrees);

        // TODO this needs to be an atomic exists check, with a write right after since another node could come
        // right after the exists says everything is ok and create a different producer at the same time.
        LOG.trace("Exists check for producer: {}", subtrees);
        List<ListenableFuture<Boolean>> existsFutures = new ArrayList<>();
        subtrees.forEach(subtree ->
                existsFutures.add(producerConfigWriterMap.get(subtree.getDatastoreType())
                        .checkProducerExists(subtree.getRootIdentifier())));
        final ListenableFuture<List<Boolean>> allFutures = Futures.allAsList(existsFutures);
        try {
            final List<Boolean> booleans = allFutures.get(PRODUCER_TIMEOUT.duration().length(),
                    PRODUCER_TIMEOUT.duration().unit());

            for (Boolean exists : booleans) {
                Preconditions.checkArgument(!exists, "Subtree %s is attached to another producer.", subtrees);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Unable to verify producer status.", e);
            throw new IllegalStateException("Unable to verify producer status.", e);
        }

        List<ListenableFuture<Void>> writeFutures = new ArrayList<>();
        subtrees.forEach(subtree -> {
            LOG.trace("Writing new producer().", subtree);
            writeFutures.add(producerConfigWriterMap.get(subtree.getDatastoreType())
                    .writeProducer(subtree.getRootIdentifier()));
        });

        final ListenableFuture<List<Void>> writeFuture = Futures.allAsList(writeFutures);

        try {
            writeFuture.get(PRODUCER_TIMEOUT.duration().length(),
                    PRODUCER_TIMEOUT.duration().unit());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Unable to write new producer status.", e);
            throw new IllegalStateException("Unable to write new producer status.", e);
        }

        // TODO we really need to be able to verify with the shardedDOMDataTree on ALL nodes whether
        // we can actually create this producer. We could potentially keep DOMDataTreePrefixTable in the
        // ProducerStatusWriter since right now were only checking for the specified prefix, but we need
        // to check whether theres a conflicting producer anywhere in the tree.
        final DOMDataTreeProducer delegate = shardedDOMDataTree.createProducer(subtrees);


        return new ProxyProducer(delegate, subtrees,
                distributedConfigDatastore.getActorContext(), shards, producerConfigWriterMap);
    }

    @Override
    public CompletionStage<DistributedShardRegistration> createDistributedShard(
            final DOMDataTreeIdentifier prefix, final Collection<MemberName> replicaMembers)
            throws DOMDataTreeShardingConflictException {

        synchronized (shards) {
            final DOMDataTreePrefixTableEntry<DOMDataTreeShardRegistration<DOMDataTreeShard>> lookup =
                    shards.lookup(prefix);
            if (lookup != null && lookup.getValue().getPrefix().equals(prefix)) {
                throw new DOMDataTreeShardingConflictException(
                        "Prefix " + prefix + " is already occupied by another shard.");
            }
        }

        final PrefixedShardConfigWriter writer = prefixConfigWriterMap.get(prefix.getDatastoreType());

        final ListenableFuture<Void> writeFuture =
                writer.writeConfig(prefix.getRootIdentifier(), replicaMembers);

        final Promise<DistributedShardRegistration> shardRegistrationPromise = akka.dispatch.Futures.promise();
        Futures.addCallback(writeFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable final Void result) {

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
                    createDatastoreClient(shardName, distributedDataStore.getActorContext());

            final DistributedShardFrontend shard =
                    new DistributedShardFrontend(distributedDataStore, entry.getKey(), prefix);

            final DOMDataTreeShardRegistration<DOMDataTreeShard> reg =
                    shardedDOMDataTree.registerDataTreeShard(prefix, shard, producer);

            synchronized (shards) {
                shards.store(prefix, reg);
            }

        } catch (final DOMDataTreeShardingConflictException e) {
            LOG.error("{}: Prefix {} is already occupied by another shard",
                    distributedConfigDatastore.getActorContext().getClusterWrapper().getCurrentMemberName(), prefix, e);
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

        final PrefixedShardConfigWriter writer = prefixConfigWriterMap.get(prefix.getDatastoreType());
        final ListenableFuture<Void> future = writer.removeConfig(prefix.getRootIdentifier());

        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable final Void result) {
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

        LOG.debug("{}: Creating distributed datastore client for shard {}", memberName, shardName);
        final Props distributedDataStoreClientProps =
                SimpleDataStoreClientActor.props(memberName, "Shard-" + shardName, actorContext, shardName);

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
    private void initDefaultShard(final LogicalDatastoreType logicalDatastoreType)
            throws ExecutionException, InterruptedException {

        final PrefixedShardConfigWriter writer = prefixConfigWriterMap.get(logicalDatastoreType);

        if (writer.checkDefaultIsPresent()) {
            LOG.debug("{}: Default shard for {} is already present in the config. Possibly saved in snapshot.",
                    memberName, logicalDatastoreType);
        } else {
            try {
                // Currently the default shard configuration is present in the out-of-box modules.conf and is
                // expected to be present. So look up the local default shard here and create the frontend.

                // TODO we don't have to do it for config and operational default shard separately. Just one of them
                // should be enough
                final ActorContext actorContext = logicalDatastoreType == LogicalDatastoreType.CONFIGURATION
                        ? distributedConfigDatastore.getActorContext() : distributedOperDatastore.getActorContext();

                final Optional<ActorRef> defaultLocalShardOptional =
                        actorContext.findLocalShard(ClusterUtils.getCleanShardName(YangInstanceIdentifier.EMPTY));

                if (defaultLocalShardOptional.isPresent()) {
                    LOG.debug("{}: Default shard for {} is already started, creating just frontend", memberName,
                            logicalDatastoreType);
                    createShardFrontend(new DOMDataTreeIdentifier(logicalDatastoreType, YangInstanceIdentifier.EMPTY));
                }

                // The local shard isn't present - we assume that means the local member isn't in the replica list
                // and will be dynamically created later via an explicit add-shard-replica request. This is the
                // bootstrapping mechanism to add a new node into an existing cluster. The following code to create
                // the default shard as a prefix shard is problematic in this scenario so it is commented out. Since
                // the default shard is a module-based shard by default, it makes sense to always treat it as such,
                // ie bootstrap it in the same manner as the special prefix-configuration and EOS shards.
//                final Collection<MemberName> names = distributedConfigDatastore.getActorContext().getConfiguration()
//                        .getUniqueMemberNamesForAllShards();
//                Await.result(FutureConverters.toScala(createDistributedShard(
//                        new DOMDataTreeIdentifier(logicalDatastoreType, YangInstanceIdentifier.EMPTY), names)),
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
        private final ActorContext actorContext;
        @GuardedBy("shardAccessMap")
        private final Map<DOMDataTreeIdentifier, CDSShardAccessImpl> shardAccessMap = new HashMap<>();

        // We don't have to guard access to shardTable in ProxyProducer.
        // ShardTable's entries relevant to this ProxyProducer shouldn't
        // change during producer's lifetime.
        private final DOMDataTreePrefixTable<DOMDataTreeShardRegistration<DOMDataTreeShard>> shardTable;
        private final EnumMap<LogicalDatastoreType, ProducerStatusWriter> producerConfigWriterMap;

        ProxyProducer(final DOMDataTreeProducer delegate,
                      final Collection<DOMDataTreeIdentifier> subtrees,
                      final ActorContext actorContext,
                      final DOMDataTreePrefixTable<DOMDataTreeShardRegistration<DOMDataTreeShard>> shardLayout,
                      final EnumMap<LogicalDatastoreType, ProducerStatusWriter> producerConfigWriterMap) {
            this.delegate = Preconditions.checkNotNull(delegate);
            this.subtrees = Preconditions.checkNotNull(subtrees);
            this.actorContext = Preconditions.checkNotNull(actorContext);
            this.shardTable = Preconditions.checkNotNull(shardLayout);
            this.producerConfigWriterMap = Preconditions.checkNotNull(producerConfigWriterMap);
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
        @SuppressWarnings("checkstyle:IllegalCatch")
        public void close() throws DOMDataTreeProducerException {
            LOG.debug("Closing producer().", subtrees);
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            subtrees.forEach(subtree ->
                    futures.add(producerConfigWriterMap.get(subtree.getDatastoreType())
                            .removeProducer(subtree.getRootIdentifier())));

            try {
                Futures.allAsList(futures).get(PRODUCER_TIMEOUT.duration().length(),
                        PRODUCER_TIMEOUT.duration().unit());
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Error while closing producer.", e);
            }

            delegate.close();

            synchronized (shardAccessMap) {
                shardAccessMap.values().forEach(CDSShardAccessImpl::close);
            }
        }

        @Override
        protected DOMDataTreeProducer delegate() {
            return delegate;
        }

        @Nonnull
        @Override
        public CDSShardAccess getShardAccess(@Nonnull final DOMDataTreeIdentifier subtree) {
            Preconditions.checkArgument(
                    subtrees.stream().anyMatch(dataTreeIdentifier -> dataTreeIdentifier.contains(subtree)),
                    "Subtree %s is not controlled by this producer %s", subtree, this);

            final DOMDataTreePrefixTableEntry<DOMDataTreeShardRegistration<DOMDataTreeShard>> lookup =
                    shardTable.lookup(subtree);
            Preconditions.checkState(lookup != null, "Subtree %s is not contained in any registered shard.", subtree);

            final DOMDataTreeIdentifier lookupId = lookup.getValue().getPrefix();

            synchronized (shardAccessMap) {
                if (shardAccessMap.get(lookupId) != null) {
                    return shardAccessMap.get(lookupId);
                }

                // TODO Maybe we can have static factory method and return the same instance
                // for same subtrees. But maybe it is not needed since there can be only one
                // producer attached to some subtree at a time. And also how we can close ShardAccess
                // then
                final CDSShardAccessImpl shardAccess = new CDSShardAccessImpl(lookupId, actorContext);
                shardAccessMap.put(lookupId, shardAccess);
                return shardAccess;
            }
        }
    }
}
