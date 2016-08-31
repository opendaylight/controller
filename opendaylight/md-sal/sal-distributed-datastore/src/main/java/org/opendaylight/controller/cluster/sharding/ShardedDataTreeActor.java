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
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Status;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberExited;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.MemberWeaklyUp;
import akka.cluster.ClusterEvent.ReachableMember;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActor;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClient;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClientActor;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.config.PrefixShardConfiguration;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.sharding.messages.CreatePrefixShard;
import org.opendaylight.controller.cluster.sharding.messages.NotifyProducerCreated;
import org.opendaylight.controller.cluster.sharding.messages.NotifyProducerRemoved;
import org.opendaylight.controller.cluster.sharding.messages.PrefixShardCreated;
import org.opendaylight.controller.cluster.sharding.messages.PrefixShardRemoved;
import org.opendaylight.controller.cluster.sharding.messages.ProducerCreated;
import org.opendaylight.controller.cluster.sharding.messages.ProducerRemoved;
import org.opendaylight.controller.cluster.sharding.messages.RemovePrefixShard;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import scala.compat.java8.FutureConverters;

/**
 * Actor that tracks currently open producers/shards on remote nodes and handles notifications of remote nodes of newly open
 * producers/shards on the local node.
 */
public class ShardedDataTreeActor extends AbstractUntypedPersistentActor {

    private static final String PERSISTENCE_ID = "sharding-service-actor";
    private static final Timeout DEFAULT_ASK_TIMEOUT = new Timeout(15, TimeUnit.SECONDS);

    private final DOMDataTreeService dataTreeService;
    private final DOMDataTreeShardingService shardingService;
    private final ActorSystem actorSystem;
    private final ClusterWrapper cluster;
    // helper actorContext used only for static calls to executeAsync etc
    // for calls that need specific actor context tied to a datastore use the one provided in the DistributedDataStore
    private final ActorContext actorContext;
    private final ShardingServiceAddressResolver resolver;
    private final DistributedDataStore distributedConfigDatastore;
    private final DistributedDataStore distributedOperDatastore;

    private final Map<DOMDataTreeIdentifier, ActorProducerRegistration> idToProducer = new HashMap<>();
    private final Map<DOMDataTreeIdentifier, ShardFrontendRegistration> idToShardRegistration = new HashMap<>();

    ShardedDataTreeActor(final ShardedDataTreeActorCreator builder) {
        LOG.debug("Creating ShardedDataTreeActor on {}", builder.getClusterWrapper().getCurrentMemberName());

        dataTreeService = builder.getDataTreeService();
        shardingService = builder.getShardingService();
        actorSystem = builder.getActorSystem();
        cluster = builder.getClusterWrapper();
        distributedConfigDatastore = builder.getDistributedConfigDatastore();
        distributedOperDatastore = builder.getDistributedOperDatastore();
        actorContext = distributedConfigDatastore.getActorContext();
        resolver = new ShardingServiceAddressResolver(DistributedShardedDOMDataTree.ACTOR_ID, cluster.getCurrentMemberName());

        cluster.subscribeToMemberEvents(self());
    }

    @Override
    protected void handleRecover(final Object message) throws Exception {
        LOG.warn("Received a recover message {}", message);
    }

    @Override
    protected void handleCommand(final Object message) throws Exception {
        if (message instanceof ClusterEvent.MemberUp) {
            memberUp((ClusterEvent.MemberUp) message);
        } else if (message instanceof ClusterEvent.MemberWeaklyUp) {
            memberWeaklyUp((ClusterEvent.MemberWeaklyUp) message);
        } else if (message instanceof ClusterEvent.MemberExited) {
            memberExited((ClusterEvent.MemberExited) message);
        } else if (message instanceof ClusterEvent.MemberRemoved) {
            memberRemoved((ClusterEvent.MemberRemoved) message);
        } else if (message instanceof ClusterEvent.UnreachableMember) {
            memberUnreachable((ClusterEvent.UnreachableMember) message);
        } else if (message instanceof ClusterEvent.ReachableMember) {
            memberReachable((ClusterEvent.ReachableMember) message);
        } else if (message instanceof ProducerCreated) {
            onProducerCreated((ProducerCreated) message);
        } else if (message instanceof NotifyProducerCreated) {
            onNotifyProducerCreated((NotifyProducerCreated) message);
        } else if (message instanceof ProducerRemoved) {
            onProducerRemoved((ProducerRemoved) message);
        } else if (message instanceof NotifyProducerRemoved) {
            onNotifyProducerRemoved((NotifyProducerRemoved) message);
        } else if (message instanceof PrefixShardCreated) {
            onPrefixShardCreated((PrefixShardCreated) message);
        } else if (message instanceof CreatePrefixShard) {
            onCreatePrefixShard((CreatePrefixShard) message);
        } else if (message instanceof RemovePrefixShard) {
            onRemovePrefixShard((RemovePrefixShard) message);
        } else if (message instanceof PrefixShardRemoved) {
            onPrefixShardRemoved((PrefixShardRemoved) message);
        }
    }

