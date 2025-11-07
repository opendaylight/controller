/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.SYNC;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.SettableFuture;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.actor.OneForOneStrategy;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Status;
import org.apache.pekko.actor.SupervisorStrategy;
import org.apache.pekko.cluster.ClusterEvent;
import org.apache.pekko.cluster.ClusterEvent.MemberWeaklyUp;
import org.apache.pekko.cluster.Member;
import org.apache.pekko.dispatch.CompletionStages;
import org.apache.pekko.pattern.Patterns;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.cluster.common.actor.Dispatchers.DispatcherType;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.exceptions.AlreadyExistsException;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.ActorInitialized;
import org.opendaylight.controller.cluster.datastore.messages.AddShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.ChangeShardMembersVotingStatus;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.FlipShardMembersVotingStatus;
import org.opendaylight.controller.cluster.datastore.messages.GetShardRole;
import org.opendaylight.controller.cluster.datastore.messages.GetShardRoleReply;
import org.opendaylight.controller.cluster.datastore.messages.LocalPrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.RemoteFindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.RemotePrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.RemoveShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.ShardLeaderStateChanged;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardManagerSnapshot;
import org.opendaylight.controller.cluster.datastore.utils.CompositeOnComplete;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.ChangeServersVotingStatus;
import org.opendaylight.controller.cluster.raft.messages.RemoveServer;
import org.opendaylight.controller.cluster.raft.messages.RemoveServerReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.messages.ServerRemoved;
import org.opendaylight.raft.spi.WellKnownRaftPolicy;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the shards for a data store. The ShardManager has the following jobs:
 * <ul>
 * <li> Create all the local shard replicas that belong on this cluster member
 * <li> Find the address of the local shard
 * <li> Find the primary replica for any given shard
 * <li> Monitor the cluster members and store their addresses
 * </ul>
 */
class ShardManager extends AbstractUntypedActorWithMetering {
    private static final Logger LOG = LoggerFactory.getLogger(ShardManager.class);
    @VisibleForTesting
    static final Path ODL_CLUSTER_SERVER = Path.of("odl.cluster.server");

    // Stores a mapping between a shard name and it's corresponding information
    // Shard names look like inventory, topology etc and are as specified in
    // configuration
    @VisibleForTesting
    final Map<String, ShardInformation> localShards = new HashMap<>();

    final @NonNull Path stateDir;

    // The type of a ShardManager reflects the type of the datastore itself
    // A data store could be of type config/operational
    private final String type;
    private final ClusterWrapper cluster;
    private final Configuration configuration;

    @VisibleForTesting
    final String shardDispatcherPath;

    private final Set<String> shardReplicaOperationsInProgress = new HashSet<>();
    private final Map<String, CompositeOnComplete<Boolean>> shardActorsStopping = new HashMap<>();
    private final Set<Consumer<String>> shardAvailabilityCallbacks = new HashSet<>();
    private final ShardManagerInfo shardManagerMBean;
    private final SettableFuture<Empty> readinessFuture;
    private final PrimaryShardInfoFutureCache primaryShardInfoCache;

    @VisibleForTesting
    final ShardPeerAddressResolver peerAddressResolver;

    private DatastoreContextFactory datastoreContextFactory;
    private EffectiveModelContext modelContext;
    private DatastoreSnapshot restoreFromSnapshot;
    private ShardManagerSnapshot currentSnapshot;

    private ShardManager(final String actorNameOverride, final Path stateDir,
            final AbstractShardManagerCreator<?> builder) {
        super(actorNameOverride);

        this.stateDir = stateDir.resolve(ODL_CLUSTER_SERVER);

        cluster = builder.getCluster();
        configuration = builder.getConfiguration();
        datastoreContextFactory = builder.getDatastoreContextFactory();
        type = datastoreContextFactory.getBaseDatastoreContext().getDataStoreName();
        shardDispatcherPath = DispatcherType.Shard.dispatcherPathIn(getContext().system().dispatchers());
        readinessFuture = builder.getReadinessFuture();
        primaryShardInfoCache = builder.getPrimaryShardInfoCache();
        restoreFromSnapshot = builder.getRestoreFromSnapshot();

        peerAddressResolver = new ShardPeerAddressResolver(type, cluster.getCurrentMemberName());

        // Subscribe this actor to cluster member events
        cluster.subscribeToMemberEvents(self());

        shardManagerMBean = new ShardManagerInfo(self(), cluster.getCurrentMemberName(),
                "shard-manager-" + type,
                datastoreContextFactory.getBaseDatastoreContext().getDataStoreMXBeanType());
        shardManagerMBean.registerMBean();

    }

    private ShardManager(final Path stateDir, final String possiblePersistenceId,
            final AbstractShardManagerCreator<?> builder) {
        this(possiblePersistenceId != null ? possiblePersistenceId
            : "shard-manager-" + builder.getDatastoreContextFactory().getBaseDatastoreContext().getDataStoreName(),
            stateDir, builder);
    }

    ShardManager(final Path stateDir, final AbstractShardManagerCreator<?> builder) {
        this(stateDir, builder.getDatastoreContextFactory().getBaseDatastoreContext().getShardManagerPersistenceId(),
            builder);
    }

