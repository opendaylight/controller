/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.Status;
import akka.actor.Status.Success;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberExited;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.MemberWeaklyUp;
import akka.cluster.ClusterEvent.ReachableMember;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.cluster.Member;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;
import akka.util.Timeout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActor;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.config.PrefixShardConfiguration;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.sharding.messages.LookupPrefixShard;
import org.opendaylight.controller.cluster.sharding.messages.NotifyProducerCreated;
import org.opendaylight.controller.cluster.sharding.messages.NotifyProducerRemoved;
import org.opendaylight.controller.cluster.sharding.messages.PrefixShardCreated;
import org.opendaylight.controller.cluster.sharding.messages.PrefixShardRemovalLookup;
import org.opendaylight.controller.cluster.sharding.messages.PrefixShardRemoved;
import org.opendaylight.controller.cluster.sharding.messages.ProducerCreated;
import org.opendaylight.controller.cluster.sharding.messages.ProducerRemoved;
import org.opendaylight.controller.cluster.sharding.messages.StartConfigShardLookup;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShard;
import org.opendaylight.mdsal.dom.broker.DOMDataTreeShardRegistration;
import org.opendaylight.mdsal.dom.spi.DOMDataTreePrefixTableEntry;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Actor that tracks currently open producers/shards on remote nodes and handles notifications of remote
 * nodes of newly open producers/shards on the local node.
 */
public class ShardedDataTreeActor extends AbstractUntypedPersistentActor {

    private static final Logger LOG = LoggerFactory.getLogger(ShardedDataTreeActor.class);

    private static final String PERSISTENCE_ID = "sharding-service-actor";
    private static final Timeout DEFAULT_ASK_TIMEOUT = new Timeout(15, TimeUnit.SECONDS);

    static final FiniteDuration SHARD_LOOKUP_TASK_INTERVAL = new FiniteDuration(1L, TimeUnit.SECONDS);

    private final DistributedShardedDOMDataTree shardingService;
    private final ActorSystem actorSystem;
    private final ClusterWrapper clusterWrapper;
    // helper actorContext used only for static calls to executeAsync etc
    // for calls that need specific actor context tied to a datastore use the one provided in the DistributedDataStore
    private final ActorUtils actorUtils;
    private final ShardingServiceAddressResolver resolver;
    private final AbstractDataStore distributedConfigDatastore;
    private final AbstractDataStore distributedOperDatastore;
    private final int lookupTaskMaxRetries;

    private final Map<DOMDataTreeIdentifier, ActorProducerRegistration> idToProducer = new HashMap<>();

    ShardedDataTreeActor(final ShardedDataTreeActorCreator builder) {
        super(builder.isBackoffSupervised());

        LOG.debug("Creating ShardedDataTreeActor on {}", builder.getClusterWrapper().getCurrentMemberName());

        shardingService = builder.getShardingService();
        actorSystem = builder.getActorSystem();
        clusterWrapper = builder.getClusterWrapper();
        distributedConfigDatastore = builder.getDistributedConfigDatastore();
        distributedOperDatastore = builder.getDistributedOperDatastore();
        lookupTaskMaxRetries = builder.getLookupTaskMaxRetries();
        actorUtils = distributedConfigDatastore.getActorUtils();
        resolver = new ShardingServiceAddressResolver(
                DistributedShardedDOMDataTree.ACTOR_ID, clusterWrapper.getCurrentMemberName());

        clusterWrapper.subscribeToMemberEvents(self());
    }

    @Override
    public void preStart() {
    }

    @Override
    protected void handleRecover(final Object message) {
        LOG.debug("Received a recover message {}", message);
    }

