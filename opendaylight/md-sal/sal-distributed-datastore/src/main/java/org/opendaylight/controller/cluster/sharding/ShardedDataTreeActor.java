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
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberExited;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.MemberWeaklyUp;
import akka.cluster.ClusterEvent.ReachableMember;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.cluster.ddata.DistributedData;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.Replicator;
import akka.cluster.ddata.Replicator.Changed;
import akka.cluster.ddata.Replicator.Subscribe;
import akka.cluster.ddata.Replicator.Update;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActor;
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
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import scala.compat.java8.FutureConverters;

/**
 * Actor that tracks currently open producers/shards on remote nodes and handles notifications of remote
 * nodes of newly open producers/shards on the local node.
 */
public class ShardedDataTreeActor extends AbstractUntypedPersistentActor {

    private static final String PERSISTENCE_ID = "sharding-service-actor";
    private static final Timeout DEFAULT_ASK_TIMEOUT = new Timeout(15, TimeUnit.SECONDS);

    private final DistributedShardedDOMDataTree shardingService;
    private final ActorSystem actorSystem;
    private final ClusterWrapper clusterWrapper;
    // helper actorContext used only for static calls to executeAsync etc
    // for calls that need specific actor context tied to a datastore use the one provided in the DistributedDataStore
    private final ActorContext actorContext;
    private final ShardingServiceAddressResolver resolver;
    private final DistributedDataStore distributedConfigDatastore;
    private final DistributedDataStore distributedOperDatastore;

    private final Map<DOMDataTreeIdentifier, ActorProducerRegistration> idToProducer = new HashMap<>();
    private final Map<DOMDataTreeIdentifier, ShardFrontendRegistration> idToShardRegistration = new HashMap<>();

    private final Cluster cluster;
    private final ActorRef replicator;

    private ORMap<PrefixShardConfiguration> currentData = ORMap.create();
    private Map<DOMDataTreeIdentifier, PrefixShardConfiguration> currentConfiguration = new HashMap<>();

    ShardedDataTreeActor(final ShardedDataTreeActorCreator builder) {
        LOG.debug("Creating ShardedDataTreeActor on {}", builder.getClusterWrapper().getCurrentMemberName());

        shardingService = builder.getShardingService();
        actorSystem = builder.getActorSystem();
        clusterWrapper = builder.getClusterWrapper();
        distributedConfigDatastore = builder.getDistributedConfigDatastore();
        distributedOperDatastore = builder.getDistributedOperDatastore();
        actorContext = distributedConfigDatastore.getActorContext();
        resolver = new ShardingServiceAddressResolver(
                DistributedShardedDOMDataTree.ACTOR_ID, clusterWrapper.getCurrentMemberName());

        clusterWrapper.subscribeToMemberEvents(self());
        cluster = Cluster.get(actorSystem);

        replicator = DistributedData.get(context().system()).replicator();
    }

    @Override
    public void preStart() {
        final Subscribe<ORMap<PrefixShardConfiguration>> subscribe =
                new Subscribe<>(ClusterUtils.CONFIGURATION_KEY, self());
        replicator.tell(subscribe, noSender());
    }

    @Override
    protected void handleRecover(final Object message) throws Exception {
        LOG.debug("Received a recover message {}", message);
    }

    @Override
    protected void handleCommand(final Object message) throws Exception {
        LOG.debug("Received {}", message);
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
        } else if (message instanceof Changed) {
            onConfigChanged((Changed) message);
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

    private void onConfigChanged(final Changed<ORMap<PrefixShardConfiguration>> change) {
        LOG.debug("member : {}, Received configuration changed: {}", clusterWrapper.getCurrentMemberName(), change);

        currentData = change.dataValue();
        final Map<String, PrefixShardConfiguration> changedConfig = change.dataValue().getEntries();

        LOG.debug("Changed set {}", changedConfig);

        try {
            final Map<DOMDataTreeIdentifier, PrefixShardConfiguration> newConfig =
                    changedConfig.values().stream().collect(
                            Collectors.toMap(PrefixShardConfiguration::getPrefix, Function.identity()));
            resolveConfig(newConfig);
        } catch (final IllegalStateException e) {
            LOG.error("Failed, ", e);
        }

    }

    private void resolveConfig(final Map<DOMDataTreeIdentifier, PrefixShardConfiguration> newConfig) {

        // get the removed configurations
        final SetView<DOMDataTreeIdentifier> deleted =
                Sets.difference(currentConfiguration.keySet(), newConfig.keySet());
        shardingService.resolveShardRemovals(deleted);

        // get the added configurations
        final SetView<DOMDataTreeIdentifier> additions =
                Sets.difference(newConfig.keySet(), currentConfiguration.keySet());
        shardingService.resolveShardAdditions(additions);
        // we can ignore those that existed previously since the potential changes in replicas will be handled by
        // shard manager.

        currentConfiguration = new HashMap<>(newConfig);
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

        // fastpath if no replication is needed, since there is only one node
        if (resolver.getShardingServicePeerActorAddresses().size() == 1) {
            getSender().tell(new Status.Success(null), noSender());
        }

        final ActorRef sender = getSender();
        final Collection<DOMDataTreeIdentifier> subtrees = message.getSubtrees();

        final List<CompletableFuture<Object>> futures = new ArrayList<>();

        for (final String address : resolver.getShardingServicePeerActorAddresses()) {
            final ActorSelection actorSelection = actorSystem.actorSelection(address);
            futures.add(
                    FutureConverters.toJava(
                            actorContext.executeOperationAsync(
                                    actorSelection, new NotifyProducerCreated(subtrees), DEFAULT_ASK_TIMEOUT))
                    .toCompletableFuture());
        }

        final CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[futures.size()]));