    private String name() {
        return getActorNameOverride();
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public final ActorRef getSender() {
        return super.getSender();
    }

    @Override
    public void preStart() throws IOException {
        LOG.info("Starting ShardManager {}", name());
        Files.createDirectories(stateDir);
        final var snapshot = loadSnapshot();
        if (snapshot != null) {
            applyShardManagerSnapshot(snapshot);
        }

        LOG.info("{}: Recovery complete", name());

        if (currentSnapshot == null && restoreFromSnapshot != null) {
            final var restoreSnapshot = restoreFromSnapshot.getShardManagerSnapshot();
            if (restoreSnapshot != null) {
                LOG.debug("{}: Restoring from ShardManagerSnapshot: {}", name(), restoreSnapshot);
                applyShardManagerSnapshot(restoreSnapshot);
            }
        }

        createLocalShards();
    }

    @Override
    public void postStop() {
        LOG.info("Stopping ShardManager {}", name());
        shardManagerMBean.unregisterMBean();
    }

    @Override
    protected void handleReceive(final Object message) {
        switch (message) {
            case RemoteFindPrimary msg -> findPrimary(msg, msg.getVisitedAddresses());
            case FindPrimary msg -> findPrimary(msg, null);
            case ForwardedFindPrimary(var msg, var previousActorPaths) -> findPrimary(msg, previousActorPaths);
            case FindLocalShard msg -> findLocalShard(msg);
            case UpdateSchemaContext msg -> updateSchemaContext(msg);
            case ActorInitialized msg -> onActorInitialized(msg);
            case ClusterEvent.MemberUp msg -> memberUp(msg);
            case ClusterEvent.MemberWeaklyUp msg -> memberWeaklyUp(msg);
            case ClusterEvent.MemberExited msg -> memberExited(msg);
            case ClusterEvent.MemberRemoved msg -> memberRemoved(msg);
            case ClusterEvent.UnreachableMember msg -> memberUnreachable(msg);
            case ClusterEvent.ReachableMember msg -> memberReachable(msg);
            case DatastoreContextFactory msg -> onDatastoreContextFactory(msg);
            case RoleChangeNotification msg -> onRoleChangeNotification(msg);
            case FollowerInitialSyncUpStatus msg -> onFollowerInitialSyncStatus(msg);
            case ShardNotInitializedTimeout msg -> onShardNotInitializedTimeout(msg);
            case ShardLeaderStateChanged msg -> onLeaderStateChanged(msg);
            case SwitchShardBehavior(var shardId, var switchBehavior) -> onSwitchShardBehavior(shardId, switchBehavior);
            case CreateShard msg -> onCreateShard(msg);
            case AddShardReplica msg -> onAddShardReplica(msg);
            case ForwardedAddServerReply msg -> onAddServerReply(msg.shardInfo, msg.addServerReply, getSender(),
                msg.leaderPath, msg.removeShardOnFailure);
            case ForwardedAddServerFailure msg -> onAddServerFailure(msg.shardName, msg.failureMessage, msg.failure,
                getSender(), msg.removeShardOnFailure);
            case RemoveShardReplica msg -> onRemoveShardReplica(msg);
            case WrappedShardResponse msg -> onWrappedShardResponse(msg);
            case GetSnapshot msg -> onGetSnapshot(msg);
            case ServerRemoved msg -> onShardReplicaRemoved(msg);
            case ChangeShardMembersVotingStatus msg -> onChangeShardServersVotingStatus(msg);
            case FlipShardMembersVotingStatus msg -> onFlipShardMembersVotingStatus(msg);
            case Shutdown msg -> onShutDown();
            case GetLocalShardIds msg -> onGetLocalShardIds();
            case GetShardRole msg -> onGetShardRole(msg);
            case RunnableMessage msg -> msg.run();
            case RegisterForShardAvailabilityChanges msg -> onRegisterForShardAvailabilityChanges(msg);
            case RegisterRoleChangeListenerReply msg ->
                LOG.trace("{}: Received RegisterRoleChangeListenerReply", name());
            case ClusterEvent.MemberEvent msg ->
                LOG.trace("{}: Received other ClusterEvent.MemberEvent: {}", name(), msg);
            default -> unknownMessage(message);
        }
    }

    private void onRegisterForShardAvailabilityChanges(final RegisterForShardAvailabilityChanges message) {
        LOG.debug("{}: onRegisterForShardAvailabilityChanges: {}", name(), message);

        final var callback = message.getCallback();
        shardAvailabilityCallbacks.add(callback);

        getSender().tell(new Status.Success((Registration)
            () -> executeInSelf(() -> shardAvailabilityCallbacks.remove(callback))), self());
    }

    private void onGetShardRole(final GetShardRole message) {
        LOG.debug("{}: onGetShardRole for shard: {}", name(), message.getName());

        final String name = message.getName();

        final ShardInformation shardInformation = localShards.get(name);

        if (shardInformation == null) {
            LOG.info("{}: no shard information for {} found", name(), name);
            getSender().tell(new Status.Failure(
                    new IllegalArgumentException("Shard with name " + name + " not present.")), ActorRef.noSender());
            return;
        }

        getSender().tell(new GetShardRoleReply(shardInformation.getRole()), ActorRef.noSender());
    }

    void onShutDown() {
        final var stopFutures = new ArrayList<CompletionStage<Boolean>>(localShards.size());
        for (var info : localShards.values()) {
            if (info.getActor() != null) {
                LOG.debug("{}: Issuing gracefulStop to shard {}", name(), info.getShardId());
                stopFutures.add(Patterns.gracefulStop(info.getActor(),
                    info.getDatastoreContext().getShardRaftConfig().getElectionTimeOutInterval().multipliedBy(2),
                    Shutdown.INSTANCE));
            }
        }

        LOG.info("Shutting down ShardManager {} - waiting on {} shards", name(), stopFutures.size());

        final var dispatcher = DispatcherType.Client.dispatcherIn(context().system().dispatchers());
        CompletionStages.sequence(stopFutures, dispatcher).whenCompleteAsync((results, failure) -> {
            LOG.debug("{}: All shards shutdown - sending PoisonPill to self", name());

            self().tell(PoisonPill.getInstance(), self());

            if (failure != null) {
                LOG.warn("{}: An error occurred attempting to shut down the shards", name(), failure);
                return;
            }

            int nfailed = 0;
            for (var result : results) {
                if (!result) {
                    nfailed++;
                }
            }

            if (nfailed > 0) {
                LOG.warn("{}: {} shards did not shut down gracefully", name(), nfailed);
            }
        }, dispatcher);
    }

    private void onWrappedShardResponse(final WrappedShardResponse message) {
        if (message.getResponse() instanceof RemoveServerReply reply) {
            onRemoveServerReply(getSender(), message.getShardId(), reply, message.getLeaderPath());
        }
    }

    private void onRemoveServerReply(final ActorRef originalSender, final ShardIdentifier shardId,
            final RemoveServerReply replyMsg, final String leaderPath) {
        shardReplicaOperationsInProgress.remove(shardId.getShardName());

        LOG.debug("{}: Received {} for shard {}", name(), replyMsg, shardId.getShardName());

        if (replyMsg.getStatus() == ServerChangeStatus.OK) {
            LOG.debug("{}: Leader shard successfully removed the replica shard {}", name(),
                    shardId.getShardName());
            originalSender.tell(new Status.Success(null), self());
        } else {
            LOG.warn("{}: Leader failed to remove shard replica {} with status {}",
                    name(), shardId, replyMsg.getStatus());

            Exception failure = getServerChangeException(RemoveServer.class, replyMsg.getStatus(), leaderPath, shardId);
            originalSender.tell(new Status.Failure(failure), self());
        }
    }

    private void removeShardReplica(final RemoveShardReplica contextMessage, final String shardName,
            final String primaryPath, final ActorRef sender) {
        if (isShardReplicaOperationInProgress(shardName, sender)) {
            return;
        }

        shardReplicaOperationsInProgress.add(shardName);

        final ShardIdentifier shardId = getShardIdentifier(contextMessage.getMemberName(), shardName);

        final DatastoreContext datastoreContext = newShardDatastoreContextBuilder(shardName).build();

        //inform ShardLeader to remove this shard as a replica by sending an RemoveServer message
        LOG.debug("{}: Sending RemoveServer message to peer {} for shard {}", name(),
                primaryPath, shardId);

        final var futureObj = Patterns.ask(getContext().actorSelection(primaryPath),
                new RemoveServer(shardId.toString()), datastoreContext.getShardLeaderElectionTimeout());

        futureObj.whenCompleteAsync((response, failure) -> {
            if (failure == null) {
                self().tell(new WrappedShardResponse(shardId, response, primaryPath), sender);
                return;
            }

            shardReplicaOperationsInProgress.remove(shardName);
            LOG.debug("{}: RemoveServer request to leader {} for shard {} failed", name(), primaryPath, shardName,
                failure);

            // FAILURE
            sender.tell(new Status.Failure(new RuntimeException(
                "RemoveServer request to leader %s for shard %s failed".formatted(primaryPath, shardName), failure)),
                self());
        }, DispatcherType.Client.dispatcherIn(context().system().dispatchers()));
    }

    private void onShardReplicaRemoved(final ServerRemoved message) {
        removeShard(new ShardIdentifier.Builder().fromShardIdString(message.getServerId()).build());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void removeShard(final ShardIdentifier shardId) {
        final String shardName = shardId.getShardName();
        final ShardInformation shardInformation = localShards.remove(shardName);
        if (shardInformation == null) {
            LOG.debug("{} : Shard replica {} is not present in list", name(), shardId.toString());
            return;
        }

        final ActorRef shardActor = shardInformation.getActor();
        if (shardActor != null) {
            long timeoutInMS = Math.max(shardInformation.getDatastoreContext().getShardRaftConfig()
                    .getElectionTimeOutInterval().multipliedBy(3).toMillis(), 10000);

            LOG.debug("{} : Sending Shutdown to Shard actor {} with {} ms timeout", name(), shardActor,
                    timeoutInMS);

            final var stopFuture = Patterns.gracefulStop(shardActor, Duration.ofMillis(timeoutInMS), Shutdown.INSTANCE);

            final var onComplete = new CompositeOnComplete<Boolean>() {
                @Override
                public void accept(final Boolean result, final Throwable failure) {
                    if (failure == null) {
                        LOG.debug("{} : Successfully shut down Shard actor {}", name(), shardActor);
                    } else {
                        LOG.warn("{}: Failed to shut down Shard actor {}", name(), shardActor, failure);
                    }

                    self().tell((RunnableMessage) () -> {
                        // At any rate, invalidate primaryShardInfo cache
                        primaryShardInfoCache.remove(shardName);

                        shardActorsStopping.remove(shardName);
                        notifyOnCompleteTasks(failure, result);
                    }, ActorRef.noSender());
                }
            };

            shardActorsStopping.put(shardName, onComplete);
            stopFuture.whenCompleteAsync(onComplete,
                DispatcherType.Client.dispatcherIn(context().system().dispatchers()));
        }

        LOG.debug("{} : Local Shard replica for shard {} has been removed", name(), shardName);
        persistShardList();
    }

    private void onGetSnapshot(final GetSnapshot getSnapshot) {
        LOG.debug("{}: onGetSnapshot", name());

        List<String> notInitialized = null;
        for (var shardInfo : localShards.values()) {
            if (!shardInfo.isShardInitialized()) {
                if (notInitialized == null) {
                    notInitialized = new ArrayList<>();
                }

                notInitialized.add(shardInfo.getShardName());
            }
        }

        if (notInitialized != null) {
            getSender().tell(new Status.Failure(new IllegalStateException(String.format(
                    "%d shard(s) %s are not initialized", notInitialized.size(), notInitialized))), self());
            return;
        }

        ActorRef replyActor = getContext().actorOf(ShardManagerGetSnapshotReplyActor.props(
                new ArrayList<>(localShards.keySet()), type, currentSnapshot , getSender(), name(),
                datastoreContextFactory.getBaseDatastoreContext().getShardInitializationTimeout()));

        for (var shardInfo : localShards.values()) {
            shardInfo.getActor().tell(getSnapshot, replyActor);
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void onCreateShard(final CreateShard createShard) {
        LOG.debug("{}: onCreateShard: {}", name(), createShard);

        Object reply;
        try {
            String shardName = createShard.getModuleShardConfig().getShardName();
            if (localShards.containsKey(shardName)) {
                LOG.debug("{}: Shard {} already exists", name(), shardName);
                reply = new Status.Success(String.format("Shard with name %s already exists", shardName));
            } else {
                doCreateShard(createShard);
                reply = new Status.Success(null);
            }
        } catch (Exception e) {
            LOG.error("{}: onCreateShard failed", name(), e);
            reply = new Status.Failure(e);
        }

        if (getSender() != null && !getContext().system().deadLetters().equals(getSender())) {
            getSender().tell(reply, self());
        }
    }

    private boolean isPreviousShardActorStopInProgress(final String shardName, final Object messageToDefer) {
        final CompositeOnComplete<Boolean> stopOnComplete = shardActorsStopping.get(shardName);
        if (stopOnComplete == null) {
            return false;
        }

        LOG.debug("{} : Stop is in progress for shard {} - adding OnComplete callback to defer {}", name(),
                shardName, messageToDefer);
        final ActorRef sender = getSender();
        stopOnComplete.addOnComplete((result, failure) -> {
            LOG.debug("{} : Stop complete for shard {} - re-queing {}", name(), shardName, messageToDefer);
            self().tell(messageToDefer, sender);
        });

        return true;
    }

    private void doCreateShard(final CreateShard createShard) {
        final ModuleShardConfiguration moduleShardConfig = createShard.getModuleShardConfig();
        final String shardName = moduleShardConfig.getShardName();

        configuration.addModuleShardConfiguration(moduleShardConfig);

        DatastoreContext shardDatastoreContext = createShard.getDatastoreContext();
        if (shardDatastoreContext == null) {
            shardDatastoreContext = newShardDatastoreContext(shardName);
        } else {
            shardDatastoreContext = DatastoreContext.newBuilderFrom(shardDatastoreContext).shardPeerAddressResolver(
                    peerAddressResolver).build();
        }

        final var shardId = getShardIdentifier(cluster.getCurrentMemberName(), shardName);

        boolean shardWasInRecoveredSnapshot = currentSnapshot != null
                && currentSnapshot.getShardList().contains(shardName);

        Map<String, String> peerAddresses;
        boolean isActiveMember;
        if (shardWasInRecoveredSnapshot || configuration.getMembersFromShardName(shardName)
                .contains(cluster.getCurrentMemberName())) {
            peerAddresses = getPeerAddresses(shardName);
            isActiveMember = true;
        } else {
            // The local member is not in the static shard member configuration and the shard did not
            // previously exist (ie !shardWasInRecoveredSnapshot). In this case we'll create
            // the shard with no peers and with elections disabled so it stays as follower. A
            // subsequent AddServer request will be needed to make it an active member.
            isActiveMember = false;
            peerAddresses = Map.of();
            shardDatastoreContext = DatastoreContext.newBuilderFrom(shardDatastoreContext)
                    .customRaftPolicyImplementation(WellKnownRaftPolicy.DISABLE_ELECTIONS.symbolicName()).build();
        }

        LOG.debug("{} doCreateShard: shardId: {}, memberNames: {}, peerAddresses: {}, isActiveMember: {}",
                name(), shardId, moduleShardConfig.getShardMemberNames(), peerAddresses, isActiveMember);

        ShardInformation info = new ShardInformation(stateDir, shardName, shardId, peerAddresses,
                shardDatastoreContext, createShard.getShardBuilder(), peerAddressResolver);
        info.setActiveMember(isActiveMember);
        localShards.put(info.getShardName(), info);

        if (modelContext != null) {
            info.setModelContext(modelContext);
            info.setActor(newShardActor(info));
        }
    }

    private DatastoreContext.Builder newShardDatastoreContextBuilder(final String shardName) {
        return DatastoreContext.newBuilderFrom(datastoreContextFactory.getShardDatastoreContext(shardName))
                .shardPeerAddressResolver(peerAddressResolver);
    }

    private DatastoreContext newShardDatastoreContext(final String shardName) {
        return newShardDatastoreContextBuilder(shardName).build();
    }

    private void checkReady() {
        if (isReadyWithLeaderId()) {
            LOG.info("{}: All Shards are ready - data store {} is ready", name(), type);
            readinessFuture.set(Empty.value());
        }
    }

    private void onLeaderStateChanged(final ShardLeaderStateChanged leaderStateChanged) {
        LOG.info("{}: Received LeaderStateChanged message: {}", name(), leaderStateChanged);

        ShardInformation shardInformation = findShardInformation(leaderStateChanged.memberId());
        if (shardInformation != null) {
            shardInformation.setLocalDataTree(leaderStateChanged.localShardDataTree());
            shardInformation.setLeaderVersion(leaderStateChanged.leaderPayloadVersion());
            if (shardInformation.setLeaderId(leaderStateChanged.leaderId())) {
                primaryShardInfoCache.remove(shardInformation.getShardName());

                notifyShardAvailabilityCallbacks(shardInformation);
            }

            checkReady();
        } else {
            LOG.debug("No shard found with member Id {}", leaderStateChanged.memberId());
        }
    }

    private void notifyShardAvailabilityCallbacks(final ShardInformation shardInformation) {
        shardAvailabilityCallbacks.forEach(callback -> callback.accept(shardInformation.getShardName()));
    }

    private void onShardNotInitializedTimeout(final ShardNotInitializedTimeout message) {
        ShardInformation shardInfo = message.getShardInfo();

        LOG.debug("{}: Received ShardNotInitializedTimeout message for shard {}", name(),
                shardInfo.getShardName());

        shardInfo.removeOnShardInitialized(message.getOnShardInitialized());

        if (!shardInfo.isShardInitialized()) {
            LOG.debug("{}: Returning NotInitializedException for shard {}", name(), shardInfo.getShardName());
            message.getSender().tell(createNotInitializedException(shardInfo.getShardId()), self());
        } else {
            LOG.debug("{}: Returning NoShardLeaderException for shard {}", name(), shardInfo.getShardName());
            message.getSender().tell(new NoShardLeaderException(shardInfo.getShardId()), self());
        }
    }

    private void onFollowerInitialSyncStatus(final FollowerInitialSyncUpStatus status) {
        LOG.info("{} Received follower initial sync status for {} status sync done {}", name(), status.memberId(),
            status.initialSyncDone());

        final var shardInformation = findShardInformation(status.memberId());
        if (shardInformation != null) {
            shardInformation.setFollowerSyncStatus(status.initialSyncDone());
            shardManagerMBean.setSyncStatus(isInSync());
        }
    }

    private void onRoleChangeNotification(final RoleChangeNotification roleChanged) {
        LOG.info("{}: Received role changed for {} from {} to {}", name(), roleChanged.memberId(),
            roleChanged.oldRole(), roleChanged.newRole());

        ShardInformation shardInformation = findShardInformation(roleChanged.memberId());
        if (shardInformation != null) {
            shardInformation.setRole(roleChanged.newRole());
            checkReady();
            shardManagerMBean.setSyncStatus(isInSync());
        }
    }

    private ShardInformation findShardInformation(final String memberId) {
        for (ShardInformation info : localShards.values()) {
            if (info.getShardId().toString().equals(memberId)) {
                return info;
            }
        }

        return null;
    }

    private boolean isReadyWithLeaderId() {
        boolean isReady = true;
        for (ShardInformation info : localShards.values()) {
            if (!info.isShardReadyWithLeaderId()) {
                isReady = false;
                break;
            }
        }
        return isReady;
    }

    private boolean isInSync() {
        for (ShardInformation info : localShards.values()) {
            if (!info.isInSync()) {
                return false;
            }
        }
        return true;
    }

    private void onActorInitialized(final ActorInitialized message) {
        final var sender = message.actorRef();

        String actorName = sender.path().name();
        //find shard name from actor name; actor name is stringified shardId

        final ShardIdentifier shardId;
        try {
            shardId = ShardIdentifier.fromShardIdString(actorName);
        } catch (IllegalArgumentException e) {
            LOG.debug("{}: ignoring actor {}", name(), actorName, e);
            return;
        }

        markShardAsInitialized(shardId.getShardName());
    }

    private void markShardAsInitialized(final String shardName) {
        LOG.debug("{}: Initializing shard [{}]", name(), shardName);

        ShardInformation shardInformation = localShards.get(shardName);
        if (shardInformation != null) {
            shardInformation.setActorInitialized();

            shardInformation.getActor().tell(new RegisterRoleChangeListener(), self());
        }
    }

    private void sendResponse(final ShardInformation shardInformation, final boolean doWait,
            final boolean wantShardReady, final Supplier<Object> messageSupplier) {
        if (!shardInformation.isShardInitialized() || wantShardReady && !shardInformation.isShardReadyWithLeaderId()) {
            if (doWait) {
                final ActorRef sender = getSender();
                final ActorRef self = self();

                Runnable replyRunnable = () -> sender.tell(messageSupplier.get(), self);

                OnShardInitialized onShardInitialized = wantShardReady ? new OnShardReady(replyRunnable) :
                    new OnShardInitialized(replyRunnable);

                shardInformation.addOnShardInitialized(onShardInitialized);

                final Duration timeout;
                if (shardInformation.isShardInitialized()) {
                    // If the shard is already initialized then we'll wait enough time for the shard to
                    // elect a leader, ie 2 times the election timeout.
                    timeout = shardInformation.getDatastoreContext().getShardRaftConfig().getElectionTimeOutInterval()
                        .multipliedBy(2);
                } else {
                    timeout = shardInformation.getDatastoreContext().getShardInitializationTimeout();
                }

                LOG.debug("{}: Scheduling {} ms timer to wait for shard {}", name(), timeout.toMillis(),
                        shardInformation);

                final var timeoutSchedule = getContext().system().scheduler().scheduleOnce(
                        timeout, self(),
                        new ShardNotInitializedTimeout(shardInformation, onShardInitialized, sender),
                        getContext().dispatcher(), self());

                onShardInitialized.setTimeoutSchedule(timeoutSchedule);

            } else if (!shardInformation.isShardInitialized()) {
                LOG.debug("{}: Returning NotInitializedException for shard {}", name(),
                        shardInformation.getShardName());
                getSender().tell(createNotInitializedException(shardInformation.getShardId()), self());
            } else {
                LOG.debug("{}: Returning NoShardLeaderException for shard {}", name(),
                        shardInformation.getShardName());
                getSender().tell(new NoShardLeaderException(shardInformation.getShardId()), self());
            }

            return;
        }

        getSender().tell(messageSupplier.get(), self());
    }

    private static NotInitializedException createNotInitializedException(final ShardIdentifier shardId) {
        return new NotInitializedException(String.format(
                "Found primary shard %s but it's not initialized yet. Please try again later", shardId));
    }

    @VisibleForTesting
    static MemberName memberToName(final Member member) {
        return MemberName.forName(member.roles().iterator().next());
    }

    private void memberRemoved(final ClusterEvent.MemberRemoved message) {
        MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberRemoved: memberName: {}, address: {}", name(), memberName,
                message.member().address());

        peerAddressResolver.removePeerAddress(memberName);
    }

    private void memberExited(final ClusterEvent.MemberExited message) {
        MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberExited: memberName: {}, address: {}", name(), memberName,
                message.member().address());

        peerAddressResolver.removePeerAddress(memberName);
    }

    private void memberUp(final ClusterEvent.MemberUp message) {
        MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberUp: memberName: {}, address: {}", name(), memberName,
                message.member().address());

        memberUp(memberName, message.member().address());
    }

    private void memberUp(final MemberName memberName, final Address address) {
        addPeerAddress(memberName, address);
        checkReady();
    }

    private void memberWeaklyUp(final MemberWeaklyUp message) {
        MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberWeaklyUp: memberName: {}, address: {}", name(), memberName,
                message.member().address());

        memberUp(memberName, message.member().address());
    }