    @Override
    protected void handleCommand(final Object message) {
        LOG.debug("{} : Received {}", clusterWrapper.getCurrentMemberName(), message);
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
        } else if (message instanceof LookupPrefixShard) {
            onLookupPrefixShard((LookupPrefixShard) message);
        } else if (message instanceof PrefixShardRemovalLookup) {
            onPrefixShardRemovalLookup((PrefixShardRemovalLookup) message);
        } else if (message instanceof PrefixShardRemoved) {
            onPrefixShardRemoved((PrefixShardRemoved) message);
        } else if (message instanceof StartConfigShardLookup) {
            onStartConfigShardLookup((StartConfigShardLookup) message);
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

        // fastpath if we have no peers
        if (resolver.getShardingServicePeerActorAddresses().isEmpty()) {
            getSender().tell(new Status.Success(null), ActorRef.noSender());
        }

        final ActorRef sender = getSender();
        final Collection<DOMDataTreeIdentifier> subtrees = message.getSubtrees();

        final List<CompletableFuture<Object>> futures = new ArrayList<>();

        for (final String address : resolver.getShardingServicePeerActorAddresses()) {
            final ActorSelection actorSelection = actorSystem.actorSelection(address);
            futures.add(
                    FutureConverters.toJava(
                            actorUtils.executeOperationAsync(
                                    actorSelection, new NotifyProducerCreated(subtrees), DEFAULT_ASK_TIMEOUT))
                    .toCompletableFuture());
        }

        final CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[futures.size()]));

        combinedFuture
                .thenRun(() -> sender.tell(new Success(null), ActorRef.noSender()))
                .exceptionally(throwable -> {
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
                    actorUtils.executeOperationAsync(selection, new NotifyProducerRemoved(message.getSubtrees())))
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
            getSender().tell(new Status.Success(null), ActorRef.noSender());
            return;
        }

        try {
            registration.close();
            getSender().tell(new Status.Success(null), ActorRef.noSender());
        } catch (final DOMDataTreeProducerException e) {
            LOG.error("Unable to close producer", e);
            getSender().tell(new Status.Failure(e), ActorRef.noSender());
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void onLookupPrefixShard(final LookupPrefixShard message) {
        LOG.debug("Member: {}, Received LookupPrefixShard: {}", clusterWrapper.getCurrentMemberName(), message);

        final DOMDataTreeIdentifier prefix = message.getPrefix();

        final ActorUtils utils = prefix.getDatastoreType() == LogicalDatastoreType.CONFIGURATION
                        ? distributedConfigDatastore.getActorUtils() : distributedOperDatastore.getActorUtils();

        // schedule a notification task for the reply
        actorSystem.scheduler().scheduleOnce(SHARD_LOOKUP_TASK_INTERVAL,
                new ShardCreationLookupTask(actorSystem, getSender(), clusterWrapper,
                        utils, shardingService, prefix, lookupTaskMaxRetries), actorSystem.dispatcher());
    }

    private void onPrefixShardCreated(final PrefixShardCreated message) {
        LOG.debug("Member: {}, Received PrefixShardCreated: {}", clusterWrapper.getCurrentMemberName(), message);

        final PrefixShardConfiguration config = message.getConfiguration();

        shardingService.resolveShardAdditions(Collections.singleton(config.getPrefix()));
    }

    private void onPrefixShardRemovalLookup(final PrefixShardRemovalLookup message) {
        LOG.debug("Member: {}, Received PrefixShardRemovalLookup: {}", clusterWrapper.getCurrentMemberName(), message);

        final ShardRemovalLookupTask removalTask =
                new ShardRemovalLookupTask(actorSystem, getSender(),
                        actorUtils, message.getPrefix(), lookupTaskMaxRetries);

        actorSystem.scheduler().scheduleOnce(SHARD_LOOKUP_TASK_INTERVAL, removalTask, actorSystem.dispatcher());
    }

    private void onPrefixShardRemoved(final PrefixShardRemoved message) {
        LOG.debug("Received PrefixShardRemoved: {}", message);

        shardingService.resolveShardRemovals(Collections.singleton(message.getPrefix()));
    }

    private void onStartConfigShardLookup(final StartConfigShardLookup message) {
        LOG.debug("Received StartConfigShardLookup: {}", message);

        final ActorUtils context =
                message.getType().equals(LogicalDatastoreType.CONFIGURATION)
                        ? distributedConfigDatastore.getActorUtils() : distributedOperDatastore.getActorUtils();

        // schedule a notification task for the reply
        actorSystem.scheduler().scheduleOnce(SHARD_LOOKUP_TASK_INTERVAL,
                new ConfigShardLookupTask(
                        actorSystem, getSender(), context, message, lookupTaskMaxRetries),
                actorSystem.dispatcher());
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

    /**
     * Handles the lookup step of cds shard creation once the configuration is updated.
     */
    private static class ShardCreationLookupTask extends LookupTask {

        private final ActorSystem system;
        private final ActorRef replyTo;
        private final ClusterWrapper clusterWrapper;
        private final ActorUtils context;
        private final DistributedShardedDOMDataTree shardingService;
        private final DOMDataTreeIdentifier toLookup;
        private final int lookupMaxRetries;

        ShardCreationLookupTask(final ActorSystem system,
                                final ActorRef replyTo,
                                final ClusterWrapper clusterWrapper,
                                final ActorUtils context,
                                final DistributedShardedDOMDataTree shardingService,
                                final DOMDataTreeIdentifier toLookup,
                                final int lookupMaxRetries) {
            super(replyTo, lookupMaxRetries);
            this.system = system;
            this.replyTo = replyTo;
            this.clusterWrapper = clusterWrapper;
            this.context = context;
            this.shardingService = shardingService;
            this.toLookup = toLookup;
            this.lookupMaxRetries = lookupMaxRetries;
        }

        @Override
        public void run() {
            final Future<ActorRef> localShardFuture =
                    context.findLocalShardAsync(ClusterUtils.getCleanShardName(toLookup.getRootIdentifier()));

            localShardFuture.onComplete(new OnComplete<ActorRef>() {
                @Override
                public void onComplete(final Throwable throwable, final ActorRef actorRef) {
                    if (throwable != null) {
                        tryReschedule(throwable);
                    } else {
                        LOG.debug("Local backend for shard[{}] lookup successful, starting leader lookup..", toLookup);

                        system.scheduler().scheduleOnce(
                                SHARD_LOOKUP_TASK_INTERVAL,
                                new ShardLeaderLookupTask(system, replyTo, context, clusterWrapper, actorRef,
                                        shardingService, toLookup, lookupMaxRetries),
                                system.dispatcher());
                    }
                }
            }, system.dispatcher());
        }

        @Override
        void reschedule(final int retries) {
            LOG.debug("Local backend for shard[{}] not found, try: {}, rescheduling..", toLookup, retries);
            system.scheduler().scheduleOnce(
                    SHARD_LOOKUP_TASK_INTERVAL, ShardCreationLookupTask.this, system.dispatcher());
        }
    }

    /**
     * Handles the readiness step by waiting for a leader of the created shard.
     */
    private static class ShardLeaderLookupTask extends LookupTask {

        private final ActorSystem system;
        private final ActorRef replyTo;
        private final ActorUtils context;
        private final ClusterWrapper clusterWrapper;
        private final ActorRef shard;
        private final DistributedShardedDOMDataTree shardingService;
        private final DOMDataTreeIdentifier toLookup;
        private final int lookupMaxRetries;

        ShardLeaderLookupTask(final ActorSystem system,
                              final ActorRef replyTo,
                              final ActorUtils context,
                              final ClusterWrapper clusterWrapper,
                              final ActorRef shard,
                              final DistributedShardedDOMDataTree shardingService,
                              final DOMDataTreeIdentifier toLookup,
                              final int lookupMaxRetries) {
            super(replyTo, lookupMaxRetries);
            this.system = system;
            this.replyTo = replyTo;
            this.context = context;
            this.clusterWrapper = clusterWrapper;
            this.shard = shard;
            this.shardingService = shardingService;
            this.toLookup = toLookup;
            this.lookupMaxRetries = lookupMaxRetries;
        }

        @Override
        public void run() {

            final Future<Object> ask = Patterns.ask(shard, FindLeader.INSTANCE, context.getOperationTimeout());

            ask.onComplete(new OnComplete<Object>() {
                @Override
                public void onComplete(final Throwable throwable, final Object findLeaderReply) {
                    if (throwable != null) {
                        tryReschedule(throwable);
                    } else {
                        final FindLeaderReply findLeader = (FindLeaderReply) findLeaderReply;
                        final Optional<String> leaderActor = findLeader.getLeaderActor();
                        if (leaderActor.isPresent()) {
                            // leader is found, backend seems ready, check if the frontend is ready
                            LOG.debug("{} - Leader for shard[{}] backend ready, starting frontend lookup..",
                                    clusterWrapper.getCurrentMemberName(), toLookup);
                            system.scheduler().scheduleOnce(
                                    SHARD_LOOKUP_TASK_INTERVAL,
                                    new FrontendLookupTask(
                                            system, replyTo, shardingService, toLookup, lookupMaxRetries),
                                    system.dispatcher());
                        } else {
                            tryReschedule(null);
                        }
                    }
                }
            }, system.dispatcher());

        }

        @Override
        void reschedule(final int retries) {
            LOG.debug("{} - Leader for shard[{}] backend not found on try: {}, retrying..",
                    clusterWrapper.getCurrentMemberName(), toLookup, retries);
            system.scheduler().scheduleOnce(
                    SHARD_LOOKUP_TASK_INTERVAL, ShardLeaderLookupTask.this, system.dispatcher());
        }
    }

    /**
     * After backend is ready this handles the last step - checking if we have a frontend shard for the backend,
     * once this completes(which should be ready by the time the backend is created, this is just a sanity check in
     * case they race), the future for the cds shard creation is completed and the shard is ready for use.
     */
    private static final class FrontendLookupTask extends LookupTask {

        private final ActorSystem system;
        private final ActorRef replyTo;
        private final DistributedShardedDOMDataTree shardingService;
        private final DOMDataTreeIdentifier toLookup;

        FrontendLookupTask(final ActorSystem system,
                           final ActorRef replyTo,
                           final DistributedShardedDOMDataTree shardingService,
                           final DOMDataTreeIdentifier toLookup,
                           final int lookupMaxRetries) {
            super(replyTo, lookupMaxRetries);
            this.system = system;
            this.replyTo = replyTo;
            this.shardingService = shardingService;
            this.toLookup = toLookup;
        }

        @Override
        public void run() {
            final DOMDataTreePrefixTableEntry<DOMDataTreeShardRegistration<DOMDataTreeShard>> entry =
                    shardingService.lookupShardFrontend(toLookup);

            if (entry != null && tableEntryIdCheck(entry, toLookup) && entry.getValue() != null) {
                replyTo.tell(new Success(null), ActorRef.noSender());
            } else {
                tryReschedule(null);
            }
        }

        private boolean tableEntryIdCheck(final DOMDataTreePrefixTableEntry<?> entry,
                                          final DOMDataTreeIdentifier prefix) {
            if (entry == null) {
                return false;
            }

            if (YangInstanceIdentifier.empty().equals(prefix.getRootIdentifier())) {
                return true;
            }

            if (entry.getIdentifier().equals(toLookup.getRootIdentifier().getLastPathArgument())) {
                return true;
            }

            return false;
        }

        @Override
        void reschedule(final int retries) {
            LOG.debug("Frontend for shard[{}] not found on try: {}, retrying..", toLookup, retries);
            system.scheduler().scheduleOnce(
                    SHARD_LOOKUP_TASK_INTERVAL, FrontendLookupTask.this, system.dispatcher());
        }
    }

    /**
     * Task that is run once a cds shard registration is closed and completes once the backend shard is removed from the
     * configuration.
     */
    private static class ShardRemovalLookupTask extends LookupTask {

        private final ActorSystem system;
        private final ActorRef replyTo;
        private final ActorUtils context;
        private final DOMDataTreeIdentifier toLookup;

        ShardRemovalLookupTask(final ActorSystem system,
                               final ActorRef replyTo,
                               final ActorUtils context,
                               final DOMDataTreeIdentifier toLookup,
                               final int lookupMaxRetries) {
            super(replyTo, lookupMaxRetries);
            this.system = system;
            this.replyTo = replyTo;
            this.context = context;
            this.toLookup = toLookup;
        }

        @Override
        public void run() {
            final Future<ActorRef> localShardFuture =
                    context.findLocalShardAsync(ClusterUtils.getCleanShardName(toLookup.getRootIdentifier()));

            localShardFuture.onComplete(new OnComplete<ActorRef>() {
                @Override
                public void onComplete(final Throwable throwable, final ActorRef actorRef) {
                    if (throwable != null) {
                        //TODO Shouldn't we check why findLocalShard failed?
                        LOG.debug("Backend shard[{}] removal lookup successful notifying the registration future",
                                toLookup);
                        replyTo.tell(new Success(null), ActorRef.noSender());
                    } else {
                        tryReschedule(null);
                    }
                }
            }, system.dispatcher());
        }

        @Override
        void reschedule(final int retries) {
            LOG.debug("Backend shard[{}] removal lookup failed, shard is still present, try: {}, rescheduling..",
                    toLookup, retries);
            system.scheduler().scheduleOnce(
                    SHARD_LOOKUP_TASK_INTERVAL, ShardRemovalLookupTask.this, system.dispatcher());
        }
    }

    /**
     * Task for handling the lookup of the backend for the configuration shard.
     */
    private static class ConfigShardLookupTask extends LookupTask {

        private final ActorSystem system;
        private final ActorRef replyTo;
        private final ActorUtils context;

        ConfigShardLookupTask(final ActorSystem system,
                              final ActorRef replyTo,
                              final ActorUtils context,
                              final StartConfigShardLookup message,
                              final int lookupMaxRetries) {
            super(replyTo, lookupMaxRetries);
            this.system = system;
            this.replyTo = replyTo;
            this.context = context;
        }

        @Override
        void reschedule(final int retries) {
            LOG.debug("Local backend for prefix configuration shard not found, try: {}, rescheduling..", retries);
            system.scheduler().scheduleOnce(
                    SHARD_LOOKUP_TASK_INTERVAL, ConfigShardLookupTask.this, system.dispatcher());
        }

        @Override
        public void run() {
            final Optional<ActorRef> localShard =
                    context.findLocalShard(ClusterUtils.PREFIX_CONFIG_SHARD_ID);

            if (!localShard.isPresent()) {
                tryReschedule(null);
            } else {
                LOG.debug("Local backend for prefix configuration shard lookup successful");
                replyTo.tell(new Status.Success(null), ActorRef.noSender());
            }
        }
    }

    /**
     * Task for handling the readiness state of the config shard. Reports success once the leader is elected.
     */
    private static class ConfigShardReadinessTask extends LookupTask {

        private final ActorSystem system;
        private final ActorRef replyTo;
        private final ActorUtils context;
        private final ClusterWrapper clusterWrapper;
        private final ActorRef shard;

        ConfigShardReadinessTask(final ActorSystem system,
                                 final ActorRef replyTo,
                                 final ActorUtils context,
                                 final ClusterWrapper clusterWrapper,
                                 final ActorRef shard,
                                 final int lookupMaxRetries) {
            super(replyTo, lookupMaxRetries);
            this.system = system;
            this.replyTo = replyTo;
            this.context = context;
            this.clusterWrapper = clusterWrapper;
            this.shard = shard;
        }

        @Override
        void reschedule(final int retries) {
            LOG.debug("{} - Leader for config shard not found on try: {}, retrying..",
                    clusterWrapper.getCurrentMemberName(), retries);
            system.scheduler().scheduleOnce(
                    SHARD_LOOKUP_TASK_INTERVAL, ConfigShardReadinessTask.this, system.dispatcher());
        }

        @Override
        public void run() {
            final Future<Object> ask = Patterns.ask(shard, FindLeader.INSTANCE, context.getOperationTimeout());

            ask.onComplete(new OnComplete<Object>() {
                @Override
                public void onComplete(final Throwable throwable, final Object findLeaderReply) {
                    if (throwable != null) {
                        tryReschedule(throwable);
                    } else {
                        final FindLeaderReply findLeader = (FindLeaderReply) findLeaderReply;
                        final Optional<String> leaderActor = findLeader.getLeaderActor();
                        if (leaderActor.isPresent()) {
                            // leader is found, backend seems ready, check if the frontend is ready
                            LOG.debug("{} - Leader for config shard is ready. Ending lookup.",
                                    clusterWrapper.getCurrentMemberName());
                            replyTo.tell(new Status.Success(null), ActorRef.noSender());
                        } else {
                            tryReschedule(null);
                        }
                    }
                }
            }, system.dispatcher());
        }
    }

    public static class ShardedDataTreeActorCreator {

        private DistributedShardedDOMDataTree shardingService;
        private AbstractDataStore distributedConfigDatastore;
        private AbstractDataStore distributedOperDatastore;
        private ActorSystem actorSystem;
        private ClusterWrapper cluster;
        private int maxRetries;
        private boolean backoffSupervised;

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

        public ShardedDataTreeActorCreator setClusterWrapper(final ClusterWrapper clusterWrapper) {
            this.cluster = clusterWrapper;
            return this;
        }

        public ClusterWrapper getClusterWrapper() {
            return cluster;
        }

        public AbstractDataStore getDistributedConfigDatastore() {
            return distributedConfigDatastore;
        }

        public ShardedDataTreeActorCreator setDistributedConfigDatastore(
                final AbstractDataStore distributedConfigDatastore) {
            this.distributedConfigDatastore = distributedConfigDatastore;
            return this;
        }

        public AbstractDataStore getDistributedOperDatastore() {
            return distributedOperDatastore;
        }

        public ShardedDataTreeActorCreator setDistributedOperDatastore(
                final AbstractDataStore distributedOperDatastore) {
            this.distributedOperDatastore = distributedOperDatastore;
            return this;
        }

        public ShardedDataTreeActorCreator setLookupTaskMaxRetries(final int newMaxRetries) {
            this.maxRetries = newMaxRetries;
            return this;
        }

        public int getLookupTaskMaxRetries() {
            return maxRetries;
        }

        public boolean isBackoffSupervised() {
            return backoffSupervised;
        }

        public void setBackoffSupervised(final boolean backoffSupervised) {
            this.backoffSupervised = backoffSupervised;
        }

        private void verify() {
            requireNonNull(shardingService);
            requireNonNull(actorSystem);
            requireNonNull(cluster);
            requireNonNull(distributedConfigDatastore);
            requireNonNull(distributedOperDatastore);
        }

        public Props props() {
            return props(false);
        }

        public Props props(final boolean newBackoffSupervised) {
            verify();
            setBackoffSupervised(newBackoffSupervised);
            return Props.create(ShardedDataTreeActor.class, this);
        }
    }
}
