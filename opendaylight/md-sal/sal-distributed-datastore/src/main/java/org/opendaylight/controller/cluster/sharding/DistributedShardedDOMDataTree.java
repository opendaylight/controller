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
import akka.cluster.Cluster;
import akka.cluster.Member;
import akka.util.Timeout;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.config.PrefixShardConfiguration;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.sharding.ShardedDataTreeActor.ShardedDataTreeActorCreator;
import org.opendaylight.controller.cluster.sharding.messages.CreatePrefixShard;
import org.opendaylight.controller.cluster.sharding.messages.ProducerCreated;
import org.opendaylight.controller.cluster.sharding.messages.ProducerRemoved;
import org.opendaylight.controller.cluster.sharding.messages.RemovePrefixShard;
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
import org.opendaylight.mdsal.dom.broker.ShardRegistration;
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataTree;
import org.opendaylight.mdsal.dom.spi.DOMDataTreePrefixTable;
import org.opendaylight.mdsal.dom.spi.DOMDataTreePrefixTableEntry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.Iterator;
import scala.collection.immutable.SortedSet;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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

    private final DOMDataTreePrefixTable<ShardRegistration<DOMDataTreeShard>> shards = DOMDataTreePrefixTable.create();

    private final EnumMap<LogicalDatastoreType, DistributedShardRegistration> defaultShardRegistrations =
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
                        .setShardingService(this)
                        .setActorSystem(actorSystem)
                        .setClusterWrapper(distributedConfigDatastore.getActorContext().getClusterWrapper())
                        .setDistributedConfigDatastore(distributedConfigDatastore)
                        .setDistributedOperDatastore(distributedOperDatastore),
                ACTOR_ID);

        this.memberName = distributedConfigDatastore.getActorContext().getCurrentMemberName();

        //create shard registration for DEFAULT_SHARD
        try {
            defaultShardRegistrations.put(LogicalDatastoreType.CONFIGURATION,
                    initDefaultShard(LogicalDatastoreType.CONFIGURATION));
        } catch (final DOMDataTreeProducerException | DOMDataTreeShardingConflictException e) {
            LOG.error("Unable to create default shard frontend for config shard", e);
        }
        try {
            defaultShardRegistrations.put(LogicalDatastoreType.OPERATIONAL,
                    initDefaultShard(LogicalDatastoreType.OPERATIONAL));
        } catch (final DOMDataTreeProducerException | DOMDataTreeShardingConflictException e) {
            LOG.error("Unable to create default shard frontend for operational shard", e);
        }
    }

    @Nonnull
    @Override
    public <T extends DOMDataTreeListener> ListenerRegistration<T> registerListener(
            final T listener, final Collection<DOMDataTreeIdentifier> subtrees,
            final boolean allowRxMerges, final Collection<DOMDataTreeProducer> producers)
            throws DOMDataTreeLoopException {

        throw new NotImplementedException();
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
    //TODO: it would be better to block here until the message is processed by the actor
    public DistributedShardRegistration createDistributedShard(
            final DOMDataTreeIdentifier prefix, final Collection<MemberName> replicaMembers)
            throws DOMDataTreeShardingConflictException, DOMDataTreeProducerException {
        final DOMDataTreePrefixTableEntry<ShardRegistration<DOMDataTreeShard>> lookup = shards.lookup(prefix);
        if (lookup != null && lookup.getValue().getPrefix().equals(prefix)) {
            throw new DOMDataTreeShardingConflictException(
                    "Prefix " + prefix + " is already occupied by another shard.");
        }

        PrefixShardConfiguration config = new PrefixShardConfiguration(prefix, "prefix", replicaMembers);
        shardedDataTreeActor.tell(new CreatePrefixShard(config), noSender());

        return new DistributedShardRegistrationImpl(prefix, shardedDataTreeActor, this);
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
        LOG.debug("Creating CDS shard for prefix: {}", prefix);
        final String shardName = ClusterUtils.getCleanShardName(prefix.getRootIdentifier());
        final DistributedDataStore distributedDataStore =
                prefix.getDatastoreType().equals(org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION)
                        ? distributedConfigDatastore : distributedOperDatastore;

        try (final DOMDataTreeProducer producer = localCreateProducer(prefix)) {
            final Entry<DistributedDataStoreClient, ActorRef> entry =
                    createDatastoreClient(shardName, distributedDataStore.getActorContext());

            final DistributedShardFrontend shard = new DistributedShardFrontend(entry.getKey(), prefix);

            @SuppressWarnings("unchecked")
            final ShardRegistration<DOMDataTreeShard> reg =
                    (ShardRegistration) shardedDOMDataTree.registerDataTreeShard(prefix, shard, producer);
            shards.store(prefix, reg);
        } catch (final DOMDataTreeShardingConflictException e) {
            LOG.error("Prefix {} is already occupied by another shard", prefix, e);
        } catch (DOMDataTreeProducerException e) {
            LOG.error("Unable to close producer", e);
        } catch (DOMDataTreeShardCreationFailedException e) {
            LOG.error("Unable to create datastore client for shard {}", prefix, e);
        }
    }

    private void despawnShardFrontend(final DOMDataTreeIdentifier prefix) {
        LOG.debug("Removing CDS shard for prefix: {}", prefix);
        final DOMDataTreePrefixTableEntry<ShardRegistration<DOMDataTreeShard>> lookup = shards.lookup(prefix);

        if (lookup == null || !lookup.getValue().getPrefix().equals(prefix)) {
            LOG.debug("Received despawn for non-existing CDS shard frontend, prefix: {}, ignoring..", prefix);
            return;
        }

        lookup.getValue().close();
        // need to remove from our local table thats used for tracking
        shards.remove(prefix);

    }

    private DOMDataTreeProducer localCreateProducer(final DOMDataTreeIdentifier prefix) {
        return shardedDOMDataTree.createProducer(Collections.singletonList(prefix));
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
    private Entry<DistributedDataStoreClient, ActorRef> createDatastoreClient(
            final String shardName, final ActorContext actorContext)
            throws DOMDataTreeShardCreationFailedException {

        LOG.debug("Creating distributed datastore client for shard {}", shardName);
        final Props distributedDataStoreClientProps =
                DistributedDataStoreClientActor.props(memberName, "Shard-" + shardName, actorContext);

        final ActorRef clientActor = actorSystem.actorOf(distributedDataStoreClientProps);
        try {
            return new SimpleEntry<>(DistributedDataStoreClientActor
                    .getDistributedDataStoreClient(clientActor, 30, TimeUnit.SECONDS), clientActor);
        } catch (final Exception e) {
            LOG.error("Failed to get actor for {}", distributedDataStoreClientProps, e);
            clientActor.tell(PoisonPill.getInstance(), noSender());
            throw new DOMDataTreeShardCreationFailedException(
                    "Unable to create datastore client for shard{" + shardName + "}", e);
        }
    }

    private DistributedShardRegistration initDefaultShard(final LogicalDatastoreType logicalDatastoreType)
            throws DOMDataTreeProducerException, DOMDataTreeShardingConflictException {
        final SortedSet<Member> members = Cluster.get(actorSystem).state().members();
        ArrayList<MemberName> names = new ArrayList<>();
        final Iterator<Member> it = members.iterator();
        while (it.hasNext()) {
            final Member next = it.next();
            final String memberRole = next.roles().iterator().next();
            names.add(MemberName.forName(memberRole));
        }
        return createDistributedShard(
                new DOMDataTreeIdentifier(logicalDatastoreType, YangInstanceIdentifier.EMPTY), names);
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
        public void close() {
            // first despawn on the local node
            distributedShardedDOMDataTree.despawnShardFrontend(prefix);
            // update the config so the remote nodes are updated
            shardedDataTreeActor.tell(new RemovePrefixShard(prefix), noSender());
        }
    }

    private static class ProxyProducer implements DOMDataTreeProducer {

        private final DOMDataTreeProducer delegate;
        private final Collection<DOMDataTreeIdentifier> subtrees;
        private final ActorRef shardDataTreeActor;
        private ActorContext actorContext;

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