    private void addPeerAddress(final MemberName memberName, final Address address) {
        peerAddressResolver.addPeerAddress(memberName, address);

        for (ShardInformation info : localShards.values()) {
            String shardName = info.getShardName();
            String peerId = getShardIdentifier(memberName, shardName).toString();
            info.updatePeerAddress(peerId, peerAddressResolver.getShardActorAddress(shardName, memberName), self());
        }
    }

    private void memberReachable(final ClusterEvent.ReachableMember message) {
        MemberName memberName = memberToName(message.member());
        LOG.info("Received ReachableMember: memberName {}, address: {}", memberName, message.member().address());

        addPeerAddress(memberName, message.member().address());

        markMemberAvailable(memberName);
    }

    private void memberUnreachable(final ClusterEvent.UnreachableMember message) {
        MemberName memberName = memberToName(message.member());
        LOG.info("Received UnreachableMember: memberName {}, address: {}", memberName, message.member().address());

        markMemberUnavailable(memberName);
    }

    private void markMemberUnavailable(final MemberName memberName) {
        for (ShardInformation info : localShards.values()) {
            String leaderId = info.getLeaderId();
            if (leaderId != null && ShardIdentifier.fromShardIdString(leaderId).getMemberName().equals(memberName)) {
                LOG.debug("Marking Leader {} as unavailable.", leaderId);
                info.setLeaderAvailable(false);

                primaryShardInfoCache.remove(info.getShardName());

                notifyShardAvailabilityCallbacks(info);
            }
        }
    }

