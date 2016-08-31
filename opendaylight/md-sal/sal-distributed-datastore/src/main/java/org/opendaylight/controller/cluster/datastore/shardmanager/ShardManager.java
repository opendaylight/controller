/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.shardmanager;

import static akka.pattern.Patterns.ask;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Cancellable;
import akka.actor.OneForOneStrategy;
import akka.actor.PoisonPill;
import akka.actor.Status;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberWeaklyUp;
import akka.cluster.Member;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.japi.Function;
import akka.pattern.Patterns;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotSelectionCriteria;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActorWithMetering;
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
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.FlipShardMembersVotingStatus;
import org.opendaylight.controller.cluster.datastore.messages.LocalPrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.RemoteFindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.RemotePrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.RemoveShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.ShardLeaderStateChanged;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.utils.Dispatchers;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
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
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * The ShardManager has the following jobs,
 * <ul>
 * <li> Create all the local shard replicas that belong on this cluster member
 * <li> Find the address of the local shard
 * <li> Find the primary replica for any given shard
 * <li> Monitor the cluster members and store their addresses
 * <ul>
 */
class ShardManager extends AbstractUntypedPersistentActorWithMetering {

    private static final Logger LOG = LoggerFactory.getLogger(ShardManager.class);

    // Stores a mapping between a shard name and it's corresponding information
    // Shard names look like inventory, topology etc and are as specified in
    // configuration
    private final Map<String, ShardInformation> localShards = new HashMap<>();

    // The type of a ShardManager reflects the type of the datastore itself
    // A data store could be of type config/operational
    private final String type;

    private final ClusterWrapper cluster;

    private final Configuration configuration;

    private final String shardDispatcherPath;

    private final ShardManagerInfo mBean;

    private DatastoreContextFactory datastoreContextFactory;

    private final CountDownLatch waitTillReadyCountdownLatch;

    private final PrimaryShardInfoFutureCache primaryShardInfoCache;

    private final ShardPeerAddressResolver peerAddressResolver;

    private SchemaContext schemaContext;

    private DatastoreSnapshot restoreFromSnapshot;

    private ShardManagerSnapshot currentSnapshot;

    private final Set<String> shardReplicaOperationsInProgress = new HashSet<>();

    private final String persistenceId;

    ShardManager(AbstractShardManagerCreator<?> builder) {
        this.cluster = builder.getCluster();
        this.configuration = builder.getConfiguration();
        this.datastoreContextFactory = builder.getDatastoreContextFactory();
        this.type = datastoreContextFactory.getBaseDatastoreContext().getDataStoreName();
        this.shardDispatcherPath =
                new Dispatchers(context().system().dispatchers()).getDispatcherPath(Dispatchers.DispatcherType.Shard);
        this.waitTillReadyCountdownLatch = builder.getWaitTillReadyCountdownLatch();
        this.primaryShardInfoCache = builder.getPrimaryShardInfoCache();
        this.restoreFromSnapshot = builder.getRestoreFromSnapshot();

        String possiblePersistenceId = datastoreContextFactory.getBaseDatastoreContext().getShardManagerPersistenceId();
        persistenceId = possiblePersistenceId != null ? possiblePersistenceId : "shard-manager-" + type;

        peerAddressResolver = new ShardPeerAddressResolver(type, cluster.getCurrentMemberName());

        // Subscribe this actor to cluster member events
        cluster.subscribeToMemberEvents(getSelf());

        mBean = new ShardManagerInfo(getSelf(), cluster.getCurrentMemberName(), "shard-manager-" + this.type,
                datastoreContextFactory.getBaseDatastoreContext().getDataStoreMXBeanType());
        mBean.registerMBean();
    }

    @Override
    public void postStop() {
        LOG.info("Stopping ShardManager {}", persistenceId());

        mBean.unregisterMBean();
    }

    @Override
    public void handleCommand(Object message) throws Exception {
        if (message  instanceof FindPrimary) {
            findPrimary((FindPrimary)message);
        } else if(message instanceof FindLocalShard){
            findLocalShard((FindLocalShard) message);
        } else if (message instanceof UpdateSchemaContext) {
            updateSchemaContext(message);
        } else if(message instanceof ActorInitialized) {
            onActorInitialized(message);
        } else if (message instanceof ClusterEvent.MemberUp){
            memberUp((ClusterEvent.MemberUp) message);
        } else if (message instanceof ClusterEvent.MemberWeaklyUp){
            memberWeaklyUp((ClusterEvent.MemberWeaklyUp) message);
        } else if (message instanceof ClusterEvent.MemberExited){
            memberExited((ClusterEvent.MemberExited) message);
        } else if(message instanceof ClusterEvent.MemberRemoved) {
            memberRemoved((ClusterEvent.MemberRemoved) message);
        } else if(message instanceof ClusterEvent.UnreachableMember) {
            memberUnreachable((ClusterEvent.UnreachableMember)message);
        } else if(message instanceof ClusterEvent.ReachableMember) {
            memberReachable((ClusterEvent.ReachableMember) message);
        } else if(message instanceof DatastoreContextFactory) {
            onDatastoreContextFactory((DatastoreContextFactory)message);
        } else if(message instanceof RoleChangeNotification) {
            onRoleChangeNotification((RoleChangeNotification) message);
        } else if(message instanceof FollowerInitialSyncUpStatus){
            onFollowerInitialSyncStatus((FollowerInitialSyncUpStatus) message);
        } else if(message instanceof ShardNotInitializedTimeout) {
            onShardNotInitializedTimeout((ShardNotInitializedTimeout)message);
        } else if(message instanceof ShardLeaderStateChanged) {
            onLeaderStateChanged((ShardLeaderStateChanged) message);
        } else if(message instanceof SwitchShardBehavior){
            onSwitchShardBehavior((SwitchShardBehavior) message);
        } else if(message instanceof CreateShard) {
            onCreateShard((CreateShard)message);
        } else if(message instanceof AddShardReplica){
            onAddShardReplica((AddShardReplica)message);
        } else if(message instanceof ForwardedAddServerReply) {
            ForwardedAddServerReply msg = (ForwardedAddServerReply)message;
            onAddServerReply(msg.shardInfo, msg.addServerReply, getSender(), msg.leaderPath,
                    msg.removeShardOnFailure);
        } else if(message instanceof ForwardedAddServerFailure) {
            ForwardedAddServerFailure msg = (ForwardedAddServerFailure)message;
            onAddServerFailure(msg.shardName, msg.failureMessage, msg.failure, getSender(), msg.removeShardOnFailure);
        } else if(message instanceof RemoveShardReplica) {
            onRemoveShardReplica((RemoveShardReplica) message);
        } else if(message instanceof WrappedShardResponse){
            onWrappedShardResponse((WrappedShardResponse) message);
        } else if(message instanceof GetSnapshot) {
            onGetSnapshot();
        } else if(message instanceof ServerRemoved){
            onShardReplicaRemoved((ServerRemoved) message);
        } else if(message instanceof ChangeShardMembersVotingStatus){
            onChangeShardServersVotingStatus((ChangeShardMembersVotingStatus) message);
        } else if(message instanceof FlipShardMembersVotingStatus){
            onFlipShardMembersVotingStatus((FlipShardMembersVotingStatus) message);
        } else if(message instanceof SaveSnapshotSuccess) {
            onSaveSnapshotSuccess((SaveSnapshotSuccess)message);
        } else if(message instanceof SaveSnapshotFailure) {
            LOG.error("{}: SaveSnapshotFailure received for saving snapshot of shards",
                    persistenceId(), ((SaveSnapshotFailure) message).cause());
        } else if(message instanceof Shutdown) {
            onShutDown();
        } else if (message instanceof GetLocalShardIds) {
            onGetLocalShardIds();
        } else if(message instanceof RunnableMessage) {
            ((RunnableMessage)message).run();
        } else {
            unknownMessage(message);
        }
    }