        combinedFuture.thenRun(() -> {
            sender.tell(new Status.Success(null), noSender());
        }).exceptionally(throwable -> {
            sender.tell(new Status.Failure(throwable), self());
            return null;
        });
    }

    private void onNotifyProducerCreated(final NotifyProducerCreated message) {
        LOG.debug("Received NotifyProducerCreated: {}", message);

        final Collection<DOMDataTreeIdentifier> subtrees = message.getSubtrees();

        try {
            final ActorProducerRegistration registration =
                    new ActorProducerRegistration(shardingService.localCreateProducer(subtrees), subtrees);
            subtrees.forEach(id -> idToProducer.put(id, registration));
            sender().tell(new Status.Success(null), self());
        } catch (final IllegalArgumentException e) {
            sender().tell(new Status.Failure(e), getSelf());
        }
    }

    private void onProducerRemoved(final ProducerRemoved message) {
        LOG.debug("Received ProducerRemoved: {}", message);

        final List<CompletableFuture<Object>> futures = new ArrayList<>();

        for (final String address : resolver.getShardingServicePeerActorAddresses()) {
            final ActorSelection selection = actorSystem.actorSelection(address);

            futures.add(FutureConverters.toJava(
                    actorContext.executeOperationAsync(selection, new NotifyProducerRemoved(message.getSubtrees())))
                    .toCompletableFuture());
        }

        final CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[futures.size()]));

        final ActorRef respondTo = getSender();

        combinedFuture
                .thenRun(() -> respondTo.tell(new Status.Success(null), self()))
                .exceptionally(e -> {
                    respondTo.tell(new Status.Failure(null), self());
                    return null;
                });

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

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void onCreatePrefixShard(final CreatePrefixShard message) {
        LOG.debug("Member: {}, Received CreatePrefixShard: {}", clusterWrapper.getCurrentMemberName(), message);

        final PrefixShardConfiguration configuration = message.getConfiguration();

        final Update<ORMap<PrefixShardConfiguration>> update =
                new Update<>(ClusterUtils.CONFIGURATION_KEY, currentData, Replicator.writeLocal(),
                    map -> map.put(cluster, configuration.toDataMapKey(), configuration));

        replicator.tell(update, self());
    }

    private void onPrefixShardCreated(final PrefixShardCreated message) {
        LOG.debug("Member: {}, Received PrefixShardCreated: {}", clusterWrapper.getCurrentMemberName(), message);

        final Collection<String> addresses = resolver.getShardingServicePeerActorAddresses();
        final ActorRef sender = getSender();

        final List<CompletableFuture<Object>> futures = new ArrayList<>();

        for (final String address : addresses) {
            final ActorSelection actorSelection = actorSystem.actorSelection(address);
            futures.add(FutureConverters.toJava(actorContext.executeOperationAsync(actorSelection,
                    new CreatePrefixShard(message.getConfiguration()))).toCompletableFuture());
        }

        final CompletableFuture<Void> combinedFuture =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));

        combinedFuture.thenRun(() -> {
            sender.tell(new Status.Success(null), self());
        }).exceptionally(throwable -> {
            sender.tell(new Status.Failure(throwable), self());
            return null;
        });
    }

    private void onRemovePrefixShard(final RemovePrefixShard message) {
        LOG.debug("Member: {}, Received RemovePrefixShard: {}", clusterWrapper.getCurrentMemberName(), message);

        //TODO the removal message should have the configuration or some other way to get to the key
        final Update<ORMap<PrefixShardConfiguration>> removal =
                new Update<>(ClusterUtils.CONFIGURATION_KEY, currentData, Replicator.writeLocal(),
                    map -> map.remove(cluster, "prefix=" + message.getPrefix()));
        replicator.tell(removal, self());
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

        ActorProducerRegistration(final DOMDataTreeProducer producer,
                                  final Collection<DOMDataTreeIdentifier> subtrees) {
            this.producer = producer;
            this.subtrees = subtrees;
        }

        void close() throws DOMDataTreeProducerException {
            producer.close();
            subtrees.forEach(idToProducer::remove);
        }
    }

    private static class ShardFrontendRegistration extends
            AbstractObjectRegistration<ListenerRegistration<DistributedShardFrontend>> {

        private final ActorRef clientActor;
        private final ListenerRegistration<DistributedShardFrontend> shardRegistration;

        ShardFrontendRegistration(final ActorRef clientActor,
                                  final ListenerRegistration<DistributedShardFrontend> shardRegistration) {
            super(shardRegistration);
            this.clientActor = clientActor;
            this.shardRegistration = shardRegistration;
        }

        @Override
        protected void removeRegistration() {
            shardRegistration.close();
            clientActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }

    public static class ShardedDataTreeActorCreator {

        private DistributedShardedDOMDataTree shardingService;
        private DistributedDataStore distributedConfigDatastore;
        private DistributedDataStore distributedOperDatastore;
        private ActorSystem actorSystem;
        private ClusterWrapper cluster;

        public DistributedShardedDOMDataTree getShardingService() {
            return shardingService;
        }

        public ShardedDataTreeActorCreator setShardingService(final DistributedShardedDOMDataTree shardingService) {
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

        public ShardedDataTreeActorCreator setDistributedConfigDatastore(
                final DistributedDataStore distributedConfigDatastore) {
            this.distributedConfigDatastore = distributedConfigDatastore;
            return this;
        }

        public DistributedDataStore getDistributedOperDatastore() {
            return distributedOperDatastore;
        }

        public ShardedDataTreeActorCreator setDistributedOperDatastore(
                final DistributedDataStore distributedOperDatastore) {
            this.distributedOperDatastore = distributedOperDatastore;
            return this;
        }

        private void verify() {
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
