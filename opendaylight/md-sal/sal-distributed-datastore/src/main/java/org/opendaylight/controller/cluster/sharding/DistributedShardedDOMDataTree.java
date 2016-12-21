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
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collection;
import java.util.Collections;
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

    private static final Timeout DEFAULT_ASK_TIMEOUT = new Timeout(15, TimeUnit.SECONDS);
    private static final int MAX_ACTOR_CREATION_RETRIES = 100;
    private static final int ACTOR_RETRY_DELAY = 100;
    private static final TimeUnit ACTOR_RETRY_TIME_UNIT = TimeUnit.MILLISECONDS;

    static final String ACTOR_ID = "ShardedDOMDataTreeFrontend";

    private final ShardedDOMDataTree shardedDOMDataTree;
    private final ActorSystem actorSystem;
    private final DistributedDataStore distributedOperDatastore;
    private final DistributedDataStore distributedConfigDatastore;

    private final ActorRef shardedDataTreeActor;
    private final MemberName memberName;

    public DistributedShardedDOMDataTree(final ActorSystem actorSystem,
                                         final DistributedDataStore distributedOperDatastore,
                                         final DistributedDataStore distributedConfigDatastore) {
        this.actorSystem = Preconditions.checkNotNull(actorSystem);
        this.distributedOperDatastore = Preconditions.checkNotNull(distributedOperDatastore);
        this.distributedConfigDatastore = Preconditions.checkNotNull(distributedConfigDatastore);
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
            throw new RuntimeException("Unexpected response to create producer received." + response);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public DistributedShardRegistration createDistributedShard(
            final DOMDataTreeIdentifier prefix, final Collection<MemberName> replicaMembers)
            throws DOMDataTreeShardingConflictException, DOMDataTreeProducerException,
            DOMDataTreeShardCreationFailedException {

        final String shardName = ClusterUtils.getCleanShardName(prefix.getRootIdentifier());
        final DistributedDataStore distributedDataStore =
                prefix.getDatastoreType().equals(org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION)
                        ? distributedConfigDatastore : distributedOperDatastore;

        final PrefixShardConfiguration config = new PrefixShardConfiguration(prefix, "prefix", replicaMembers);
        if (replicaMembers.contains(memberName)) {
            // spawn the backend shard and have the shard Manager create all replicas
            final ActorRef shardManager = distributedDataStore.getActorContext().getShardManager();

            shardManager.tell(new CreatePrefixedShard(config, null, Shard.builder()), noSender());
        }

        LOG.debug("Creating distributed datastore client for shard {}", shardName);
        final Props distributedDataStoreClientProps =
                SimpleDataStoreClientActor
                        .props(memberName, "Shard-" + shardName, distributedDataStore.getActorContext(), shardName);

        final ActorRef clientActor = actorSystem.actorOf(distributedDataStoreClientProps);
        final DataStoreClient client;
        try {
            client = SimpleDataStoreClientActor.getDistributedDataStoreClient(clientActor, 30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOG.error("Failed to get actor for {}", distributedDataStoreClientProps, e);
            clientActor.tell(PoisonPill.getInstance(), noSender());
            throw new DOMDataTreeProducerException("Unable to create producer", e);
        }

        // register the frontend into the sharding service and let the actor distribute this onto the other nodes
        final ListenerRegistration<ShardFrontend> shardFrontendRegistration;
        try (DOMDataTreeProducer producer = createProducer(Collections.singletonList(prefix))) {
            shardFrontendRegistration = shardedDOMDataTree
                    .registerDataTreeShard(prefix,
                            new ShardFrontend(client, prefix),
                            ((ProxyProducer) producer).delegate());
        }

        final Future<Object> future = distributedDataStore.getActorContext()
                .executeOperationAsync(shardedDataTreeActor, new PrefixShardCreated(config), DEFAULT_ASK_TIMEOUT);
        try {
            final Object result = Await.result(future, DEFAULT_ASK_TIMEOUT.duration());
            if (result != null) {
                throw new DOMDataTreeShardCreationFailedException("Received unexpected response to PrefixShardCreated"
                        + result);
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

    private static class DistributedShardRegistrationImpl implements DistributedShardRegistration {
        private final ListenerRegistration<ShardFrontend> registration;
        private final DOMDataTreeIdentifier prefix;
        private final ActorRef shardedDataTreeActor;

        DistributedShardRegistrationImpl(final ListenerRegistration<ShardFrontend> registration,
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