    private void markMemberAvailable(final MemberName memberName) {
        for (ShardInformation info : localShards.values()) {
            String leaderId = info.getLeaderId();
            if (leaderId != null && ShardIdentifier.fromShardIdString(leaderId).getMemberName().equals(memberName)) {
                LOG.debug("Marking Leader {} as available.", leaderId);
                info.setLeaderAvailable(true);
            }
        }
    }

    private void onDatastoreContextFactory(final DatastoreContextFactory factory) {
        datastoreContextFactory = factory;
        for (ShardInformation info : localShards.values()) {
            info.setDatastoreContext(newShardDatastoreContext(info.getShardName()), self());
        }
    }

    private void onGetLocalShardIds() {
        final List<String> response = new ArrayList<>(localShards.size());

        for (ShardInformation info : localShards.values()) {
            response.add(info.getShardId().toString());
        }

        getSender().tell(new Status.Success(response), self());
    }

    private void onSwitchShardBehavior(final @Nullable ShardIdentifier shardId, final SwitchBehavior switchBehavior) {
        final var status = shardId != null ? switchOneShard(shardId, switchBehavior) : switchAllShards(switchBehavior);
        getSender().tell(status, self());
    }

    private Status.Success switchAllShards(final SwitchBehavior switchBehavior) {
        for (var info : localShards.values()) {
            switchShardBehavior(info, switchBehavior);
        }
        return new Status.Success(null);
    }

