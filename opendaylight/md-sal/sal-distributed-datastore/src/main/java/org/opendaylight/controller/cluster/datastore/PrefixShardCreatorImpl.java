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
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.messages.CreatePrefixedShard;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataTree;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefixShardCreatorImpl implements PrefixShardCreator {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixShardCreatorImpl.class);

    private final ShardedDOMDataTree shardedDOMDataTree;
    private final DistributedDataStore distributedOperDatastore;
    private final DistributedDataStore distributedConfigDatastore;
    private final ActorSystem actorSystem;
    private final MemberName memberName;

    public PrefixShardCreatorImpl(final ShardedDOMDataTree shardedDOMDataTree,
                                  final DistributedDataStore distributedOperDatastore,
                                  final DistributedDataStore distributedConfigDatastore,
                                  final ActorSystem actorSystem) {
        this.shardedDOMDataTree = shardedDOMDataTree;
        this.distributedOperDatastore = distributedOperDatastore;
        this.distributedConfigDatastore = distributedConfigDatastore;
        this.actorSystem = actorSystem;
        this.memberName = distributedConfigDatastore.getActorContext().getCurrentMemberName();
    }

    // TODO it might be a good idea to provide an easier way to register a shard across the whole top level nodes of a model,
    // like in the static module based configuration.
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

}