    @Override
    public String persistenceId() {
        return PERSISTENCE_ID;
    }

    private void memberUp(final MemberUp message) {
        final MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberUp: memberName: {}, address: {}", persistenceId(), memberName,
                message.member().address());

        resolver.addPeerAddress(memberName, message.member().address());
    }

    private void memberWeaklyUp(final MemberWeaklyUp message) {
        final MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberWeaklyUp: memberName: {}, address: {}", persistenceId(), memberName,
                message.member().address());

        resolver.addPeerAddress(memberName, message.member().address());
    }

    private void memberExited(final MemberExited message) {
        final MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberExited: memberName: {}, address: {}", persistenceId(), memberName,
                message.member().address());

        resolver.removePeerAddress(memberName);
    }

    private void memberRemoved(final MemberRemoved message) {
        final MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberRemoved: memberName: {}, address: {}", persistenceId(), memberName,
                message.member().address());

        resolver.removePeerAddress(memberName);
    }

    private void memberUnreachable(final UnreachableMember message) {
        final MemberName memberName = memberToName(message.member());
        LOG.debug("Received UnreachableMember: memberName {}, address: {}", memberName, message.member().address());

        resolver.removePeerAddress(memberName);
    }

    private void memberReachable(final ReachableMember message) {
        final MemberName memberName = memberToName(message.member());
        LOG.debug("Received ReachableMember: memberName {}, address: {}", memberName, message.member().address());

        resolver.addPeerAddress(memberName, message.member().address());
    }

    private void onProducerCreated(final ProducerCreated message) {
        LOG.debug("Received ProducerCreated: {}", message);
        final ActorRef sender = getSender();
        final Collection<DOMDataTreeIdentifier> subtrees = message.getSubtrees();

        final List<CompletableFuture<Object>> futures = new ArrayList<>();

        for (final String address : resolver.getShardingServicePeerActorAddresses()) {
            final ActorSelection actorSelection = actorSystem.actorSelection(address);
            futures.add(FutureConverters.toJava(
                    actorContext.executeOperationAsync(actorSelection, new NotifyProducerCreated(subtrees), DEFAULT_ASK_TIMEOUT)).toCompletableFuture());
        }

        final CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));

        combinedFuture.handleAsync((aVoid, throwable) -> {
            LOG.debug("Create producer futures completed");
            if (throwable == null) {
                for (final CompletableFuture<Object> future : futures) {
                    try {
                        final Object result = future.get();
                        if (result instanceof Status.Failure) {
                            sender.tell(result, self());
                            return new CompletableFuture<>().completeExceptionally(((Status.Failure) result).cause());
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        sender.tell(new Status.Failure(e), self());
                        return new CompletableFuture<>().completeExceptionally(e);
                    }
                }
                sender.tell(new Status.Success(null), noSender());
                return CompletableFuture.completedFuture(null);
            } else {
                sender.tell(new Status.Failure(throwable), self());
                return new CompletableFuture<>().completeExceptionally(throwable);
            }
        });
    }

    private void onNotifyProducerCreated(final NotifyProducerCreated message) {
        LOG.debug("Received NotifyProducerCreated: {}", message);

        final Collection<DOMDataTreeIdentifier> subtrees = message.getSubtrees();

        try {
            final ActorProducerRegistration registration = new ActorProducerRegistration(dataTreeService.createProducer(subtrees), subtrees);
            subtrees.forEach(id -> idToProducer.put(id, registration));
            sender().tell(new Status.Success(null), self());
        } catch (final IllegalArgumentException e) {
            sender().tell(new Status.Failure(e), getSelf());
        }
    }

    private void onProducerRemoved(final ProducerRemoved message) {
        LOG.debug("Received ProducerRemoved: {}", message);

        for (final String address : resolver.getShardingServicePeerActorAddresses()) {
            final ActorSelection selection = actorSystem.actorSelection(address);
            final Object o = actorContext.executeOperation(selection, new NotifyProducerRemoved(message.getSubtrees()));
            if (o != null) {
                getSender().tell(new Status.Failure((Exception) o), self());
                return;
            }
        }

        getSender().tell(new Status.Success(null), self());
    }

    private void onNotifyProducerRemoved(final NotifyProducerRemoved message) {
        LOG.debug("Received NotifyProducerRemoved: {}", message);

        final ActorProducerRegistration registration = idToProducer.remove(message.getSubtrees().iterator().next());
        if (registration == null) {
            LOG.warn("The notification contained a path on which no producer is registered, throwing away");
            getSender().tell(new Status.Success(null), noSender());
            return;
        }

        try {
            registration.close();
            getSender().tell(new Status.Success(null), noSender());
        } catch (final DOMDataTreeProducerException e) {
            LOG.error("Unable to close producer", e);
            getSender().tell(new Status.Failure(e), noSender());
        }
    }

    private void onCreatePrefixShard(final CreatePrefixShard message) {
        LOG.debug("Received CreatePrefixShard: {}", message);

        final PrefixShardConfiguration configuration = message.getConfiguration();

        final DOMDataTreeProducer producer = dataTreeService.createProducer(Collections.singleton(configuration.getPrefix()));

        final DistributedDataStore distributedDataStore = configuration.getPrefix().getDatastoreType() == LogicalDatastoreType.CONFIGURATION ? distributedConfigDatastore : distributedOperDatastore;
        final String shardName = ClusterUtils.getCleanShardName(configuration.getPrefix().getRootIdentifier());
        LOG.debug("Creating distributed datastore client for shard {}", shardName);
        final Props distributedDataStoreClientProps =
                DistributedDataStoreClientActor.props(cluster.getCurrentMemberName(), "Shard-" + shardName, distributedDataStore.getActorContext());

        final ActorRef clientActor = actorSystem.actorOf(distributedDataStoreClientProps);
        final DistributedDataStoreClient client;
        try {
            client = DistributedDataStoreClientActor.getDistributedDataStoreClient(clientActor, 30, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOG.error("Failed to get actor for {}", distributedDataStoreClientProps, e);
            clientActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
            throw Throwables.propagate(e);
        }

        try {
            final ListenerRegistration<ShardFrontend> shardFrontendRegistration =
                    shardingService.registerDataTreeShard(configuration.getPrefix(),
                            new ShardFrontend(client, configuration.getPrefix(), distributedDataStore.getActorContext()),
                            producer);
            idToShardRegistration.put(configuration.getPrefix(), new ShardFrontendRegistration(clientActor, shardFrontendRegistration));

            sender().tell(new Status.Success(null), self());
        } catch (final DOMDataTreeShardingConflictException e) {
            LOG.error("Unable to create shard", e);
            clientActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
            sender().tell(new Status.Failure(e), self());
        } finally {
            try {
                producer.close();
            } catch (final DOMDataTreeProducerException e) {
                LOG.error("Unable to close producer that was used for shard registration {}", producer, e);
            }
        }
    }

    private void onPrefixShardCreated(final PrefixShardCreated message) {
        LOG.debug("Received PrefixShardCreated: {}", message);

        final Collection<String> addresses = resolver.getShardingServicePeerActorAddresses();
        final ActorRef sender = getSender();

        final List<CompletableFuture<Object>> futures = new ArrayList<>();

        for (final String address : addresses) {
            final ActorSelection actorSelection = actorSystem.actorSelection(address);
            futures.add(FutureConverters.toJava(actorContext.executeOperationAsync(actorSelection,
                    new CreatePrefixShard(message.getConfiguration()))).toCompletableFuture());
        }

        final CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));

        combinedFuture.handleAsync((aVoid, throwable) -> {
            if (throwable == null) {
                for (final CompletableFuture<Object> future : futures) {
                    try {
                        final Object result = future.get();
                        if (result instanceof Status.Failure) {
                            sender.tell(result, self());
                            return new CompletableFuture<>().completeExceptionally(((Status.Failure) result).cause());
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        sender.tell(new Status.Failure(e), self());
                        return new CompletableFuture<>().completeExceptionally(e);
                    }
                }
                sender.tell(new Status.Success(null), self());
                return CompletableFuture.completedFuture(null);
            } else {
                sender.tell(new Status.Failure(throwable), self());
                return new CompletableFuture<>().completeExceptionally(throwable);
            }
        });
    }

    private void onRemovePrefixShard(final RemovePrefixShard message) {
        LOG.debug("Received RemovePrefixShard: {}", message);

        for (final String address : resolver.getShardingServicePeerActorAddresses()) {
            final ActorSelection selection = actorContext.actorSelection(address);
            selection.tell(new PrefixShardRemoved(message.getPrefix()), getSelf());
        }
    }

    private void onPrefixShardRemoved(final PrefixShardRemoved message) {
        LOG.debug("Received PrefixShardRemoved: {}", message);

        final ShardFrontendRegistration registration = idToShardRegistration.get(message.getPrefix());

        if (registration == null) {
            LOG.warn("Received shard removed for {}, but not shard registered at this prefix all registrations: {}",
                    message.getPrefix(), idToShardRegistration);
            return;
        }

        registration.close();
    }

    private static MemberName memberToName(final Member member) {
        return MemberName.forName(member.roles().iterator().next());
    }

    private class ActorProducerRegistration {

        private final DOMDataTreeProducer producer;
        private final Collection<DOMDataTreeIdentifier> subtrees;

        ActorProducerRegistration(final DOMDataTreeProducer producer, final Collection<DOMDataTreeIdentifier> subtrees) {
            this.producer = producer;
            this.subtrees = subtrees;
        }

        void close() throws DOMDataTreeProducerException {
            producer.close();
            subtrees.forEach(idToProducer::remove);
        }
    }

    private static class ShardFrontendRegistration {

        private final ActorRef clientActor;
        private final ListenerRegistration<ShardFrontend> shardRegistration;

        public ShardFrontendRegistration(final ActorRef clientActor, final ListenerRegistration<ShardFrontend> shardRegistration) {
            this.clientActor = clientActor;
            this.shardRegistration = shardRegistration;
        }

        public void close() {
            shardRegistration.close();
            clientActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }

    public static class ShardedDataTreeActorCreator {

        private DOMDataTreeService dataTreeService;
        private DOMDataTreeShardingService shardingService;
        private DistributedDataStore distributedConfigDatastore;
        private DistributedDataStore distributedOperDatastore;
        private ActorSystem actorSystem;
        private ClusterWrapper cluster;

        public DOMDataTreeService getDataTreeService() {
            return dataTreeService;
        }

        public ShardedDataTreeActorCreator setDataTreeService(final DOMDataTreeService dataTreeService) {
            this.dataTreeService = dataTreeService;
            return this;
        }

        public DOMDataTreeShardingService getShardingService() {
            return shardingService;
        }

        public ShardedDataTreeActorCreator setShardingService(final DOMDataTreeShardingService shardingService) {
            this.shardingService = shardingService;
            return this;
        }

        public ActorSystem getActorSystem() {
            return actorSystem;
        }

        public ShardedDataTreeActorCreator setActorSystem(final ActorSystem actorSystem) {
            this.actorSystem = actorSystem;
            return this;
        }

        public ShardedDataTreeActorCreator setClusterWrapper(final ClusterWrapper cluster) {
            this.cluster = cluster;
            return this;
        }

        public ClusterWrapper getClusterWrapper() {
            return cluster;
        }

        public DistributedDataStore getDistributedConfigDatastore() {
            return distributedConfigDatastore;
        }

        public ShardedDataTreeActorCreator setDistributedConfigDatastore(final DistributedDataStore distributedConfigDatastore) {
            this.distributedConfigDatastore = distributedConfigDatastore;
            return this;
        }

        public DistributedDataStore getDistributedOperDatastore() {
            return distributedOperDatastore;
        }

        public ShardedDataTreeActorCreator setDistributedOperDatastore(final DistributedDataStore distributedOperDatastore) {
            this.distributedOperDatastore = distributedOperDatastore;
            return this;
        }

        private void verify() {
            Preconditions.checkNotNull(dataTreeService);
            Preconditions.checkNotNull(shardingService);
            Preconditions.checkNotNull(actorSystem);
            Preconditions.checkNotNull(cluster);
            Preconditions.checkNotNull(distributedConfigDatastore);
            Preconditions.checkNotNull(distributedOperDatastore);
        }


        public Props props() {
            verify();
            return Props.create(ShardedDataTreeActor.class, this);
        }


    }
}