    private void onShutDown() {
        List<Future<Boolean>> stopFutures = new ArrayList<>(localShards.size());
        for (ShardInformation info : localShards.values()) {
            if (info.getActor() != null) {
                LOG.debug("{}: Issuing gracefulStop to shard {}", persistenceId(), info.getShardId());

                FiniteDuration duration = info.getDatastoreContext().getShardRaftConfig().getElectionTimeOutInterval().$times(2);
                stopFutures.add(Patterns.gracefulStop(info.getActor(), duration, Shutdown.INSTANCE));
            }
        }

        LOG.info("Shutting down ShardManager {} - waiting on {} shards", persistenceId(), stopFutures.size());

        ExecutionContext dispatcher = new Dispatchers(context().system().dispatchers()).getDispatcher(Dispatchers.DispatcherType.Client);
        Future<Iterable<Boolean>> combinedFutures = Futures.sequence(stopFutures, dispatcher);

        combinedFutures.onComplete(new OnComplete<Iterable<Boolean>>() {
            @Override
            public void onComplete(Throwable failure, Iterable<Boolean> results) {
                LOG.debug("{}: All shards shutdown - sending PoisonPill to self", persistenceId());

                self().tell(PoisonPill.getInstance(), self());

                if(failure != null) {
                    LOG.warn("{}: An error occurred attempting to shut down the shards", persistenceId(), failure);
                } else {
                    int nfailed = 0;
                    for(Boolean r: results) {
                        if(!r) {
                            nfailed++;
                        }
                    }

                    if(nfailed > 0) {
                        LOG.warn("{}: {} shards did not shut down gracefully", persistenceId(), nfailed);
                    }
                }
            }
        }, dispatcher);
    }

    private void onWrappedShardResponse(WrappedShardResponse message) {
        if (message.getResponse() instanceof RemoveServerReply) {
            onRemoveServerReply(getSender(), message.getShardId(), (RemoveServerReply) message.getResponse(),
                    message.getLeaderPath());
        }
    }

    private void onRemoveServerReply(ActorRef originalSender, ShardIdentifier shardId, RemoveServerReply replyMsg,
            String leaderPath) {
        shardReplicaOperationsInProgress.remove(shardId.getShardName());

        LOG.debug ("{}: Received {} for shard {}", persistenceId(), replyMsg, shardId.getShardName());

        if (replyMsg.getStatus() == ServerChangeStatus.OK) {
            LOG.debug ("{}: Leader shard successfully removed the replica shard {}", persistenceId(),
                    shardId.getShardName());
            originalSender.tell(new Status.Success(null), getSelf());
        } else {
            LOG.warn ("{}: Leader failed to remove shard replica {} with status {}",
                    persistenceId(), shardId, replyMsg.getStatus());

            Exception failure = getServerChangeException(RemoveServer.class, replyMsg.getStatus(), leaderPath, shardId);
            originalSender.tell(new Status.Failure(failure), getSelf());
        }
    }

