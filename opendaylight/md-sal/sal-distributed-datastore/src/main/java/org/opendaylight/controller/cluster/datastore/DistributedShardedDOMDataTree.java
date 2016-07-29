/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.ShardedDataTreeActor.ShardedDataTreeActorCreator;
import org.opendaylight.controller.cluster.datastore.messages.CreatePrefixedShard;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
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

//TODO hide DOMDataTreeShardingService from the end user and only let him create shards via the helper api's
public class DistributedShardedDOMDataTree implements DOMDataTreeService, DOMDataTreeShardingService, PrefixShardCreator {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedShardedDOMDataTree.class);
    private static final String ACTOR_ID = "ShardedDOMDataTreeFrontend";

    private final ShardedDOMDataTree shardedDOMDataTree;
    private final ActorSystem actorSystem;
    private final DistributedDataStore distributedOperDatastore;
    private final DistributedDataStore distributedConfigDatastore;

    private final ActorRef shardedDataTreeActor;
    private final MemberName memberName;

    public DistributedShardedDOMDataTree(final ActorSystem actorSystem,
                                         final DistributedDataStore distributedOperDatastore,
                                         final DistributedDataStore distributedConfigDatastore) {
        this.actorSystem = actorSystem;
        this.distributedOperDatastore = distributedOperDatastore;
        this.distributedConfigDatastore = distributedConfigDatastore;
        shardedDOMDataTree = new ShardedDOMDataTree();

        shardedDataTreeActor = createShardedDataTreeActor(actorSystem, new ShardedDataTreeActorCreator()
                .setDataTreeService(this)
                .setShardingService(this), ACTOR_ID);

        this.memberName = distributedConfigDatastore.getActorContext().getCurrentMemberName();

    }

    @Nonnull
    @Override
    public <T extends DOMDataTreeListener> ListenerRegistration<T> registerListener(final T listener, final Collection<DOMDataTreeIdentifier> subtrees, final boolean allowRxMerges, final Collection<DOMDataTreeProducer> producers) throws DOMDataTreeLoopException {
        return null;
    }

    @Nonnull
    @Override
    public DOMDataTreeProducer createProducer(@Nonnull final Collection<DOMDataTreeIdentifier> subtrees) {
        return null;
    }

    @Override
    public PrefixShardRegistration createPrefixShard(final DOMDataTreeIdentifier identifier, final Collection<MemberName> replicaMembers) throws DOMDataTreeShardingConflictException, DOMDataTreeProducerException {
        final DOMDataTreeProducer producer = shardedDOMDataTree.createProducer(Collections.singletonList(identifier));

        //TODO replace the LogicalDatastoreType with the mdsal impl
        final DistributedDataStore distributedDataStore = identifier.getDatastoreType().equals(org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION) ? distributedConfigDatastore : distributedOperDatastore;

        final String shardName = ClusterUtils.getCleanShardName(identifier.getRootIdentifier());

        LOG.debug("Creating distributed datastore client for shard {}", shardName);

        final Props distributedDataStoreClientProps =
                DistributedDataStoreClientActor.props(memberName, "Shard-" + shardName, distributedDataStore.getActorContext());

        final ActorRef clientActor = actorSystem.actorOf(distributedDataStoreClientProps);
        final DistributedDataStoreClient client;
        try {
            client = DistributedDataStoreClientActor.getDistributedDataStoreClient(clientActor, 30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOG.error("Failed to get actor for {}", distributedDataStoreClientProps, e);
            clientActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
            throw Throwables.propagate(e);
        }

        final ActorRef shardManager = distributedDataStore.getActorContext().getShardManager();

        shardManager.tell(new CreatePrefixedShard(new PrefixShardConfiguration(identifier, "prefix", replicaMembers), null, Shard.builder()), ActorRef.noSender());

        final ListenerRegistration<ShardFrontend> shardFrontendRegistration = shardedDOMDataTree.registerDataTreeShard(identifier, new ShardFrontend(client, identifier), producer);
        producer.close();

        return new PrefixShardRegistrationImpl(shardFrontendRegistration);
    }

    private static class PrefixShardRegistrationImpl implements PrefixShardRegistration {

        private final ListenerRegistration<ShardFrontend> registration;

        PrefixShardRegistrationImpl(final ListenerRegistration<ShardFrontend> registration) {
            this.registration = registration;
        }

        @Override
        public void close() {
            // send the correct messages to ShardManager to destroy the shard
            // maybe we could provide replica removal mechanisms also?
            registration.close();
        }
    }

    @Nonnull
    @Override
    public <T extends DOMDataTreeShard> ListenerRegistration<T> registerDataTreeShard(@Nonnull final DOMDataTreeIdentifier prefix, @Nonnull final T shard, @Nonnull final DOMDataTreeProducer producer) throws DOMDataTreeShardingConflictException {
        return null;
    }

    private static ActorRef createShardedDataTreeActor(final ActorSystem actorSystem,
                                                       final ShardedDataTreeActorCreator creator,
                                                       final String shardDataTreeActorId) {
        Exception lastException = null;

        for (int i = 0; i < 100; i++) {
            try {
                actorSystem.actorOf(creator.props(), shardDataTreeActorId);
            } catch (final Exception e) {
                lastException = e;
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                LOG.debug("Could not create actor {} because of {} - waiting for sometime before retrying (retry count = {})",
                        shardDataTreeActorId, e.getMessage(), i);
            }
        }

        throw new IllegalStateException("Failed to create actor for ShardedDOMDataTree", lastException);
    }
}