    private Status.Status switchOneShard(final ShardIdentifier shardId, final SwitchBehavior switchBehavior) {
        final var info = localShards.get(shardId.getShardName());
        if (info == null) {
            return new Status.Failure(new IllegalArgumentException("Shard " + shardId + " is not local"));
        }
        switchShardBehavior(info, switchBehavior);
        return new Status.Success(null);
    }

    private void switchShardBehavior(final ShardInformation info, final SwitchBehavior switchBehavior) {
        final var actor = info.getActor();
        if (actor != null) {
            actor.tell(switchBehavior, self());
        } else {
            LOG.warn("Could not switch the behavior of shard {} - shard is not yet available", info.getShardName());
        }
    }

    /**
     * Notifies all the local shards of a change in the schema context.
     *
     * @param message the message to send
     */
    private void updateSchemaContext(final UpdateSchemaContext message) {
        modelContext = message.modelContext();

        LOG.debug("Got updated SchemaContext: # of modules {}", modelContext.getModules().size());

        for (var info : localShards.values()) {
            info.setModelContext(modelContext);
            final var actor = info.getActor();
            if (actor != null) {
                actor.tell(message, self());
                continue;
            }

            LOG.debug("Creating Shard {}", info.getShardId());
            info.setActor(newShardActor(info));
            // Update peer address for every existing peer memeber to avoid missing sending
            // PeerAddressResolved and PeerUp to this shard while UpdateSchemaContext comes after MemberUp.
            String shardName = info.getShardName();
            for (var memberName : peerAddressResolver.getPeerMembers()) {
                String peerId = getShardIdentifier(memberName, shardName).toString() ;
                String peerAddress = peerAddressResolver.getShardActorAddress(shardName, memberName);
                info.updatePeerAddress(peerId, peerAddress, self());
                LOG.debug("{}: updated peer {} on member {} with address {} on shard {} whose actor address is {}",
                    name(), peerId, memberName, peerAddress, info.getShardId(), info.getActor());
            }
        }
    }

    @VisibleForTesting
    protected ClusterWrapper getCluster() {
        return cluster;
    }

    @VisibleForTesting
    protected ActorRef newShardActor(final ShardInformation info) {
        return getContext().actorOf(info.newProps().withDispatcher(shardDispatcherPath),
                info.getShardId().toString());
    }

    private Duration findTimeout() {
        return datastoreContextFactory.getBaseDatastoreContext().getShardInitializationTimeout().multipliedBy(2);
    }

    // FIXME: non-null prevAddresses and update checks below once we have removed RemoteFindPrimary
    private void findPrimary(final FindPrimary message, final @Nullable Set<String> previousActorPaths) {
        LOG.debug("{}: In findPrimary: {}", name(), message);

        final String shardName = message.getShardName();

        // First see if the there is a local replica for the shard
        final var info = localShards.get(shardName);
        if (info != null && info.isActiveMember()) {
            final boolean canReturnLocalShardState = previousActorPaths == null;
            sendResponse(info, message.isWaitUntilReady(), true, () -> {
                String primaryPath = info.getSerializedLeaderActor();
                Object found = canReturnLocalShardState && info.isLeader()
                        ? new LocalPrimaryShardFound(primaryPath, info.getLocalShardDataTree().orElseThrow()) :
                            new RemotePrimaryShardFound(primaryPath, info.getLeaderVersion());

                LOG.debug("{}: Found primary for {}: {}", name(), shardName, found);
                return found;
            });

            return;
        }

        final var visitedAddresses = previousActorPaths != null ? new HashSet<>(previousActorPaths)
            : HashSet.<String>newHashSet(1);
        visitedAddresses.add(peerAddressResolver.getShardManagerActorPathBuilder(cluster.getSelfAddress()).toString());

        for (var address : peerAddressResolver.getShardManagerPeerActorAddresses()) {
            if (visitedAddresses.contains(address)) {
                continue;
            }

            LOG.debug("{}: findPrimary for {} forwarding to remote ShardManager {}, visitedAddresses: {}",
                    name(), shardName, address, visitedAddresses);

            final var context = getContext();
            context.actorSelection(address).forward(new ForwardedFindPrimary(message, visitedAddresses), context);
            return;
        }

        LOG.debug("{}: No shard found for {}", name(), shardName);

        getSender().tell(new PrimaryNotFoundException(
                String.format("No primary shard found for %s.", shardName)), self());
    }

    private void findPrimary(final String shardName, final FindPrimaryResponseHandler handler) {
        Patterns.ask(self(), new FindPrimary(shardName, true), findTimeout()).whenCompleteAsync(
            (response, failure) -> {
                if (failure != null) {
                    handler.onFailure(failure);
                    return;
                }
                switch (response) {
                    case RemotePrimaryShardFound msg -> handler.onRemotePrimaryShardFound(msg);
                    case LocalPrimaryShardFound msg -> handler.onLocalPrimaryFound(msg);
                    default -> handler.onUnknownResponse(response);
                }
            }, DispatcherType.Client.dispatcherIn(context().system().dispatchers()));
    }

    /**
     * Construct the name of the shard actor given the name of the member on
     * which the shard resides and the name of the shard.
     *
     * @param memberName the member name
     * @param shardName the shard name
     * @return a b
     */
    private ShardIdentifier getShardIdentifier(final MemberName memberName, final String shardName) {
        return peerAddressResolver.getShardIdentifier(memberName, shardName);
    }

    /**
     * Create shards that are local to the member on which the ShardManager runs.
     */
    private void createLocalShards() {
        MemberName memberName = cluster.getCurrentMemberName();
        final var memberShardNames = configuration.getMemberShardNames(memberName);

        final var shardSnapshots = new HashMap<String, DatastoreSnapshot.ShardSnapshot>();
        if (restoreFromSnapshot != null) {
            for (var snapshot : restoreFromSnapshot.getShardSnapshots()) {
                shardSnapshots.put(snapshot.getName(), snapshot);
            }
        }

        // null out to GC
        restoreFromSnapshot = null;

        for (var shardName : memberShardNames) {
            final var shardId = getShardIdentifier(memberName, shardName);
            LOG.debug("{}: Creating local shard: {}", name(), shardId);

            final var peerAddresses = getPeerAddresses(shardName);
            localShards.put(shardName, createShardInfoFor(shardName, shardId, peerAddresses,
                    newShardDatastoreContext(shardName), shardSnapshots));
        }
    }

    @VisibleForTesting
    ShardInformation createShardInfoFor(final String shardName, final ShardIdentifier shardId,
                                        final Map<String, String> peerAddresses,
                                        final DatastoreContext datastoreContext,
                                        final Map<String, DatastoreSnapshot.ShardSnapshot> shardSnapshots) {
        return new ShardInformation(stateDir, shardName, shardId, peerAddresses,
                datastoreContext, Shard.builder().restoreFromSnapshot(shardSnapshots.get(shardName)),
                peerAddressResolver);
    }

    /**
     * Given the name of the shard find the addresses of all it's peers.
     *
     * @param shardName the shard name
     */
    Map<String, String> getPeerAddresses(final String shardName) {
        return getPeerAddresses(shardName, configuration.getMembersFromShardName(shardName));
    }

    private Map<String, String> getPeerAddresses(final String shardName, final Collection<MemberName> members) {
        final var peerAddresses = new HashMap<String, String>();
        final var currentMemberName = cluster.getCurrentMemberName();

        for (var memberName : members) {
            if (!currentMemberName.equals(memberName)) {
                final var shardId = getShardIdentifier(memberName, shardName);
                final var address = peerAddressResolver.getShardActorAddress(shardName, memberName);
                peerAddresses.put(shardId.toString(), address);
            }
        }
        return peerAddresses;
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(10, Duration.ofMinutes(1), t -> {
            LOG.warn("Supervisor Strategy caught unexpected exception - resuming", t);
            return SupervisorStrategy.resume();
        });
    }

    @VisibleForTesting
    ShardManagerInfoMBean getMBean() {
        return shardManagerMBean;
    }

