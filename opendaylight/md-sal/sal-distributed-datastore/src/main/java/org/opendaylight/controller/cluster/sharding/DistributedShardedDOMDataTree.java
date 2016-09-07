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
import akka.util.Timeout;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.SimpleDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.config.PrefixShardConfiguration;
import org.opendaylight.controller.cluster.datastore.messages.CreatePrefixedShard;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.sharding.ShardedDataTreeActor.ShardedDataTreeActorCreator;
import org.opendaylight.controller.cluster.sharding.messages.PrefixShardCreated;
import org.opendaylight.controller.cluster.sharding.messages.PrefixShardRemoved;
import org.opendaylight.controller.cluster.sharding.messages.ProducerCreated;
import org.opendaylight.controller.cluster.sharding.messages.ProducerRemoved;
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
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataTree;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

/**
 * A layer on top of DOMDataTreeService that distributes producer/shard registrations to remote nodes via
 * {@link ShardedDataTreeActor}. Also provides QoL method for addition of prefix based clustered shard into the system.
 */
public class DistributedShardedDOMDataTree implements DOMDataTreeService, DOMDataTreeShardingService,
        DistributedShardFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedShardedDOMDataTree.class);
    static final String ACTOR_ID = "ShardedDOMDataTreeFrontend";
    private static final Timeout DEFAULT_ASK_TIMEOUT = new Timeout(15, TimeUnit.SECONDS);

    private final ShardedDOMDataTree shardedDOMDataTree;
    private final ActorSystem actorSystem;
    private final DistributedDataStore distributedOperDatastore;
    private final DistributedDataStore distributedConfigDatastore;

    private final ActorRef shardedDataTreeActor;
    private final MemberName memberName;

    private final EnumMap<LogicalDatastoreType, ListenerRegistration<DistributedShardFrontend>> defaultRegistrations =
            new EnumMap<>(LogicalDatastoreType.class);

    public DistributedShardedDOMDataTree(final ActorSystem actorSystem,
                                         final DistributedDataStore distributedOperDatastore,
                                         final DistributedDataStore distributedConfigDatastore) {
        this.actorSystem = actorSystem;
        this.distributedOperDatastore = distributedOperDatastore;
        this.distributedConfigDatastore = distributedConfigDatastore;
        shardedDOMDataTree = new ShardedDOMDataTree();

        shardedDataTreeActor = createShardedDataTreeActor(actorSystem,
                new ShardedDataTreeActorCreator()
                        .setDataTreeService(shardedDOMDataTree)
                        .setShardingService(shardedDOMDataTree)
                        .setActorSystem(actorSystem)
                        .setClusterWrapper(distributedConfigDatastore.getActorContext().getClusterWrapper())
                        .setDistributedConfigDatastore(distributedConfigDatastore)
                        .setDistributedOperDatastore(distributedOperDatastore),
                ACTOR_ID);

        this.memberName = distributedConfigDatastore.getActorContext().getCurrentMemberName();

        //create shard registration for DEFAULT_SHARD
        try {
            defaultRegistrations.put(LogicalDatastoreType.CONFIGURATION,
                    initDefaultShard(distributedConfigDatastore, LogicalDatastoreType.CONFIGURATION));
        } catch (final DOMDataTreeShardCreationFailedException | DOMDataTreeProducerException
                | DOMDataTreeShardingConflictException e) {
            LOG.error("Unable to create default shard frontend for config shard", e);
        }
        try {
            defaultRegistrations.put(LogicalDatastoreType.OPERATIONAL,
                    initDefaultShard(distributedOperDatastore, LogicalDatastoreType.OPERATIONAL));
        } catch (final DOMDataTreeShardCreationFailedException | DOMDataTreeProducerException
                | DOMDataTreeShardingConflictException e) {
            LOG.error("Unable to create default shard frontend for operational shard", e);
        }
    }

    @Nonnull
    @Override
    public <T extends DOMDataTreeListener> ListenerRegistration<T> registerListener(
            final T listener, final Collection<DOMDataTreeIdentifier> subtrees,
            final boolean allowRxMerges, final Collection<DOMDataTreeProducer> producers)
            throws DOMDataTreeLoopException {

        throw new UnsupportedOperationException("Not implemented");
    }

    @Nonnull
    @Override
    public DOMDataTreeProducer createProducer(@Nonnull final Collection<DOMDataTreeIdentifier> subtrees) {
        LOG.debug("Creating producer for {}", subtrees);
        final DOMDataTreeProducer producer = shardedDOMDataTree.createProducer(subtrees);

        final Object response = distributedConfigDatastore.getActorContext()
                .executeOperation(shardedDataTreeActor, new ProducerCreated(subtrees));
        if (response == null) {
            LOG.debug("Received success from remote nodes, creating producer:{}", subtrees);
            return new ProxyProducer(producer, subtrees, shardedDataTreeActor,
                    distributedConfigDatastore.getActorContext());
        } else if (response instanceof Exception) {
            closeProducer(producer);
            throw Throwables.propagate((Exception) response);
        } else {
            closeProducer(producer);
            throw new RuntimeException("Unexpected response to create producer received");
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public DistributedShardRegistration createDistributedShard(
            final DOMDataTreeIdentifier prefix, final Collection<MemberName> replicaMembers)
            throws DOMDataTreeShardingConflictException,DOMDataTreeProducerException,
            DOMDataTreeShardCreationFailedException {

        final String shardName = ClusterUtils.getCleanShardName(prefix.getRootIdentifier());
        final DistributedDataStore distributedDataStore =
                prefix.getDatastoreType().equals(org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION)
                        ? distributedConfigDatastore : distributedOperDatastore;

        final PrefixShardConfiguration config = new PrefixShardConfiguration(prefix, "prefix", replicaMembers);
        if (replicaMembers.contains(memberName)) {
            // spawn the backend shard only on this node, replicas will be handled when the configuration is distributed
            final ActorRef shardManager = distributedDataStore.getActorContext().getShardManager();

            shardManager.tell(new CreatePrefixedShard(config, null, Shard.builder()), noSender());
        }

        final Entry<DataStoreClient, ActorRef> entry =
                createDatastoreClient(shardName, distributedDataStore.getActorContext());
        final DataStoreClient client = entry.getKey();
        final ActorRef clientActor = entry.getValue();

        // register the frontend into the sharding service and let the actor distribute this onto the other nodes
        final ListenerRegistration<DistributedShardFrontend> shardFrontendRegistration;
        try (DOMDataTreeProducer producer = createProducer(Collections.singletonList(prefix))) {
            shardFrontendRegistration = shardedDOMDataTree
                    .registerDataTreeShard(prefix,
                            new DistributedShardFrontend(client, prefix),
                            ((ProxyProducer) producer).getDelegate());
        }

        final Future<Object> future = distributedDataStore.getActorContext()
                .executeOperationAsync(shardedDataTreeActor, new PrefixShardCreated(config), DEFAULT_ASK_TIMEOUT);
        try {
            final Object result = Await.result(future, DEFAULT_ASK_TIMEOUT.duration());
            if (result != null) {
                throw new IllegalStateException("Unexpected response to PrefixShardCreated" + result.toString());
            }

            return new DistributedShardRegistrationImpl(shardFrontendRegistration, prefix, shardedDataTreeActor);
        } catch (final CompletionException e) {
            shardedDataTreeActor.tell(new PrefixShardRemoved(prefix), noSender());
            clientActor.tell(PoisonPill.getInstance(), noSender());

            final Throwable cause = e.getCause();
            if (cause instanceof DOMDataTreeShardingConflictException) {
                throw (DOMDataTreeShardingConflictException) cause;
            }

            throw new DOMDataTreeShardCreationFailedException("Shard creation failed.", e.getCause());
        } catch (final Exception e) {
            shardedDataTreeActor.tell(new PrefixShardRemoved(prefix), noSender());
            clientActor.tell(PoisonPill.getInstance(), noSender());

            throw new DOMDataTreeShardCreationFailedException("Shard creation failed.", e);
        }
    }

    @Nonnull
    @Override
    public <T extends DOMDataTreeShard> ListenerRegistration<T> registerDataTreeShard(
            @Nonnull final DOMDataTreeIdentifier prefix,
            @Nonnull final T shard,
            @Nonnull final DOMDataTreeProducer producer)
            throws DOMDataTreeShardingConflictException {

        LOG.debug("Registering shard[{}] at prefix: {}", shard, prefix);

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

    private ListenerRegistration<DistributedShardFrontend> initDefaultShard(
            final DistributedDataStore distributedDataStore, final LogicalDatastoreType logicalDatastoreType)
            throws DOMDataTreeShardCreationFailedException, DOMDataTreeProducerException,
            DOMDataTreeShardingConflictException {

        final DOMDataTreeIdentifier prefix =
                new DOMDataTreeIdentifier(logicalDatastoreType, YangInstanceIdentifier.EMPTY);
        final String shardName = ClusterUtils.getCleanShardName(prefix.getRootIdentifier());

        final Entry<DataStoreClient, ActorRef> entry =
                createDatastoreClient(shardName, distributedDataStore.getActorContext());

        try (final DOMDataTreeProducer producer =
                     shardedDOMDataTree.createProducer(Collections.singletonList(prefix))) {
            return shardedDOMDataTree
                    .registerDataTreeShard(
                            prefix,
                            new DistributedShardFrontend(entry.getKey(), prefix),
                            producer);
        }
    }

    private static void closeProducer(final DOMDataTreeProducer producer) {
        try {
            producer.close();
        } catch (final DOMDataTreeProducerException e) {
            LOG.error("Unable to close producer", e);
        }
    }

    public void close() {

    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static ActorRef createShardedDataTreeActor(final ActorSystem actorSystem,
                                                       final ShardedDataTreeActorCreator creator,
                                                       final String shardDataTreeActorId) {
        Exception lastException = null;

        for (int i = 0; i < 100; i++) {
            try {
                return actorSystem.actorOf(creator.props(), shardDataTreeActorId);
            } catch (final Exception e) {
                lastException = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                LOG.debug("Could not create actor {} because of {} -"
                                + " waiting for sometime before retrying (retry count = {})",
                        shardDataTreeActorId, e.getMessage(), i);
            }
        }

        throw new IllegalStateException("Failed to create actor for ShardedDOMDataTree", lastException);
    }

    private static class DistributedShardRegistrationImpl implements DistributedShardRegistration {

        private final ListenerRegistration<DistributedShardFrontend> registration;
        private final DOMDataTreeIdentifier prefix;
        private final ActorRef shardedDataTreeActor;

        DistributedShardRegistrationImpl(final ListenerRegistration<DistributedShardFrontend> registration,
                                         final DOMDataTreeIdentifier prefix,
                                         final ActorRef shardedDataTreeActor) {
            this.registration = registration;
            this.prefix = prefix;
            this.shardedDataTreeActor = shardedDataTreeActor;
        }

        @Override
        public void close() {
            // TODO send the correct messages to ShardManager to destroy the shard
            // maybe we could provide replica removal mechanisms also?
            shardedDataTreeActor.tell(new PrefixShardRemoved(prefix), noSender());
            registration.close();
        }
    }

    private static class ProxyProducer implements DOMDataTreeProducer {

        private final DOMDataTreeProducer delegate;
        private final Collection<DOMDataTreeIdentifier> subtrees;
        private final ActorRef shardDataTreeActor;
        private final ActorContext actorContext;

        ProxyProducer(final DOMDataTreeProducer delegate,
                             final Collection<DOMDataTreeIdentifier> subtrees,
                             final ActorRef shardDataTreeActor,
                             final ActorContext actorContext) {
            this.delegate = delegate;
            this.subtrees = subtrees;
            this.shardDataTreeActor = shardDataTreeActor;
            this.actorContext = actorContext;
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
            // open we sure have the rights to all the subtrees.
            return delegate.createProducer(subtrees);
        }

        @Override
        public void close() throws DOMDataTreeProducerException {
            final Object o = actorContext.executeOperation(shardDataTreeActor, new ProducerRemoved(subtrees));
            if (o instanceof DOMDataTreeProducerException) {
                throw ((DOMDataTreeProducerException) o);
            } else if (o instanceof Throwable) {
                throw new DOMDataTreeProducerException("Unable to close producer", (Throwable) o);
            }
            delegate.close();
        }

        DOMDataTreeProducer getDelegate() {
            return delegate;
        }
    }
}