    private void removeShardReplica(RemoveShardReplica contextMessage, final String shardName, final String primaryPath,
            final ActorRef sender) {
        if(isShardReplicaOperationInProgress(shardName, sender)) {
            return;
        }

        shardReplicaOperationsInProgress.add(shardName);

        final ShardIdentifier shardId = getShardIdentifier(contextMessage.getMemberName(), shardName);

        final DatastoreContext datastoreContext = newShardDatastoreContextBuilder(shardName).build();

        //inform ShardLeader to remove this shard as a replica by sending an RemoveServer message
        LOG.debug ("{}: Sending RemoveServer message to peer {} for shard {}", persistenceId(),
                primaryPath, shardId);

        Timeout removeServerTimeout = new Timeout(datastoreContext.getShardLeaderElectionTimeout().
                duration());
        Future<Object> futureObj = ask(getContext().actorSelection(primaryPath),
                new RemoveServer(shardId.toString()), removeServerTimeout);

        futureObj.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) {
                if (failure != null) {
                    shardReplicaOperationsInProgress.remove(shardName);
                    String msg = String.format("RemoveServer request to leader %s for shard %s failed",
                            primaryPath, shardName);

                    LOG.debug ("{}: {}", persistenceId(), msg, failure);

                    // FAILURE
                    sender.tell(new Status.Failure(new RuntimeException(msg, failure)), self());
                } else {
                    // SUCCESS
                    self().tell(new WrappedShardResponse(shardId, response, primaryPath), sender);
                }
            }
        }, new Dispatchers(context().system().dispatchers()).getDispatcher(Dispatchers.DispatcherType.Client));
    }

    private void onShardReplicaRemoved(ServerRemoved message) {
        final ShardIdentifier shardId = new ShardIdentifier.Builder().fromShardIdString(message.getServerId()).build();
        final ShardInformation shardInformation = localShards.remove(shardId.getShardName());
        if(shardInformation == null) {
            LOG.debug("{} : Shard replica {} is not present in list", persistenceId(), shardId.toString());
            return;
        } else if(shardInformation.getActor() != null) {
            LOG.debug("{} : Sending Shutdown to Shard actor {}", persistenceId(), shardInformation.getActor());
            shardInformation.getActor().tell(Shutdown.INSTANCE, self());
        }
        LOG.debug("{} : Local Shard replica for shard {} has been removed", persistenceId(), shardId.getShardName());
        persistShardList();
    }

    private void onGetSnapshot() {
        LOG.debug("{}: onGetSnapshot", persistenceId());

        List<String> notInitialized = null;
        for(ShardInformation shardInfo: localShards.values()) {
            if(!shardInfo.isShardInitialized()) {
                if(notInitialized == null) {
                    notInitialized = new ArrayList<>();
                }

                notInitialized.add(shardInfo.getShardName());
            }
        }

        if(notInitialized != null) {
            getSender().tell(new Status.Failure(new IllegalStateException(String.format(
                    "%d shard(s) %s are not initialized", notInitialized.size(), notInitialized))), getSelf());
            return;
        }

        byte[] shardManagerSnapshot = null;
        if(currentSnapshot != null) {
            shardManagerSnapshot = SerializationUtils.serialize(currentSnapshot);
        }

        ActorRef replyActor = getContext().actorOf(ShardManagerGetSnapshotReplyActor.props(
                new ArrayList<>(localShards.keySet()), type, shardManagerSnapshot , getSender(), persistenceId(),
                datastoreContextFactory.getBaseDatastoreContext().getShardInitializationTimeout().duration()));

        for(ShardInformation shardInfo: localShards.values()) {
            shardInfo.getActor().tell(GetSnapshot.INSTANCE, replyActor);
        }
    }

    private void onCreateShard(CreateShard createShard) {
        LOG.debug("{}: onCreateShard: {}", persistenceId(), createShard);

        Object reply;
        try {
            String shardName = createShard.getModuleShardConfig().getShardName();
            if(localShards.containsKey(shardName)) {
                LOG.debug("{}: Shard {} already exists", persistenceId(), shardName);
                reply = new Status.Success(String.format("Shard with name %s already exists", shardName));
            } else {
                doCreateShard(createShard);
                reply = new Status.Success(null);
            }
        } catch (Exception e) {
            LOG.error("{}: onCreateShard failed", persistenceId(), e);
            reply = new Status.Failure(e);
        }

        if(getSender() != null && !getContext().system().deadLetters().equals(getSender())) {
            getSender().tell(reply, getSelf());
        }
    }

    private void doCreateShard(CreateShard createShard) {
        ModuleShardConfiguration moduleShardConfig = createShard.getModuleShardConfig();
        String shardName = moduleShardConfig.getShardName();

        configuration.addModuleShardConfiguration(moduleShardConfig);

        DatastoreContext shardDatastoreContext = createShard.getDatastoreContext();
        if(shardDatastoreContext == null) {
            shardDatastoreContext = newShardDatastoreContext(shardName);
        } else {
            shardDatastoreContext = DatastoreContext.newBuilderFrom(shardDatastoreContext).shardPeerAddressResolver(
                    peerAddressResolver).build();
        }

        ShardIdentifier shardId = getShardIdentifier(cluster.getCurrentMemberName(), shardName);

        boolean shardWasInRecoveredSnapshot = currentSnapshot != null &&
                currentSnapshot.getShardList().contains(shardName);

        Map<String, String> peerAddresses;
        boolean isActiveMember;
        if(shardWasInRecoveredSnapshot || configuration.getMembersFromShardName(shardName).
                contains(cluster.getCurrentMemberName())) {
            peerAddresses = getPeerAddresses(shardName);
            isActiveMember = true;
        } else {
            // The local member is not in the static shard member configuration and the shard did not
            // previously exist (ie !shardWasInRecoveredSnapshot). In this case we'll create
            // the shard with no peers and with elections disabled so it stays as follower. A
            // subsequent AddServer request will be needed to make it an active member.
            isActiveMember = false;
            peerAddresses = Collections.emptyMap();
            shardDatastoreContext = DatastoreContext.newBuilderFrom(shardDatastoreContext).
                    customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName()).build();
        }

        LOG.debug("{} doCreateShard: shardId: {}, memberNames: {}, peerAddresses: {}, isActiveMember: {}",
                persistenceId(), shardId, moduleShardConfig.getShardMemberNames(), peerAddresses,
                isActiveMember);

        ShardInformation info = new ShardInformation(shardName, shardId, peerAddresses,
                shardDatastoreContext, createShard.getShardBuilder(), peerAddressResolver);
        info.setActiveMember(isActiveMember);
        localShards.put(info.getShardName(), info);

        if(schemaContext != null) {
            info.setActor(newShardActor(schemaContext, info));
        }
    }

    private DatastoreContext.Builder newShardDatastoreContextBuilder(String shardName) {
        return DatastoreContext.newBuilderFrom(datastoreContextFactory.getShardDatastoreContext(shardName)).
                shardPeerAddressResolver(peerAddressResolver);
    }

    private DatastoreContext newShardDatastoreContext(String shardName) {
        return newShardDatastoreContextBuilder(shardName).build();
    }

    private void checkReady(){
        if (isReadyWithLeaderId()) {
            LOG.info("{}: All Shards are ready - data store {} is ready, available count is {}",
                    persistenceId(), type, waitTillReadyCountdownLatch.getCount());

            waitTillReadyCountdownLatch.countDown();
        }
    }

    private void onLeaderStateChanged(ShardLeaderStateChanged leaderStateChanged) {
        LOG.info("{}: Received LeaderStateChanged message: {}", persistenceId(), leaderStateChanged);

        ShardInformation shardInformation = findShardInformation(leaderStateChanged.getMemberId());
        if(shardInformation != null) {
            shardInformation.setLocalDataTree(leaderStateChanged.getLocalShardDataTree());
            shardInformation.setLeaderVersion(leaderStateChanged.getLeaderPayloadVersion());
            if(shardInformation.setLeaderId(leaderStateChanged.getLeaderId())) {
                primaryShardInfoCache.remove(shardInformation.getShardName());
            }

            checkReady();
        } else {
            LOG.debug("No shard found with member Id {}", leaderStateChanged.getMemberId());
        }
    }

    private void onShardNotInitializedTimeout(ShardNotInitializedTimeout message) {
        ShardInformation shardInfo = message.getShardInfo();

        LOG.debug("{}: Received ShardNotInitializedTimeout message for shard {}", persistenceId(),
                shardInfo.getShardName());

        shardInfo.removeOnShardInitialized(message.getOnShardInitialized());

        if(!shardInfo.isShardInitialized()) {
            LOG.debug("{}: Returning NotInitializedException for shard {}", persistenceId(), shardInfo.getShardName());
            message.getSender().tell(createNotInitializedException(shardInfo.getShardId()), getSelf());
        } else {
            LOG.debug("{}: Returning NoShardLeaderException for shard {}", persistenceId(), shardInfo.getShardName());
            message.getSender().tell(createNoShardLeaderException(shardInfo.getShardId()), getSelf());
        }
    }

    private void onFollowerInitialSyncStatus(FollowerInitialSyncUpStatus status) {
        LOG.info("{} Received follower initial sync status for {} status sync done {}", persistenceId(),
                status.getName(), status.isInitialSyncDone());

        ShardInformation shardInformation = findShardInformation(status.getName());

        if(shardInformation != null) {
            shardInformation.setFollowerSyncStatus(status.isInitialSyncDone());

            mBean.setSyncStatus(isInSync());
        }

    }

    private void onRoleChangeNotification(RoleChangeNotification roleChanged) {
        LOG.info("{}: Received role changed for {} from {} to {}", persistenceId(), roleChanged.getMemberId(),
                roleChanged.getOldRole(), roleChanged.getNewRole());

        ShardInformation shardInformation = findShardInformation(roleChanged.getMemberId());
        if(shardInformation != null) {
            shardInformation.setRole(roleChanged.getNewRole());
            checkReady();
            mBean.setSyncStatus(isInSync());
        }
    }


    private ShardInformation findShardInformation(String memberId) {
        for(ShardInformation info : localShards.values()){
            if(info.getShardId().toString().equals(memberId)){
                return info;
            }
        }

        return null;
    }

    private boolean isReadyWithLeaderId() {
        boolean isReady = true;
        for (ShardInformation info : localShards.values()) {
            if(!info.isShardReadyWithLeaderId()){
                isReady = false;
                break;
            }
        }
        return isReady;
    }

    private boolean isInSync(){
        for (ShardInformation info : localShards.values()) {
            if(!info.isInSync()){
                return false;
            }
        }
        return true;
    }

    private void onActorInitialized(Object message) {
        final ActorRef sender = getSender();

        if (sender == null) {
            return; //why is a non-actor sending this message? Just ignore.
        }

        String actorName = sender.path().name();
        //find shard name from actor name; actor name is stringified shardId

        final ShardIdentifier shardId;
        try {
            shardId = ShardIdentifier.fromShardIdString(actorName);
        } catch (IllegalArgumentException e) {
            LOG.debug("{}: ignoring actor {}", actorName, e);
            return;
        }

        markShardAsInitialized(shardId.getShardName());
    }

    private void markShardAsInitialized(String shardName) {
        LOG.debug("{}: Initializing shard [{}]", persistenceId(), shardName);

        ShardInformation shardInformation = localShards.get(shardName);
        if (shardInformation != null) {
            shardInformation.setActorInitialized();

            shardInformation.getActor().tell(new RegisterRoleChangeListener(), self());
        }
    }

    @Override
    protected void handleRecover(Object message) throws Exception {
        if (message instanceof RecoveryCompleted) {
            onRecoveryCompleted();
        } else if (message instanceof SnapshotOffer) {
            applyShardManagerSnapshot((ShardManagerSnapshot)((SnapshotOffer) message).snapshot());
        }
    }

    private void onRecoveryCompleted() {
        LOG.info("Recovery complete : {}", persistenceId());

        // We no longer persist SchemaContext modules so delete all the prior messages from the akka
        // journal on upgrade from Helium.
        deleteMessages(lastSequenceNr());

        if(currentSnapshot == null && restoreFromSnapshot != null &&
                restoreFromSnapshot.getShardManagerSnapshot() != null) {
            try(ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                    restoreFromSnapshot.getShardManagerSnapshot()))) {
                ShardManagerSnapshot snapshot = (ShardManagerSnapshot) ois.readObject();

                LOG.debug("{}: Deserialized restored ShardManagerSnapshot: {}", persistenceId(), snapshot);

                applyShardManagerSnapshot(snapshot);
            } catch(Exception e) {
                LOG.error("{}: Error deserializing restored ShardManagerSnapshot", persistenceId(), e);
            }
        }

        createLocalShards();
    }

    private void findLocalShard(FindLocalShard message) {
        final ShardInformation shardInformation = localShards.get(message.getShardName());

        if(shardInformation == null){
            getSender().tell(new LocalShardNotFound(message.getShardName()), getSelf());
            return;
        }

        sendResponse(shardInformation, message.isWaitUntilInitialized(), false, () -> new LocalShardFound(shardInformation.getActor()));
    }

    private void sendResponse(ShardInformation shardInformation, boolean doWait,
            boolean wantShardReady, final Supplier<Object> messageSupplier) {
        if (!shardInformation.isShardInitialized() || (wantShardReady && !shardInformation.isShardReadyWithLeaderId())) {
            if(doWait) {
                final ActorRef sender = getSender();
                final ActorRef self = self();

                Runnable replyRunnable = () -> sender.tell(messageSupplier.get(), self);

                OnShardInitialized onShardInitialized = wantShardReady ? new OnShardReady(replyRunnable) :
                    new OnShardInitialized(replyRunnable);

                shardInformation.addOnShardInitialized(onShardInitialized);

                FiniteDuration timeout = shardInformation.getDatastoreContext().getShardInitializationTimeout().duration();
                if(shardInformation.isShardInitialized()) {
                    // If the shard is already initialized then we'll wait enough time for the shard to
                    // elect a leader, ie 2 times the election timeout.
                    timeout = FiniteDuration.create(shardInformation.getDatastoreContext().getShardRaftConfig()
                            .getElectionTimeOutInterval().toMillis() * 2, TimeUnit.MILLISECONDS);
                }

                LOG.debug("{}: Scheduling {} ms timer to wait for shard {}", persistenceId(), timeout.toMillis(),
                        shardInformation.getShardName());

                Cancellable timeoutSchedule = getContext().system().scheduler().scheduleOnce(
                        timeout, getSelf(),
                        new ShardNotInitializedTimeout(shardInformation, onShardInitialized, sender),
                        getContext().dispatcher(), getSelf());

                onShardInitialized.setTimeoutSchedule(timeoutSchedule);

            } else if (!shardInformation.isShardInitialized()) {
                LOG.debug("{}: Returning NotInitializedException for shard {}", persistenceId(),
                        shardInformation.getShardName());
                getSender().tell(createNotInitializedException(shardInformation.getShardId()), getSelf());
            } else {
                LOG.debug("{}: Returning NoShardLeaderException for shard {}", persistenceId(),
                        shardInformation.getShardName());
                getSender().tell(createNoShardLeaderException(shardInformation.getShardId()), getSelf());
            }

            return;
        }

        getSender().tell(messageSupplier.get(), getSelf());
    }

    private static NoShardLeaderException createNoShardLeaderException(ShardIdentifier shardId) {
        return new NoShardLeaderException(null, shardId.toString());
    }

    private static NotInitializedException createNotInitializedException(ShardIdentifier shardId) {
        return new NotInitializedException(String.format(
                "Found primary shard %s but it's not initialized yet. Please try again later", shardId));
    }

    @VisibleForTesting
    static MemberName memberToName(final Member member) {
        return MemberName.forName(member.roles().iterator().next());
    }

    private void memberRemoved(ClusterEvent.MemberRemoved message) {
        MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberRemoved: memberName: {}, address: {}", persistenceId(), memberName,
                message.member().address());

        peerAddressResolver.removePeerAddress(memberName);

        for(ShardInformation info : localShards.values()){
            info.peerDown(memberName, getShardIdentifier(memberName, info.getShardName()).toString(), getSelf());
        }
    }

    private void memberExited(ClusterEvent.MemberExited message) {
        MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberExited: memberName: {}, address: {}", persistenceId(), memberName,
                message.member().address());

        peerAddressResolver.removePeerAddress(memberName);

        for(ShardInformation info : localShards.values()){
            info.peerDown(memberName, getShardIdentifier(memberName, info.getShardName()).toString(), getSelf());
        }
    }

    private void memberUp(ClusterEvent.MemberUp message) {
        MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberUp: memberName: {}, address: {}", persistenceId(), memberName,
                message.member().address());

        memberUp(memberName, message.member().address());
    }

    private void memberUp(MemberName memberName, Address address) {
        addPeerAddress(memberName, address);
        checkReady();
    }

    private void memberWeaklyUp(MemberWeaklyUp message) {
        MemberName memberName = memberToName(message.member());

        LOG.info("{}: Received MemberWeaklyUp: memberName: {}, address: {}", persistenceId(), memberName,
                message.member().address());

        memberUp(memberName, message.member().address());
    }

    private void addPeerAddress(MemberName memberName, Address address) {
        peerAddressResolver.addPeerAddress(memberName, address);

        for(ShardInformation info : localShards.values()){
            String shardName = info.getShardName();
            String peerId = getShardIdentifier(memberName, shardName).toString();
            info.updatePeerAddress(peerId, peerAddressResolver.getShardActorAddress(shardName, memberName), getSelf());

            info.peerUp(memberName, peerId, getSelf());
        }
    }

    private void memberReachable(ClusterEvent.ReachableMember message) {
        MemberName memberName = memberToName(message.member());
        LOG.info("Received ReachableMember: memberName {}, address: {}", memberName, message.member().address());

        addPeerAddress(memberName, message.member().address());

        markMemberAvailable(memberName);
    }

    private void memberUnreachable(ClusterEvent.UnreachableMember message) {
        MemberName memberName = memberToName(message.member());
        LOG.info("Received UnreachableMember: memberName {}, address: {}", memberName, message.member().address());

        markMemberUnavailable(memberName);
    }

    private void markMemberUnavailable(final MemberName memberName) {
        final String memberStr = memberName.getName();
        for (ShardInformation info : localShards.values()) {
            String leaderId = info.getLeaderId();
            // XXX: why are we using String#contains() here?
            if (leaderId != null && leaderId.contains(memberStr)) {
                LOG.debug("Marking Leader {} as unavailable.", leaderId);
                info.setLeaderAvailable(false);

                primaryShardInfoCache.remove(info.getShardName());
            }

            info.peerDown(memberName, getShardIdentifier(memberName, info.getShardName()).toString(), getSelf());
        }
    }

    private void markMemberAvailable(final MemberName memberName) {
        final String memberStr = memberName.getName();
        for (ShardInformation info : localShards.values()) {
            String leaderId = info.getLeaderId();
            // XXX: why are we using String#contains() here?
            if (leaderId != null && leaderId.contains(memberStr)) {
                LOG.debug("Marking Leader {} as available.", leaderId);
                info.setLeaderAvailable(true);
            }

            info.peerUp(memberName, getShardIdentifier(memberName, info.getShardName()).toString(), getSelf());
        }
    }

    private void onDatastoreContextFactory(DatastoreContextFactory factory) {
        datastoreContextFactory = factory;
        for (ShardInformation info : localShards.values()) {
            info.setDatastoreContext(newShardDatastoreContext(info.getShardName()), getSelf());
        }
    }

    private void onGetLocalShardIds() {
        final List<String> response = new ArrayList<>(localShards.size());

        for (ShardInformation info : localShards.values()) {
            response.add(info.getShardId().toString());
        }

        getSender().tell(new Status.Success(response), getSelf());
    }

    private void onSwitchShardBehavior(final SwitchShardBehavior message) {
        final ShardIdentifier identifier = message.getShardId();

        if (identifier != null) {
            final ShardInformation info = localShards.get(identifier.getShardName());
            if (info == null) {
                getSender().tell(new Status.Failure(
                    new IllegalArgumentException("Shard " + identifier + " is not local")), getSelf());
                return;
            }

            switchShardBehavior(info, new SwitchBehavior(message.getNewState(), message.getTerm()));
        } else {
            for (ShardInformation info : localShards.values()) {
                switchShardBehavior(info, new SwitchBehavior(message.getNewState(), message.getTerm()));
            }
        }

        getSender().tell(new Status.Success(null), getSelf());
    }

    private void switchShardBehavior(final ShardInformation info, final SwitchBehavior switchBehavior) {
        final ActorRef actor = info.getActor();
        if (actor != null) {
            actor.tell(switchBehavior, getSelf());
          } else {
            LOG.warn("Could not switch the behavior of shard {} to {} - shard is not yet available",
                info.getShardName(), switchBehavior.getNewState());
        }
    }

    /**
     * Notifies all the local shards of a change in the schema context
     *
     * @param message
     */
    private void updateSchemaContext(final Object message) {
        schemaContext = ((UpdateSchemaContext) message).getSchemaContext();

        LOG.debug("Got updated SchemaContext: # of modules {}", schemaContext.getAllModuleIdentifiers().size());

        for (ShardInformation info : localShards.values()) {
            if (info.getActor() == null) {
                LOG.debug("Creating Shard {}", info.getShardId());
                info.setActor(newShardActor(schemaContext, info));
            } else {
                info.getActor().tell(message, getSelf());
            }
        }
    }

    @VisibleForTesting
    protected ClusterWrapper getCluster() {
        return cluster;
    }

    @VisibleForTesting
    protected ActorRef newShardActor(final SchemaContext schemaContext, ShardInformation info) {
        return getContext().actorOf(info.newProps(schemaContext)
                .withDispatcher(shardDispatcherPath), info.getShardId().toString());
    }

    private void findPrimary(FindPrimary message) {
        LOG.debug("{}: In findPrimary: {}", persistenceId(), message);

        final String shardName = message.getShardName();
        final boolean canReturnLocalShardState = !(message instanceof RemoteFindPrimary);

        // First see if the there is a local replica for the shard
        final ShardInformation info = localShards.get(shardName);
        if (info != null && info.isActiveMember()) {
            sendResponse(info, message.isWaitUntilReady(), true, () -> {
                String primaryPath = info.getSerializedLeaderActor();
                Object found = canReturnLocalShardState && info.isLeader() ?
                        new LocalPrimaryShardFound(primaryPath, info.getLocalShardDataTree().get()) :
                            new RemotePrimaryShardFound(primaryPath, info.getLeaderVersion());

                        if(LOG.isDebugEnabled()) {
                            LOG.debug("{}: Found primary for {}: {}", persistenceId(), shardName, found);
                        }

                        return found;
            });

            return;
        }

        final Collection<String> visitedAddresses;
        if(message instanceof RemoteFindPrimary) {
            visitedAddresses = ((RemoteFindPrimary)message).getVisitedAddresses();
        } else {
            visitedAddresses = new ArrayList<>(1);
        }

        visitedAddresses.add(peerAddressResolver.getShardManagerActorPathBuilder(cluster.getSelfAddress()).toString());

        for(String address: peerAddressResolver.getShardManagerPeerActorAddresses()) {
            if(visitedAddresses.contains(address)) {
                continue;
            }

            LOG.debug("{}: findPrimary for {} forwarding to remote ShardManager {}, visitedAddresses: {}",
                    persistenceId(), shardName, address, visitedAddresses);

            getContext().actorSelection(address).forward(new RemoteFindPrimary(shardName,
                    message.isWaitUntilReady(), visitedAddresses), getContext());
            return;
        }

        LOG.debug("{}: No shard found for {}", persistenceId(), shardName);

        getSender().tell(new PrimaryNotFoundException(
                String.format("No primary shard found for %s.", shardName)), getSelf());
    }

    /**
     * Construct the name of the shard actor given the name of the member on
     * which the shard resides and the name of the shard
     *
     * @param memberName
     * @param shardName
     * @return
     */
    private ShardIdentifier getShardIdentifier(MemberName memberName, String shardName){
        return peerAddressResolver.getShardIdentifier(memberName, shardName);
    }

    /**
     * Create shards that are local to the member on which the ShardManager
     * runs
     *
     */
    private void createLocalShards() {
        MemberName memberName = this.cluster.getCurrentMemberName();
        Collection<String> memberShardNames = this.configuration.getMemberShardNames(memberName);

        Map<String, DatastoreSnapshot.ShardSnapshot> shardSnapshots = new HashMap<>();
        if(restoreFromSnapshot != null)
        {
            for(DatastoreSnapshot.ShardSnapshot snapshot: restoreFromSnapshot.getShardSnapshots()) {
                shardSnapshots.put(snapshot.getName(), snapshot);
            }
        }

        restoreFromSnapshot = null; // null out to GC

        for(String shardName : memberShardNames){
            ShardIdentifier shardId = getShardIdentifier(memberName, shardName);

            LOG.debug("{}: Creating local shard: {}", persistenceId(), shardId);

            Map<String, String> peerAddresses = getPeerAddresses(shardName);
            localShards.put(shardName, new ShardInformation(shardName, shardId, peerAddresses,
                    newShardDatastoreContext(shardName), Shard.builder().restoreFromSnapshot(
                        shardSnapshots.get(shardName)), peerAddressResolver));
        }
    }

    /**
     * Given the name of the shard find the addresses of all it's peers
     *
     * @param shardName
     */
    private Map<String, String> getPeerAddresses(String shardName) {
        Collection<MemberName> members = configuration.getMembersFromShardName(shardName);
        Map<String, String> peerAddresses = new HashMap<>();

        MemberName currentMemberName = this.cluster.getCurrentMemberName();

        for (MemberName memberName : members) {
            if (!currentMemberName.equals(memberName)) {
                ShardIdentifier shardId = getShardIdentifier(memberName, shardName);
                String address = peerAddressResolver.getShardActorAddress(shardName, memberName);
                peerAddresses.put(shardId.toString(), address);
            }
        }
        return peerAddresses;
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {

        return new OneForOneStrategy(10, Duration.create("1 minute"),
                (Function<Throwable, Directive>) t -> {
                    LOG.warn("Supervisor Strategy caught unexpected exception - resuming", t);
                    return SupervisorStrategy.resume();
                }
                );

    }

    @Override
    public String persistenceId() {
        return persistenceId;
    }

    @VisibleForTesting
    ShardManagerInfoMBean getMBean(){
        return mBean;
    }

    private boolean isShardReplicaOperationInProgress(final String shardName, final ActorRef sender) {
        if (shardReplicaOperationsInProgress.contains(shardName)) {
            String msg = String.format("A shard replica operation for %s is already in progress", shardName);
            LOG.debug ("{}: {}", persistenceId(), msg);
            sender.tell(new Status.Failure(new IllegalStateException(msg)), getSelf());
            return true;
        }

        return false;
    }

    private void onAddShardReplica (final AddShardReplica shardReplicaMsg) {
        final String shardName = shardReplicaMsg.getShardName();

        LOG.debug("{}: onAddShardReplica: {}", persistenceId(), shardReplicaMsg);

        // verify the shard with the specified name is present in the cluster configuration
        if (!(this.configuration.isShardConfigured(shardName))) {
            String msg = String.format("No module configuration exists for shard %s", shardName);
            LOG.debug ("{}: {}", persistenceId(), msg);
            getSender().tell(new Status.Failure(new IllegalArgumentException(msg)), getSelf());
            return;
        }

        // Create the localShard
        if (schemaContext == null) {
            String msg = String.format(
                  "No SchemaContext is available in order to create a local shard instance for %s", shardName);
            LOG.debug ("{}: {}", persistenceId(), msg);
            getSender().tell(new Status.Failure(new IllegalStateException(msg)), getSelf());
            return;
        }

        findPrimary(shardName, new AutoFindPrimaryFailureResponseHandler(getSender(), shardName, persistenceId(), getSelf()) {
            @Override
            public void onRemotePrimaryShardFound(RemotePrimaryShardFound response) {
                getSelf().tell((RunnableMessage) () -> addShard(getShardName(), response, getSender()), getTargetActor());
            }

            @Override
            public void onLocalPrimaryFound(LocalPrimaryShardFound message) {
                sendLocalReplicaAlreadyExistsReply(getShardName(), getTargetActor());
            }

        });
    }

    private void sendLocalReplicaAlreadyExistsReply(String shardName, ActorRef sender) {
        String msg = String.format("Local shard %s already exists", shardName);
        LOG.debug ("{}: {}", persistenceId(), msg);
        sender.tell(new Status.Failure(new AlreadyExistsException(msg)), getSelf());
    }

    private void addShard(final String shardName, final RemotePrimaryShardFound response, final ActorRef sender) {
        if(isShardReplicaOperationInProgress(shardName, sender)) {
            return;
        }

        shardReplicaOperationsInProgress.add(shardName);

        final ShardInformation shardInfo;
        final boolean removeShardOnFailure;
        ShardInformation existingShardInfo = localShards.get(shardName);
        if(existingShardInfo == null) {
            removeShardOnFailure = true;
            ShardIdentifier shardId = getShardIdentifier(cluster.getCurrentMemberName(), shardName);

            DatastoreContext datastoreContext = newShardDatastoreContextBuilder(shardName).customRaftPolicyImplementation(
                    DisableElectionsRaftPolicy.class.getName()).build();

            shardInfo = new ShardInformation(shardName, shardId, getPeerAddresses(shardName), datastoreContext,
                    Shard.builder(), peerAddressResolver);
            shardInfo.setActiveMember(false);
            localShards.put(shardName, shardInfo);
            shardInfo.setActor(newShardActor(schemaContext, shardInfo));
        } else {
            removeShardOnFailure = false;
            shardInfo = existingShardInfo;
        }

        String localShardAddress = peerAddressResolver.getShardActorAddress(shardName, cluster.getCurrentMemberName());

        //inform ShardLeader to add this shard as a replica by sending an AddServer message
        LOG.debug ("{}: Sending AddServer message to peer {} for shard {}", persistenceId(),
                response.getPrimaryPath(), shardInfo.getShardId());

        Timeout addServerTimeout = new Timeout(shardInfo.getDatastoreContext().getShardLeaderElectionTimeout().
                duration());
        Future<Object> futureObj = ask(getContext().actorSelection(response.getPrimaryPath()),
            new AddServer(shardInfo.getShardId().toString(), localShardAddress, true), addServerTimeout);

        futureObj.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object addServerResponse) {
                if (failure != null) {
                    LOG.debug ("{}: AddServer request to {} for {} failed", persistenceId(),
                            response.getPrimaryPath(), shardName, failure);

                    String msg = String.format("AddServer request to leader %s for shard %s failed",
                            response.getPrimaryPath(), shardName);
                    self().tell(new ForwardedAddServerFailure(shardName, msg, failure, removeShardOnFailure), sender);
                } else {
                    self().tell(new ForwardedAddServerReply(shardInfo, (AddServerReply)addServerResponse,
                            response.getPrimaryPath(), removeShardOnFailure), sender);
                }
            }
        }, new Dispatchers(context().system().dispatchers()).getDispatcher(Dispatchers.DispatcherType.Client));
    }

    private void onAddServerFailure(String shardName, String message, Throwable failure, ActorRef sender,
            boolean removeShardOnFailure) {
        shardReplicaOperationsInProgress.remove(shardName);

        if(removeShardOnFailure) {
            ShardInformation shardInfo = localShards.remove(shardName);
            if (shardInfo.getActor() != null) {
                shardInfo.getActor().tell(PoisonPill.getInstance(), getSelf());
            }
        }

        sender.tell(new Status.Failure(message == null ? failure :
            new RuntimeException(message, failure)), getSelf());
    }

    private void onAddServerReply(ShardInformation shardInfo, AddServerReply replyMsg, ActorRef sender,
            String leaderPath, boolean removeShardOnFailure) {
        String shardName = shardInfo.getShardName();
        shardReplicaOperationsInProgress.remove(shardName);

        LOG.debug ("{}: Received {} for shard {} from leader {}", persistenceId(), replyMsg, shardName, leaderPath);

        if (replyMsg.getStatus() == ServerChangeStatus.OK) {
            LOG.debug ("{}: Leader shard successfully added the replica shard {}", persistenceId(), shardName);

            // Make the local shard voting capable
            shardInfo.setDatastoreContext(newShardDatastoreContext(shardName), getSelf());
            shardInfo.setActiveMember(true);
            persistShardList();

            sender.tell(new Status.Success(null), getSelf());
        } else if(replyMsg.getStatus() == ServerChangeStatus.ALREADY_EXISTS) {
            sendLocalReplicaAlreadyExistsReply(shardName, sender);
        } else {
            LOG.warn ("{}: Leader failed to add shard replica {} with status {}",
                    persistenceId(), shardName, replyMsg.getStatus());

            Exception failure = getServerChangeException(AddServer.class, replyMsg.getStatus(), leaderPath, shardInfo.getShardId());

            onAddServerFailure(shardName, null, failure, sender, removeShardOnFailure);
        }
    }

    private static Exception getServerChangeException(Class<?> serverChange, ServerChangeStatus serverChangeStatus,
                                               String leaderPath, ShardIdentifier shardId) {
        Exception failure;
        switch (serverChangeStatus) {
            case TIMEOUT:
                failure = new TimeoutException(String.format(
                        "The shard leader %s timed out trying to replicate the initial data to the new shard %s." +
                        "Possible causes - there was a problem replicating the data or shard leadership changed while replicating the shard data",
                        leaderPath, shardId.getShardName()));
                break;
            case NO_LEADER:
                failure = createNoShardLeaderException(shardId);
                break;
            case NOT_SUPPORTED:
                failure = new UnsupportedOperationException(String.format("%s request is not supported for shard %s",
                        serverChange.getSimpleName(), shardId.getShardName()));
                break;
            default :
                failure = new RuntimeException(String.format(
                        "%s request to leader %s for shard %s failed with status %s",
                        serverChange.getSimpleName(), leaderPath, shardId.getShardName(), serverChangeStatus));
        }
        return failure;
    }

    private void onRemoveShardReplica (final RemoveShardReplica shardReplicaMsg) {
        LOG.debug("{}: onRemoveShardReplica: {}", persistenceId(), shardReplicaMsg);

        findPrimary(shardReplicaMsg.getShardName(), new AutoFindPrimaryFailureResponseHandler(getSender(),
                shardReplicaMsg.getShardName(), persistenceId(), getSelf()) {
            @Override
            public void onRemotePrimaryShardFound(RemotePrimaryShardFound response) {
                doRemoveShardReplicaAsync(response.getPrimaryPath());
            }

            @Override
            public void onLocalPrimaryFound(LocalPrimaryShardFound response) {
                doRemoveShardReplicaAsync(response.getPrimaryPath());
            }

            private void doRemoveShardReplicaAsync(final String primaryPath) {
                getSelf().tell((RunnableMessage) () -> removeShardReplica(shardReplicaMsg, getShardName(), primaryPath, getSender()), getTargetActor());
            }
        });
    }

    private void persistShardList() {
        List<String> shardList = new ArrayList<>(localShards.keySet());
        for (ShardInformation shardInfo : localShards.values()) {
            if (!shardInfo.isActiveMember()) {
                shardList.remove(shardInfo.getShardName());
            }
        }
        LOG.debug ("{}: persisting the shard list {}", persistenceId(), shardList);
        saveSnapshot(updateShardManagerSnapshot(shardList));
    }

    private ShardManagerSnapshot updateShardManagerSnapshot(List<String> shardList) {
        currentSnapshot = new ShardManagerSnapshot(shardList);
        return currentSnapshot;
    }

    private void applyShardManagerSnapshot(ShardManagerSnapshot snapshot) {
        currentSnapshot = snapshot;

        LOG.debug ("{}: onSnapshotOffer: {}", persistenceId(), currentSnapshot);

        final MemberName currentMember = cluster.getCurrentMemberName();
        Set<String> configuredShardList =
            new HashSet<>(configuration.getMemberShardNames(currentMember));
        for (String shard : currentSnapshot.getShardList()) {
            if (!configuredShardList.contains(shard)) {
                // add the current member as a replica for the shard
                LOG.debug ("{}: adding shard {}", persistenceId(), shard);
                configuration.addMemberReplicaForShard(shard, currentMember);
            } else {
                configuredShardList.remove(shard);
            }
        }
        for (String shard : configuredShardList) {
            // remove the member as a replica for the shard
            LOG.debug ("{}: removing shard {}", persistenceId(), shard);
            configuration.removeMemberReplicaForShard(shard, currentMember);
        }
    }

    private void onSaveSnapshotSuccess (SaveSnapshotSuccess successMessage) {
        LOG.debug ("{} saved ShardManager snapshot successfully. Deleting the prev snapshot if available",
            persistenceId());
        deleteSnapshots(new SnapshotSelectionCriteria(scala.Long.MaxValue(), successMessage.metadata().timestamp() - 1,
            0, 0));
    }

    private void onChangeShardServersVotingStatus(final ChangeShardMembersVotingStatus changeMembersVotingStatus) {
        LOG.debug("{}: onChangeShardServersVotingStatus: {}", persistenceId(), changeMembersVotingStatus);

        String shardName = changeMembersVotingStatus.getShardName();
        Map<String, Boolean> serverVotingStatusMap = new HashMap<>();
        for(Entry<String, Boolean> e: changeMembersVotingStatus.getMeberVotingStatusMap().entrySet()) {
            serverVotingStatusMap.put(getShardIdentifier(MemberName.forName(e.getKey()), shardName).toString(),
                    e.getValue());
        }

        ChangeServersVotingStatus changeServersVotingStatus = new ChangeServersVotingStatus(serverVotingStatusMap);

        findLocalShard(shardName, getSender(),
                localShardFound -> changeShardMembersVotingStatus(changeServersVotingStatus, shardName,
                        localShardFound.getPath(), getSender()));
    }

    private void onFlipShardMembersVotingStatus(FlipShardMembersVotingStatus flipMembersVotingStatus) {
        LOG.debug("{}: onFlipShardMembersVotingStatus: {}", persistenceId(), flipMembersVotingStatus);

        ActorRef sender = getSender();
        final String shardName = flipMembersVotingStatus.getShardName();
        findLocalShard(shardName, sender, localShardFound -> {
            Future<Object> future = ask(localShardFound.getPath(), GetOnDemandRaftState.INSTANCE,
                    Timeout.apply(30, TimeUnit.SECONDS));

            future.onComplete(new OnComplete<Object>() {
                @Override
                public void onComplete(Throwable failure, Object response) {
                    if (failure != null) {
                        sender.tell(new Status.Failure(new RuntimeException(
                                String.format("Failed to access local shard %s", shardName), failure)), self());
                        return;
                    }

                    OnDemandRaftState raftState = (OnDemandRaftState) response;
                    Map<String, Boolean> serverVotingStatusMap = new HashMap<>();
                    for(Entry<String, Boolean> e: raftState.getPeerVotingStates().entrySet()) {
                        serverVotingStatusMap.put(e.getKey(), !e.getValue());
                    }

                    serverVotingStatusMap.put(getShardIdentifier(cluster.getCurrentMemberName(), shardName).
                            toString(), !raftState.isVoting());

                    changeShardMembersVotingStatus(new ChangeServersVotingStatus(serverVotingStatusMap),
                            shardName, localShardFound.getPath(), sender);
                }
            }, new Dispatchers(context().system().dispatchers()).getDispatcher(Dispatchers.DispatcherType.Client));
        });

    }

    private void findLocalShard(final String shardName, final ActorRef sender,
            final Consumer<LocalShardFound> onLocalShardFound) {
        Timeout findLocalTimeout = new Timeout(datastoreContextFactory.getBaseDatastoreContext().
                getShardInitializationTimeout().duration().$times(2));

        Future<Object> futureObj = ask(getSelf(), new FindLocalShard(shardName, true), findLocalTimeout);
        futureObj.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) {
                if (failure != null) {
                    LOG.debug ("{}: Received failure from FindLocalShard for shard {}", persistenceId, shardName, failure);
                    sender.tell(new Status.Failure(new RuntimeException(
                            String.format("Failed to find local shard %s", shardName), failure)), self());
                } else {
                    if(response instanceof LocalShardFound) {
                        getSelf().tell((RunnableMessage) () -> onLocalShardFound.accept((LocalShardFound) response), sender);
                    } else if(response instanceof LocalShardNotFound) {
                        String msg = String.format("Local shard %s does not exist", shardName);
                        LOG.debug ("{}: {}", persistenceId, msg);
                        sender.tell(new Status.Failure(new IllegalArgumentException(msg)), self());
                    } else {
                        String msg = String.format("Failed to find local shard %s: received response: %s",
                                shardName, response);
                        LOG.debug ("{}: {}", persistenceId, msg);
                        sender.tell(new Status.Failure(response instanceof Throwable ? (Throwable) response :
                                new RuntimeException(msg)), self());
                    }
                }
            }
        }, new Dispatchers(context().system().dispatchers()).getDispatcher(Dispatchers.DispatcherType.Client));
    }

    private void changeShardMembersVotingStatus(ChangeServersVotingStatus changeServersVotingStatus,
            final String shardName, final ActorRef shardActorRef, final ActorRef sender) {
        if(isShardReplicaOperationInProgress(shardName, sender)) {
            return;
        }

        shardReplicaOperationsInProgress.add(shardName);

        DatastoreContext datastoreContext = newShardDatastoreContextBuilder(shardName).build();
        final ShardIdentifier shardId = getShardIdentifier(cluster.getCurrentMemberName(), shardName);

        LOG.debug("{}: Sending ChangeServersVotingStatus message {} to local shard {}", persistenceId(),
                changeServersVotingStatus, shardActorRef.path());

        Timeout timeout = new Timeout(datastoreContext.getShardLeaderElectionTimeout().duration().$times(2));
        Future<Object> futureObj = ask(shardActorRef, changeServersVotingStatus, timeout);

        futureObj.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) {
                shardReplicaOperationsInProgress.remove(shardName);
                if (failure != null) {
                    String msg = String.format("ChangeServersVotingStatus request to local shard %s failed",
                            shardActorRef.path());
                    LOG.debug ("{}: {}", persistenceId(), msg, failure);
                    sender.tell(new Status.Failure(new RuntimeException(msg, failure)), self());
                } else {
                    LOG.debug ("{}: Received {} from local shard {}", persistenceId(), response, shardActorRef.path());

                    ServerChangeReply replyMsg = (ServerChangeReply) response;
                    if(replyMsg.getStatus() == ServerChangeStatus.OK) {
                        LOG.debug ("{}: ChangeServersVotingStatus succeeded for shard {}", persistenceId(), shardName);
                        sender.tell(new Status.Success(null), getSelf());
                    } else if(replyMsg.getStatus() == ServerChangeStatus.INVALID_REQUEST) {
                        sender.tell(new Status.Failure(new IllegalArgumentException(String.format(
                                "The requested voting state change for shard %s is invalid. At least one member must be voting",
                                shardId.getShardName()))), getSelf());
                    } else {
                        LOG.warn ("{}: ChangeServersVotingStatus failed for shard {} with status {}",
                                persistenceId(), shardName, replyMsg.getStatus());

                        Exception error = getServerChangeException(ChangeServersVotingStatus.class,
                                replyMsg.getStatus(), shardActorRef.path().toString(), shardId);
                        sender.tell(new Status.Failure(error), getSelf());
                    }
                }
            }
        }, new Dispatchers(context().system().dispatchers()).getDispatcher(Dispatchers.DispatcherType.Client));
    }

    private static final class ForwardedAddServerReply {
        ShardInformation shardInfo;
        AddServerReply addServerReply;
        String leaderPath;
        boolean removeShardOnFailure;

        ForwardedAddServerReply(ShardInformation shardInfo, AddServerReply addServerReply, String leaderPath,
                boolean removeShardOnFailure) {
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

        ForwardedAddServerFailure(String shardName, String failureMessage, Throwable failure,
                boolean removeShardOnFailure) {
            this.shardName = shardName;
            this.failureMessage = failureMessage;
            this.failure = failure;
            this.removeShardOnFailure = removeShardOnFailure;
        }
    }

    static class OnShardInitialized {
        private final Runnable replyRunnable;
        private Cancellable timeoutSchedule;

        OnShardInitialized(Runnable replyRunnable) {
            this.replyRunnable = replyRunnable;
        }

        Runnable getReplyRunnable() {
            return replyRunnable;
        }

        Cancellable getTimeoutSchedule() {
            return timeoutSchedule;
        }

        void setTimeoutSchedule(Cancellable timeoutSchedule) {
            this.timeoutSchedule = timeoutSchedule;
        }
    }

    static class OnShardReady extends OnShardInitialized {
        OnShardReady(Runnable replyRunnable) {
            super(replyRunnable);
        }
    }

    private void findPrimary(final String shardName, final FindPrimaryResponseHandler handler) {
        Timeout findPrimaryTimeout = new Timeout(datastoreContextFactory.getBaseDatastoreContext().
                getShardInitializationTimeout().duration().$times(2));


        Future<Object> futureObj = ask(getSelf(), new FindPrimary(shardName, true), findPrimaryTimeout);
        futureObj.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) {
                if (failure != null) {
                    handler.onFailure(failure);
                } else {
                    if(response instanceof RemotePrimaryShardFound) {
                        handler.onRemotePrimaryShardFound((RemotePrimaryShardFound) response);
                    } else if(response instanceof LocalPrimaryShardFound) {
                        handler.onLocalPrimaryFound((LocalPrimaryShardFound) response);
                    } else {
                        handler.onUnknownResponse(response);
                    }
                }
            }
        }, new Dispatchers(context().system().dispatchers()).getDispatcher(Dispatchers.DispatcherType.Client));
    }

    private static interface RunnableMessage extends Runnable {
    }

    /**
     * The FindPrimaryResponseHandler provides specific callback methods which are invoked when a response to the
     * a remote or local find primary message is processed
     */
    private static interface FindPrimaryResponseHandler {
        /**
         * Invoked when a Failure message is received as a response
         *
         * @param failure
         */
        void onFailure(Throwable failure);

        /**
         * Invoked when a RemotePrimaryShardFound response is received
         *
         * @param response
         */
        void onRemotePrimaryShardFound(RemotePrimaryShardFound response);

        /**
         * Invoked when a LocalPrimaryShardFound response is received
         * @param response
         */
        void onLocalPrimaryFound(LocalPrimaryShardFound response);

        /**
         * Invoked when an unknown response is received. This is another type of failure.
         *
         * @param response
         */
        void onUnknownResponse(Object response);
    }

    /**
     * The AutoFindPrimaryFailureResponseHandler automatically processes Failure responses when finding a primary
     * replica and sends a wrapped Failure response to some targetActor
     */
    private static abstract class AutoFindPrimaryFailureResponseHandler implements FindPrimaryResponseHandler {
        private final ActorRef targetActor;
        private final String shardName;
        private final String persistenceId;
        private final ActorRef shardManagerActor;

        /**
         * @param targetActor The actor to whom the Failure response should be sent when a FindPrimary failure occurs
         * @param shardName The name of the shard for which the primary replica had to be found
         * @param persistenceId The persistenceId for the ShardManager
         * @param shardManagerActor The ShardManager actor which triggered the call to FindPrimary
         */
        protected AutoFindPrimaryFailureResponseHandler(ActorRef targetActor, String shardName, String persistenceId, ActorRef shardManagerActor){
            this.targetActor = Preconditions.checkNotNull(targetActor);
            this.shardName = Preconditions.checkNotNull(shardName);
            this.persistenceId = Preconditions.checkNotNull(persistenceId);
            this.shardManagerActor = Preconditions.checkNotNull(shardManagerActor);
        }

        public ActorRef getTargetActor() {
            return targetActor;
        }

        public String getShardName() {
            return shardName;
        }

        @Override
        public void onFailure(Throwable failure) {
            LOG.debug ("{}: Received failure from FindPrimary for shard {}", persistenceId, shardName, failure);
            targetActor.tell(new Status.Failure(new RuntimeException(
                    String.format("Failed to find leader for shard %s", shardName), failure)), shardManagerActor);
        }

        @Override
        public void onUnknownResponse(Object response) {
            String msg = String.format("Failed to find leader for shard %s: received response: %s",
                    shardName, response);
            LOG.debug ("{}: {}", persistenceId, msg);
            targetActor.tell(new Status.Failure(response instanceof Throwable ? (Throwable) response :
                    new RuntimeException(msg)), shardManagerActor);
        }
    }

    /**
     * The WrappedShardResponse class wraps a response from a Shard.
     */
    private static final class WrappedShardResponse {
        private final ShardIdentifier shardId;
        private final Object response;
        private final String leaderPath;

        WrappedShardResponse(ShardIdentifier shardId, Object response, String leaderPath) {
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

        ShardNotInitializedTimeout(ShardInformation shardInfo, OnShardInitialized onShardInitialized, ActorRef sender) {
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