    private boolean isShardReplicaOperationInProgress(final String shardName, final ActorRef sender) {
        if (shardReplicaOperationsInProgress.contains(shardName)) {
            LOG.debug("{}: A shard replica operation for {} is already in progress", name(), shardName);
            sender.tell(new Status.Failure(new IllegalStateException(
                String.format("A shard replica operation for %s is already in progress", shardName))), self());
            return true;
        }

        return false;
    }

    private void onAddShardReplica(final AddShardReplica shardReplicaMsg) {
        final String shardName = shardReplicaMsg.getShardName();

        LOG.debug("{}: onAddShardReplica: {}", name(), shardReplicaMsg);

        // verify the shard with the specified name is present in the cluster configuration
        if (!configuration.isShardConfigured(shardName)) {
            LOG.debug("{}: No module configuration exists for shard {}", name(), shardName);
            getSender().tell(new Status.Failure(new IllegalArgumentException(
                "No module configuration exists for shard " + shardName)), self());
            return;
        }

        // Create the localShard
        if (modelContext == null) {
            LOG.debug("{}: No SchemaContext is available in order to create a local shard instance for {}",
                name(), shardName);
            getSender().tell(new Status.Failure(new IllegalStateException(
                "No SchemaContext is available in order to create a local shard instance for " + shardName)), self());
            return;
        }

        findPrimary(shardName, new AutoFindPrimaryFailureResponseHandler(getSender(), shardName, name(),
                self()) {
            @Override
            public void onRemotePrimaryShardFound(final RemotePrimaryShardFound response) {
                final RunnableMessage runnable = () -> addShard(getShardName(), response, getSender());
                if (!isPreviousShardActorStopInProgress(getShardName(), runnable)) {
                    self().tell(runnable, getTargetActor());
                }
            }

            @Override
            public void onLocalPrimaryFound(final LocalPrimaryShardFound message) {
                sendLocalReplicaAlreadyExistsReply(getShardName(), getTargetActor());
            }
        });
    }

    private void sendLocalReplicaAlreadyExistsReply(final String shardName, final ActorRef sender) {
        LOG.debug("{}: Local shard {} already exists", name(), shardName);
        sender.tell(new Status.Failure(new AlreadyExistsException(
            String.format("Local shard %s already exists", shardName))), self());
    }

    private void addShard(final String shardName, final RemotePrimaryShardFound response, final ActorRef sender) {
        if (isShardReplicaOperationInProgress(shardName, sender)) {
            return;
        }

        shardReplicaOperationsInProgress.add(shardName);

        final ShardInformation shardInfo;
        final boolean removeShardOnFailure;
        ShardInformation existingShardInfo = localShards.get(shardName);
        if (existingShardInfo == null) {
            removeShardOnFailure = true;
            ShardIdentifier shardId = getShardIdentifier(cluster.getCurrentMemberName(), shardName);

            DatastoreContext datastoreContext = newShardDatastoreContextBuilder(shardName)
                    .customRaftPolicyImplementation(WellKnownRaftPolicy.DISABLE_ELECTIONS.symbolicName()).build();

            shardInfo = new ShardInformation(stateDir, shardName, shardId, getPeerAddresses(shardName),
                datastoreContext, Shard.builder(), peerAddressResolver);
            shardInfo.setActiveMember(false);
            shardInfo.setModelContext(modelContext);
            localShards.put(shardName, shardInfo);
            shardInfo.setActor(newShardActor(shardInfo));
        } else {
            removeShardOnFailure = false;
            shardInfo = existingShardInfo;
        }

        execAddShard(shardName, shardInfo, response, removeShardOnFailure, sender);
    }

    private void execAddShard(final String shardName,
                              final ShardInformation shardInfo,
                              final RemotePrimaryShardFound response,
                              final boolean removeShardOnFailure,
                              final ActorRef sender) {

        final String localShardAddress =
                peerAddressResolver.getShardActorAddress(shardName, cluster.getCurrentMemberName());

        //inform ShardLeader to add this shard as a replica by sending an AddServer message
        LOG.debug("{}: Sending AddServer message to peer {} for shard {}", name(), response.primaryPath(),
            shardInfo.getShardId());

        final var futureObj = Patterns.ask(getContext().actorSelection(response.primaryPath()),
            new AddServer(shardInfo.getShardId().toString(), localShardAddress, true),
            shardInfo.getDatastoreContext().getShardLeaderElectionTimeout());

        futureObj.whenCompleteAsync((addServerResponse, failure) -> {
            if (failure == null) {
                self().tell(new ForwardedAddServerReply(shardInfo, (AddServerReply) addServerResponse,
                    response.primaryPath(), removeShardOnFailure), sender);
                return;
            }

            LOG.debug("{}: AddServer request to {} for {} failed", name(), response.primaryPath(), shardName, failure);

            self().tell(new ForwardedAddServerFailure(shardName,
                "AddServer request to leader %s for shard %s failed".formatted(response.primaryPath(), shardName),
                failure, removeShardOnFailure), sender);
        }, DispatcherType.Client.dispatcherIn(context().system().dispatchers()));
    }

    private void onAddServerFailure(final String shardName, final String message, final Throwable failure,
            final ActorRef sender, final boolean removeShardOnFailure) {
        shardReplicaOperationsInProgress.remove(shardName);

        if (removeShardOnFailure) {
            final var shardInfo = localShards.remove(shardName);
            final var actor = shardInfo.getActor();
            if (actor != null) {
                actor.tell(PoisonPill.getInstance(), self());
            }
        }

        sender.tell(new Status.Failure(message == null ? failure : new RuntimeException(message, failure)), self());
    }

    private void onAddServerReply(final ShardInformation shardInfo, final AddServerReply replyMsg,
            final ActorRef sender, final String leaderPath, final boolean removeShardOnFailure) {
        String shardName = shardInfo.getShardName();
        shardReplicaOperationsInProgress.remove(shardName);

        LOG.debug("{}: Received {} for shard {} from leader {}", name(), replyMsg, shardName, leaderPath);

        if (replyMsg.getStatus() == ServerChangeStatus.OK) {
            LOG.debug("{}: Leader shard successfully added the replica shard {}", name(), shardName);

            // Make the local shard voting capable
            shardInfo.setDatastoreContext(newShardDatastoreContext(shardName), self());
            shardInfo.setActiveMember(true);
            persistShardList();

            sender.tell(new Status.Success(null), self());
        } else if (replyMsg.getStatus() == ServerChangeStatus.ALREADY_EXISTS) {
            sendLocalReplicaAlreadyExistsReply(shardName, sender);
        } else {
            LOG.warn("{}: Leader failed to add shard replica {} with status {}",
                    name(), shardName, replyMsg.getStatus());

            Exception failure = getServerChangeException(AddServer.class, replyMsg.getStatus(), leaderPath,
                    shardInfo.getShardId());

            onAddServerFailure(shardName, null, failure, sender, removeShardOnFailure);
        }
    }

    private static Exception getServerChangeException(final Class<?> serverChange,
            final ServerChangeStatus serverChangeStatus, final String leaderPath, final ShardIdentifier shardId) {
        return switch (serverChangeStatus) {
            case TIMEOUT -> new TimeoutException("""
                The shard leader %s timed out trying to replicate the initial data to the new shard %s. Possible \
                causes - there was a problem replicating the data or shard leadership changed while replicating the \
                shard data""".formatted(leaderPath, shardId.getShardName()));
            case NO_LEADER -> new NoShardLeaderException(shardId);
            case NOT_SUPPORTED -> new UnsupportedOperationException(
                "%s request is not supported for shard %s".formatted(
                    serverChange.getSimpleName(), shardId.getShardName()));
            default -> new RuntimeException("%s request to leader %s for shard %s failed with status %s".formatted(
                serverChange.getSimpleName(), leaderPath, shardId.getShardName(), serverChangeStatus));
        };
    }

    private void onRemoveShardReplica(final RemoveShardReplica shardReplicaMsg) {
        LOG.debug("{}: onRemoveShardReplica: {}", name(), shardReplicaMsg);

        findPrimary(shardReplicaMsg.getShardName(), new AutoFindPrimaryFailureResponseHandler(getSender(),
                shardReplicaMsg.getShardName(), name(), self()) {
            @Override
            public void onRemotePrimaryShardFound(final RemotePrimaryShardFound response) {
                doRemoveShardReplicaAsync(response.primaryPath());
            }

            @Override
            public void onLocalPrimaryFound(final LocalPrimaryShardFound response) {
                doRemoveShardReplicaAsync(response.primaryPath());
            }

            private void doRemoveShardReplicaAsync(final String primaryPath) {
                self().tell((RunnableMessage) () -> removeShardReplica(shardReplicaMsg, getShardName(),
                        primaryPath, getSender()), getTargetActor());
            }
        });
    }

    private void persistShardList() {
        final var shardList = new ArrayList<>(localShards.keySet());
        for (var shardInfo : localShards.values()) {
            if (!shardInfo.isActiveMember()) {
                shardList.remove(shardInfo.getShardName());
            }
        }
        LOG.debug("{}: persisting the shard list {}", name(), shardList);
        updateSnapshot(new ShardManagerSnapshot(shardList));
    }

    @NonNullByDefault
    private void updateSnapshot(final ShardManagerSnapshot newSnapshot) {
        currentSnapshot = newSnapshot;
        try {
            saveSnapshot(stateDir, name(), newSnapshot);
        } catch (IOException e) {
            LOG.error("{}: SaveSnapshotFailure received for saving snapshot of shards", name(), e);
        }
    }

    @VisibleForTesting
    @Nullable ShardManagerSnapshot loadSnapshot() throws IOException {
        try (var dis = new DataInputStream(Files.newInputStream(stateDir.resolve(name())))) {
            final var size = dis.readInt();
            final var builder = ImmutableList.<String>builderWithExpectedSize(size);
            for (int i = 0; i < size; ++i) {
                builder.add(dis.readUTF());
            }
            return new ShardManagerSnapshot(builder.build());
        } catch (NoSuchFileException e) {
            if (!LOG.isTraceEnabled()) {
                e = null;
            }
            LOG.debug("{}: snapshot not present", name(), e);
            return null;
        }
    }

    @VisibleForTesting
    static final void saveSnapshot(final Path stateDir, final String fileName, final ShardManagerSnapshot snapshot)
            throws IOException {
        final var tmp = Files.createTempFile(stateDir, fileName, null);
        try (var dos = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp, SYNC)))) {
            final var shardList = snapshot.getShardList();
            dos.writeInt(shardList.size());
            for (var shard : shardList) {
                dos.writeUTF(shard);
            }
        }

        Files.move(tmp, stateDir.resolve(fileName), ATOMIC_MOVE, REPLACE_EXISTING);
    }

    private void applyShardManagerSnapshot(final ShardManagerSnapshot snapshot) {
        currentSnapshot = requireNonNull(snapshot);
        LOG.debug("{}: applying snapshot  {}", name(), snapshot);

        final var currentMember = cluster.getCurrentMemberName();
        final var configuredShardList = new HashSet<>(configuration.getMemberShardNames(currentMember));
        for (var shard : currentSnapshot.getShardList()) {
            if (!configuredShardList.contains(shard)) {
                // add the current member as a replica for the shard
                LOG.debug("{}: adding shard {}", name(), shard);
                configuration.addMemberReplicaForShard(shard, currentMember);
            } else {
                configuredShardList.remove(shard);
            }
        }
        for (var shard : configuredShardList) {
            // remove the member as a replica for the shard
            LOG.debug("{}: removing shard {}", name(), shard);
            configuration.removeMemberReplicaForShard(shard, currentMember);
        }
    }

    private void onChangeShardServersVotingStatus(final ChangeShardMembersVotingStatus changeMembersVotingStatus) {
        LOG.debug("{}: onChangeShardServersVotingStatus: {}", name(), changeMembersVotingStatus);

        String shardName = changeMembersVotingStatus.getShardName();
        Map<String, Boolean> serverVotingStatusMap = new HashMap<>();
        for (Entry<String, Boolean> e: changeMembersVotingStatus.getMeberVotingStatusMap().entrySet()) {
            serverVotingStatusMap.put(getShardIdentifier(MemberName.forName(e.getKey()), shardName).toString(),
                    e.getValue());
        }

        ChangeServersVotingStatus changeServersVotingStatus = new ChangeServersVotingStatus(serverVotingStatusMap);

        findLocalShard(shardName, getSender(),
            localShardFound -> changeShardMembersVotingStatus(changeServersVotingStatus, shardName,
            localShardFound.getPath(), getSender()));
    }

    private void onFlipShardMembersVotingStatus(final FlipShardMembersVotingStatus flipMembersVotingStatus) {
        LOG.debug("{}: onFlipShardMembersVotingStatus: {}", name(), flipMembersVotingStatus);

        ActorRef sender = getSender();
        final String shardName = flipMembersVotingStatus.getShardName();
        findLocalShard(shardName, sender, localShardFound ->
            Patterns.ask(localShardFound.getPath(), GetOnDemandRaftState.INSTANCE, Duration.ofSeconds(30))
                .whenCompleteAsync((response, failure) -> {
                    if (failure != null) {
                        sender.tell(new Status.Failure(new RuntimeException(
                                String.format("Failed to access local shard %s", shardName), failure)), self());
                        return;
                    }

                    final var raftState = (OnDemandRaftState) response;
                    final var serverVotingStatusMap = new HashMap<String, Boolean>();
                    for (var entry : raftState.getPeerVotingStates().entrySet()) {
                        serverVotingStatusMap.put(entry.getKey(), !entry.getValue());
                    }

                    serverVotingStatusMap.put(getShardIdentifier(cluster.getCurrentMemberName(), shardName).toString(),
                        !raftState.isVoting());

                    changeShardMembersVotingStatus(new ChangeServersVotingStatus(serverVotingStatusMap), shardName,
                        localShardFound.getPath(), sender);
                }, DispatcherType.Client.dispatcherIn(context().system().dispatchers())));
    }

    private void findLocalShard(final FindLocalShard message) {
        LOG.debug("{}: findLocalShard : {}", name(), message.getShardName());

        final ShardInformation shardInformation = localShards.get(message.getShardName());

        if (shardInformation == null) {
            LOG.debug("{}: Local shard {} not found - shards present: {}",
                    name(), message.getShardName(), localShards.keySet());

            getSender().tell(new LocalShardNotFound(message.getShardName()), self());
            return;
        }

        sendResponse(shardInformation, message.isWaitUntilInitialized(), false,
            () -> new LocalShardFound(shardInformation.getActor()));
    }

    private void findLocalShard(final String shardName, final ActorRef sender,
            final Consumer<LocalShardFound> onLocalShardFound) {
        Patterns.ask(self(), new FindLocalShard(shardName, true), findTimeout()).whenCompleteAsync(
            (response, failure) -> {
                if (failure != null) {
                    LOG.debug("{}: Received failure from FindLocalShard for shard {}", name(), shardName,
                        failure);
                    sender.tell(new Status.Failure(new RuntimeException(
                        String.format("Failed to find local shard %s", shardName), failure)), self());
                    return;
                }

                switch (response) {
                    case LocalShardFound msg ->
                        self().tell((RunnableMessage) () -> onLocalShardFound.accept(msg), sender);
                    case LocalShardNotFound msg -> {
                        LOG.debug("{}: Local shard {} does not exist", name(), shardName);
                        sender.tell(new Status.Failure(new IllegalArgumentException(
                            String.format("Local shard %s does not exist", shardName))), self());
                    }
                    default -> {
                        LOG.debug("{}: Failed to find local shard {}: received response: {}", name(), shardName,
                            response);
                        sender.tell(new Status.Failure(response instanceof Throwable throwable ? throwable
                            : new RuntimeException(String.format("Failed to find local shard %s: received response: %s",
                                shardName, response))), self());
                    }
                }
            }, DispatcherType.Client.dispatcherIn(context().system().dispatchers()));
    }

    private void changeShardMembersVotingStatus(final ChangeServersVotingStatus changeServersVotingStatus,
            final String shardName, final ActorRef shardActorRef, final ActorRef sender) {
        if (isShardReplicaOperationInProgress(shardName, sender)) {
            return;
        }

        shardReplicaOperationsInProgress.add(shardName);

        DatastoreContext datastoreContext = newShardDatastoreContextBuilder(shardName).build();
        final var shardId = getShardIdentifier(cluster.getCurrentMemberName(), shardName);

        LOG.debug("{}: Sending ChangeServersVotingStatus message {} to local shard {}", name(),
            changeServersVotingStatus, shardActorRef.path());

        Patterns.ask(shardActorRef, changeServersVotingStatus,
            datastoreContext.getShardLeaderElectionTimeout().multipliedBy(2)).whenCompleteAsync((response, failure) -> {
                shardReplicaOperationsInProgress.remove(shardName);
                if (failure != null) {
                    LOG.debug("{}: ChangeServersVotingStatus request to local shard {} failed", name(),
                        shardActorRef.path(), failure);
                    sender.tell(new Status.Failure(new RuntimeException(
                        "ChangeServersVotingStatus request to local shard %s failed".formatted(
                            shardActorRef.path()), failure)), self());
                    return;
                }

                LOG.debug("{}: Received {} from local shard {}", name(), response, shardActorRef.path());
                final var replyMsg = (ServerChangeReply) response;
                if (replyMsg.getStatus() == ServerChangeStatus.OK) {
                    LOG.debug("{}: ChangeServersVotingStatus succeeded for shard {}", name(), shardName);
                    sender.tell(new Status.Success(null), self());
                } else if (replyMsg.getStatus() == ServerChangeStatus.INVALID_REQUEST) {
                    sender.tell(new Status.Failure(new IllegalArgumentException(String.format(
                        "The requested voting state change for shard %s is invalid. At least one member "
                            + "must be voting", shardId.getShardName()))), self());
                } else {
                    LOG.warn("{}: ChangeServersVotingStatus failed for shard {} with status {}",
                        name(), shardName, replyMsg.getStatus());

                    Exception error = getServerChangeException(ChangeServersVotingStatus.class,
                        replyMsg.getStatus(), shardActorRef.path().toString(), shardId);
                    sender.tell(new Status.Failure(error), self());
                }
            }, DispatcherType.Client.dispatcherIn(context().system().dispatchers()));
    }

    private static final class ForwardedAddServerReply {
        ShardInformation shardInfo;
        AddServerReply addServerReply;
        String leaderPath;
        boolean removeShardOnFailure;

        ForwardedAddServerReply(final ShardInformation shardInfo, final AddServerReply addServerReply,
            final String leaderPath, final boolean removeShardOnFailure) {
            this.shardInfo = shardInfo;
            this.addServerReply = addServerReply;
            this.leaderPath = leaderPath;
            this.removeShardOnFailure = removeShardOnFailure;
        }
    }

    private static final class ForwardedAddServerFailure {
        String shardName;
        String failureMessage;
        Throwable failure;
        boolean removeShardOnFailure;

        ForwardedAddServerFailure(final String shardName, final String failureMessage, final Throwable failure,
                final boolean removeShardOnFailure) {
            this.shardName = shardName;
            this.failureMessage = failureMessage;
            this.failure = failure;
            this.removeShardOnFailure = removeShardOnFailure;
        }
    }

    static class OnShardInitialized {
        private final Runnable replyRunnable;
        private Cancellable timeoutSchedule;

        OnShardInitialized(final Runnable replyRunnable) {
            this.replyRunnable = replyRunnable;
        }

        Runnable getReplyRunnable() {
            return replyRunnable;
        }

        Cancellable getTimeoutSchedule() {
            return timeoutSchedule;
        }

        void setTimeoutSchedule(final Cancellable timeoutSchedule) {
            this.timeoutSchedule = timeoutSchedule;
        }
    }

    static class OnShardReady extends OnShardInitialized {
        OnShardReady(final Runnable replyRunnable) {
            super(replyRunnable);
        }
    }

    private interface RunnableMessage extends Runnable {
    }

    /**
     * The FindPrimaryResponseHandler provides specific callback methods which are invoked when a response to the
     * a remote or local find primary message is processed.
     */
    private interface FindPrimaryResponseHandler {
        /**
         * Invoked when a Failure message is received as a response.
         *
         * @param failure the failure exception
         */
        void onFailure(Throwable failure);

        /**
         * Invoked when a RemotePrimaryShardFound response is received.
         *
         * @param response the response
         */
        void onRemotePrimaryShardFound(RemotePrimaryShardFound response);

        /**
         * Invoked when a LocalPrimaryShardFound response is received.
         *
         * @param response the response
         */
        void onLocalPrimaryFound(LocalPrimaryShardFound response);

        /**
         * Invoked when an unknown response is received. This is another type of failure.
         *
         * @param response the response
         */
        void onUnknownResponse(Object response);
    }

    /**
     * The AutoFindPrimaryFailureResponseHandler automatically processes Failure responses when finding a primary
     * replica and sends a wrapped Failure response to some targetActor.
     */
    private abstract static class AutoFindPrimaryFailureResponseHandler implements FindPrimaryResponseHandler {
        private final ActorRef targetActor;
        private final String shardName;
        private final String persistenceId;
        private final ActorRef shardManagerActor;

        /**
         * Constructs an instance.
         *
         * @param targetActor The actor to whom the Failure response should be sent when a FindPrimary failure occurs
         * @param shardName The name of the shard for which the primary replica had to be found
         * @param persistenceId The persistenceId for the ShardManager
         * @param shardManagerActor The ShardManager actor which triggered the call to FindPrimary
         */
        protected AutoFindPrimaryFailureResponseHandler(final ActorRef targetActor, final String shardName,
                final String persistenceId, final ActorRef shardManagerActor) {
            this.targetActor = requireNonNull(targetActor);
            this.shardName = requireNonNull(shardName);
            this.persistenceId = requireNonNull(persistenceId);
            this.shardManagerActor = requireNonNull(shardManagerActor);
        }

        public ActorRef getTargetActor() {
            return targetActor;
        }

        public String getShardName() {
            return shardName;
        }

        @Override
        public void onFailure(final Throwable failure) {
            LOG.debug("{}: Received failure from FindPrimary for shard {}", persistenceId, shardName, failure);
            targetActor.tell(new Status.Failure(new RuntimeException(
                    String.format("Failed to find leader for shard %s", shardName), failure)), shardManagerActor);
        }

        @Override
        public void onUnknownResponse(final Object response) {
            LOG.debug("{}: Failed to find leader for shard {}: received response: {}", persistenceId, shardName,
                response);
            targetActor.tell(new Status.Failure(response instanceof Throwable throwable ? throwable
                    : new RuntimeException(String.format("Failed to find leader for shard %s: received response: %s",
                        shardName, response))), shardManagerActor);
        }
    }

    /**
     * The WrappedShardResponse class wraps a response from a Shard.
     */
    private static final class WrappedShardResponse {
        private final ShardIdentifier shardId;
        private final Object response;
        private final String leaderPath;

        WrappedShardResponse(final ShardIdentifier shardId, final Object response, final String leaderPath) {
            this.shardId = shardId;
            this.response = response;
            this.leaderPath = leaderPath;
        }

        ShardIdentifier getShardId() {
            return shardId;
        }

        Object getResponse() {
            return response;
        }

        String getLeaderPath() {
            return leaderPath;
        }
    }

    private static final class ShardNotInitializedTimeout {
        private final ActorRef sender;
        private final ShardInformation shardInfo;
        private final OnShardInitialized onShardInitialized;

        ShardNotInitializedTimeout(final ShardInformation shardInfo, final OnShardInitialized onShardInitialized,
            final ActorRef sender) {
            this.sender = sender;
            this.shardInfo = shardInfo;
            this.onShardInitialized = onShardInitialized;
        }

        ActorRef getSender() {
            return sender;
        }

        ShardInformation getShardInfo() {
            return shardInfo;
        }

        OnShardInitialized getOnShardInitialized() {
            return onShardInitialized;
        }
    }
}
