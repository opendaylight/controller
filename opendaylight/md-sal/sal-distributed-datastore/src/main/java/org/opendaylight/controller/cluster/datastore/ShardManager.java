/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static akka.pattern.Patterns.ask;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.Address;
import akka.actor.Cancellable;
import akka.actor.OneForOneStrategy;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.cluster.ClusterEvent;
import akka.dispatch.OnComplete;
import akka.japi.Creator;
import akka.japi.Function;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.persistence.SnapshotOffer;
import akka.serialization.Serialization;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedPersistentActorWithMetering;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ModuleShardConfiguration;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.NotInitializedException;
import org.opendaylight.controller.cluster.datastore.exceptions.PrimaryNotFoundException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shardmanager.ShardManagerInfo;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shardmanager.ShardManagerInfoMBean;
import org.opendaylight.controller.cluster.datastore.messages.ActorInitialized;
import org.opendaylight.controller.cluster.datastore.messages.AddShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.CreateShard;
import org.opendaylight.controller.cluster.datastore.messages.CreateShardReply;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.LocalPrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.PeerDown;
import org.opendaylight.controller.cluster.datastore.messages.PeerUp;
import org.opendaylight.controller.cluster.datastore.messages.RemoteFindPrimary;
import org.opendaylight.controller.cluster.datastore.messages.RemotePrimaryShardFound;
import org.opendaylight.controller.cluster.datastore.messages.RemoveShardReplica;
import org.opendaylight.controller.cluster.datastore.messages.ShardLeaderStateChanged;
import org.opendaylight.controller.cluster.datastore.messages.SwitchShardBehavior;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.utils.Dispatchers;
import org.opendaylight.controller.cluster.datastore.utils.PrimaryShardInfoFutureCache;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.base.messages.SwitchBehavior;
import org.opendaylight.controller.cluster.raft.messages.AddServer;
import org.opendaylight.controller.cluster.raft.messages.AddServerReply;
import org.opendaylight.controller.cluster.raft.messages.ServerChangeStatus;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ShardManager extends AbstractUntypedPersistentActorWithMetering {

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

    private ShardManagerInfo mBean;

    private DatastoreContext datastoreContext;

    private final CountDownLatch waitTillReadyCountdownLatch;

    private final PrimaryShardInfoFutureCache primaryShardInfoCache;

    private final ShardPeerAddressResolver peerAddressResolver;

    private SchemaContext schemaContext;

    private boolean recoveryCompleteStatus = false;

    /**
     */
    protected ShardManager(ClusterWrapper cluster, Configuration configuration,
            DatastoreContext datastoreContext, CountDownLatch waitTillReadyCountdownLatch,
            PrimaryShardInfoFutureCache primaryShardInfoCache) {

        this.cluster = Preconditions.checkNotNull(cluster, "cluster should not be null");
        this.configuration = Preconditions.checkNotNull(configuration, "configuration should not be null");
        this.datastoreContext = datastoreContext;
        this.type = datastoreContext.getDataStoreType();
        this.shardDispatcherPath =
                new Dispatchers(context().system().dispatchers()).getDispatcherPath(Dispatchers.DispatcherType.Shard);
        this.waitTillReadyCountdownLatch = waitTillReadyCountdownLatch;
        this.primaryShardInfoCache = primaryShardInfoCache;

        peerAddressResolver = new ShardPeerAddressResolver(type, cluster.getCurrentMemberName());
        this.datastoreContext = DatastoreContext.newBuilderFrom(datastoreContext).shardPeerAddressResolver(
                peerAddressResolver).build();

        // Subscribe this actor to cluster member events
        cluster.subscribeToMemberEvents(getSelf());

    }

    public static Props props(
            final ClusterWrapper cluster,
            final Configuration configuration,
            final DatastoreContext datastoreContext,
            final CountDownLatch waitTillReadyCountdownLatch,
            final PrimaryShardInfoFutureCache primaryShardInfoCache) {

        Preconditions.checkNotNull(cluster, "cluster should not be null");
        Preconditions.checkNotNull(configuration, "configuration should not be null");
        Preconditions.checkNotNull(waitTillReadyCountdownLatch, "waitTillReadyCountdownLatch should not be null");
        Preconditions.checkNotNull(primaryShardInfoCache, "primaryShardInfoCache should not be null");

        return Props.create(new ShardManagerCreator(cluster, configuration, datastoreContext,
                waitTillReadyCountdownLatch, primaryShardInfoCache));
    }

    @Override
    public void postStop() {
        LOG.info("Stopping ShardManager");

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
        } else if (message instanceof ClusterEvent.MemberExited){
            memberExited((ClusterEvent.MemberExited) message);
        } else if(message instanceof ClusterEvent.MemberRemoved) {
            memberRemoved((ClusterEvent.MemberRemoved) message);
        } else if(message instanceof ClusterEvent.UnreachableMember) {
            memberUnreachable((ClusterEvent.UnreachableMember)message);
        } else if(message instanceof ClusterEvent.ReachableMember) {
            memberReachable((ClusterEvent.ReachableMember) message);
        } else if(message instanceof DatastoreContext) {
            onDatastoreContext((DatastoreContext)message);
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
        } else if(message instanceof RemoveShardReplica){
            onRemoveShardReplica((RemoveShardReplica)message);
        } else if (message instanceof SaveSnapshotSuccess) {
            LOG.debug ("saved ShardManager snapshot successfully");
        } else if (message instanceof SaveSnapshotFailure) {
            LOG.error ("SaveSnapshotFailure received for saving snapshot of shards");
        } else {
            unknownMessage(message);
        }

    }

    private void onCreateShard(CreateShard createShard) {
        Object reply;
        try {
            ModuleShardConfiguration moduleShardConfig = createShard.getModuleShardConfig();
            if(localShards.containsKey(moduleShardConfig.getShardName())) {
                throw new IllegalStateException(String.format("Shard with name %s already exists",
                        moduleShardConfig.getShardName()));
            }

            configuration.addModuleShardConfiguration(moduleShardConfig);

            ShardIdentifier shardId = getShardIdentifier(cluster.getCurrentMemberName(), moduleShardConfig.getShardName());
            Map<String, String> peerAddresses = getPeerAddresses(moduleShardConfig.getShardName()/*,
                    moduleShardConfig.getShardMemberNames()*/);

            LOG.debug("onCreateShard: shardId: {}, memberNames: {}. peerAddresses: {}", shardId,
                    moduleShardConfig.getShardMemberNames(), peerAddresses);

            DatastoreContext shardDatastoreContext = createShard.getDatastoreContext();
            if(shardDatastoreContext == null) {
                shardDatastoreContext = datastoreContext;
            } else {
                shardDatastoreContext = DatastoreContext.newBuilderFrom(shardDatastoreContext).shardPeerAddressResolver(
                        peerAddressResolver).build();
            }

            ShardInformation info = new ShardInformation(moduleShardConfig.getShardName(), shardId, peerAddresses,
                    shardDatastoreContext, createShard.getShardPropsCreator(), peerAddressResolver);
            localShards.put(info.getShardName(), info);

            mBean.addLocalShard(shardId.toString());

            if(schemaContext != null) {
                info.setActor(newShardActor(schemaContext, info));
            }

            reply = new CreateShardReply();
        } catch (Exception e) {
            LOG.error("onCreateShard failed", e);
            reply = new akka.actor.Status.Failure(e);
        }

        if(getSender() != null && !getContext().system().deadLetters().equals(getSender())) {
            getSender().tell(reply, getSelf());
        }
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
            message.getSender().tell(createNotInitializedException(shardInfo.shardId), getSelf());
        } else {
            LOG.debug("{}: Returning NoShardLeaderException for shard {}", persistenceId(), shardInfo.getShardName());
            message.getSender().tell(createNoShardLeaderException(shardInfo.shardId), getSelf());
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
        ShardIdentifier shardId = ShardIdentifier.builder().fromShardIdString(actorName).build();

        if (shardId.getShardName() == null) {
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
            LOG.info("Recovery complete : {}", persistenceId());

            // We no longer persist SchemaContext modules so delete all the prior messages from the akka
            // journal on upgrade from Helium.
            deleteMessages(lastSequenceNr());
            recoveryCompleteStatus = true;
            createLocalShards();
        } else if (message instanceof SnapshotOffer) {
            handleShardRecovery((SnapshotOffer) message);
        }
    }

    private void findLocalShard(FindLocalShard message) {
        final ShardInformation shardInformation = localShards.get(message.getShardName());

        if(shardInformation == null){
            getSender().tell(new LocalShardNotFound(message.getShardName()), getSelf());
            return;
        }

        sendResponse(shardInformation, message.isWaitUntilInitialized(), false, new Supplier<Object>() {
            @Override
            public Object get() {
                return new LocalShardFound(shardInformation.getActor());
            }
        });
    }

    private void sendResponse(ShardInformation shardInformation, boolean doWait,
            boolean wantShardReady, final Supplier<Object> messageSupplier) {
        if (!shardInformation.isShardInitialized() || (wantShardReady && !shardInformation.isShardReadyWithLeaderId())) {
            if(doWait) {
                final ActorRef sender = getSender();
                final ActorRef self = self();

                Runnable replyRunnable = new Runnable() {
                    @Override
                    public void run() {
                        sender.tell(messageSupplier.get(), self);
                    }
                };

                OnShardInitialized onShardInitialized = wantShardReady ? new OnShardReady(replyRunnable) :
                    new OnShardInitialized(replyRunnable);

                shardInformation.addOnShardInitialized(onShardInitialized);

                LOG.debug("{}: Scheduling timer to wait for shard {}", persistenceId(), shardInformation.getShardName());

                FiniteDuration timeout = datastoreContext.getShardInitializationTimeout().duration();
                if(shardInformation.isShardInitialized()) {
                    // If the shard is already initialized then we'll wait enough time for the shard to
                    // elect a leader, ie 2 times the election timeout.
                    timeout = FiniteDuration.create(datastoreContext.getShardRaftConfig()
                            .getElectionTimeOutInterval().toMillis() * 2, TimeUnit.MILLISECONDS);
                }

                Cancellable timeoutSchedule = getContext().system().scheduler().scheduleOnce(
                        timeout, getSelf(),
                        new ShardNotInitializedTimeout(shardInformation, onShardInitialized, sender),
                        getContext().dispatcher(), getSelf());

                onShardInitialized.setTimeoutSchedule(timeoutSchedule);

            } else if (!shardInformation.isShardInitialized()) {
                LOG.debug("{}: Returning NotInitializedException for shard {}", persistenceId(),
                        shardInformation.getShardName());
                getSender().tell(createNotInitializedException(shardInformation.shardId), getSelf());
            } else {
                LOG.debug("{}: Returning NoShardLeaderException for shard {}", persistenceId(),
                        shardInformation.getShardName());
                getSender().tell(createNoShardLeaderException(shardInformation.shardId), getSelf());
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

    private void memberRemoved(ClusterEvent.MemberRemoved message) {
        String memberName = message.member().roles().head();

        LOG.debug("{}: Received MemberRemoved: memberName: {}, address: {}", persistenceId(), memberName,
                message.member().address());

        peerAddressResolver.removePeerAddress(memberName);

        for(ShardInformation info : localShards.values()){
            info.peerDown(memberName, getShardIdentifier(memberName, info.getShardName()).toString(), getSelf());
        }
    }

    private void memberExited(ClusterEvent.MemberExited message) {
        String memberName = message.member().roles().head();

        LOG.debug("{}: Received MemberExited: memberName: {}, address: {}", persistenceId(), memberName,
                message.member().address());

        peerAddressResolver.removePeerAddress(memberName);

        for(ShardInformation info : localShards.values()){
            info.peerDown(memberName, getShardIdentifier(memberName, info.getShardName()).toString(), getSelf());
        }
    }

    private void memberUp(ClusterEvent.MemberUp message) {
        String memberName = message.member().roles().head();

        LOG.debug("{}: Received MemberUp: memberName: {}, address: {}", persistenceId(), memberName,
                message.member().address());

        addPeerAddress(memberName, message.member().address());

        checkReady();
    }

    private void addPeerAddress(String memberName, Address address) {
        peerAddressResolver.addPeerAddress(memberName, address);

        for(ShardInformation info : localShards.values()){
            String shardName = info.getShardName();
            String peerId = getShardIdentifier(memberName, shardName).toString();
            info.updatePeerAddress(peerId, peerAddressResolver.getShardActorAddress(shardName, memberName), getSelf());

            info.peerUp(memberName, peerId, getSelf());
        }
    }

    private void memberReachable(ClusterEvent.ReachableMember message) {
        String memberName = message.member().roles().head();
        LOG.debug("Received ReachableMember: memberName {}, address: {}", memberName, message.member().address());

        addPeerAddress(memberName, message.member().address());

        markMemberAvailable(memberName);
    }

    private void memberUnreachable(ClusterEvent.UnreachableMember message) {
        String memberName = message.member().roles().head();
        LOG.debug("Received UnreachableMember: memberName {}, address: {}", memberName, message.member().address());

        markMemberUnavailable(memberName);
    }

    private void markMemberUnavailable(final String memberName) {
        for(ShardInformation info : localShards.values()){
            String leaderId = info.getLeaderId();
            if(leaderId != null && leaderId.contains(memberName)) {
                LOG.debug("Marking Leader {} as unavailable.", leaderId);
                info.setLeaderAvailable(false);

                primaryShardInfoCache.remove(info.getShardName());
            }

            info.peerDown(memberName, getShardIdentifier(memberName, info.getShardName()).toString(), getSelf());
        }
    }

    private void markMemberAvailable(final String memberName) {
        for(ShardInformation info : localShards.values()){
            String leaderId = info.getLeaderId();
            if(leaderId != null && leaderId.contains(memberName)) {
                LOG.debug("Marking Leader {} as available.", leaderId);
                info.setLeaderAvailable(true);
            }

            info.peerUp(memberName, getShardIdentifier(memberName, info.getShardName()).toString(), getSelf());
        }
    }

    private void onDatastoreContext(DatastoreContext context) {
        datastoreContext = DatastoreContext.newBuilderFrom(context).shardPeerAddressResolver(
                peerAddressResolver).build();
        for (ShardInformation info : localShards.values()) {
            if (info.getActor() != null) {
                info.getActor().tell(datastoreContext, getSelf());
            }
        }
    }

    private void onSwitchShardBehavior(SwitchShardBehavior message) {
        ShardIdentifier identifier = ShardIdentifier.builder().fromShardIdString(message.getShardName()).build();

        ShardInformation shardInformation = localShards.get(identifier.getShardName());

        if(shardInformation != null && shardInformation.getActor() != null) {
            shardInformation.getActor().tell(
                    new SwitchBehavior(RaftState.valueOf(message.getNewState()), message.getTerm()), getSelf());
        } else {
            LOG.warn("Could not switch the behavior of shard {} to {} - shard is not yet available",
                    message.getShardName(), message.getNewState());
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
        if (info != null) {
            sendResponse(info, message.isWaitUntilReady(), true, new Supplier<Object>() {
                @Override
                public Object get() {
                    String primaryPath = info.getSerializedLeaderActor();
                    Object found = canReturnLocalShardState && info.isLeader() ?
                            new LocalPrimaryShardFound(primaryPath, info.getLocalShardDataTree().get()) :
                                new RemotePrimaryShardFound(primaryPath, info.getLeaderVersion());

                            if(LOG.isDebugEnabled()) {
                                LOG.debug("{}: Found primary for {}: {}", persistenceId(), shardName, found);
                            }

                            return found;
                }
            });

            return;
        }

        for(String address: peerAddressResolver.getShardManagerPeerActorAddresses()) {
            LOG.debug("{}: findPrimary for {} forwarding to remote ShardManager {}", persistenceId(),
                    shardName, address);

            getContext().actorSelection(address).forward(new RemoteFindPrimary(shardName,
                    message.isWaitUntilReady()), getContext());
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
    private ShardIdentifier getShardIdentifier(String memberName, String shardName){
        return peerAddressResolver.getShardIdentifier(memberName, shardName);
    }

    /**
     * Create shards that are local to the member on which the ShardManager
     * runs
     *
     */
    private void createLocalShards() {
        String memberName = this.cluster.getCurrentMemberName();
        Collection<String> memberShardNames = this.configuration.getMemberShardNames(memberName);

        ShardPropsCreator shardPropsCreator = new DefaultShardPropsCreator();
        List<String> localShardActorNames = new ArrayList<>();
        for(String shardName : memberShardNames){
            ShardIdentifier shardId = getShardIdentifier(memberName, shardName);
            Map<String, String> peerAddresses = getPeerAddresses(shardName);
            localShardActorNames.add(shardId.toString());
            localShards.put(shardName, new ShardInformation(shardName, shardId, peerAddresses, datastoreContext,
                    shardPropsCreator, peerAddressResolver));
        }

        mBean = ShardManagerInfo.createShardManagerMBean(memberName, "shard-manager-" + this.type,
                datastoreContext.getDataStoreMXBeanType(), localShardActorNames);

        mBean.setShardManager(this);
    }

    /**
     * Given the name of the shard find the addresses of all it's peers
     *
     * @param shardName
     */
    private Map<String, String> getPeerAddresses(String shardName) {
        Collection<String> members = configuration.getMembersFromShardName(shardName);
        Map<String, String> peerAddresses = new HashMap<>();

        String currentMemberName = this.cluster.getCurrentMemberName();

        for(String memberName : members) {
            if(!currentMemberName.equals(memberName)) {
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
                new Function<Throwable, SupervisorStrategy.Directive>() {
            @Override
            public SupervisorStrategy.Directive apply(Throwable t) {
                LOG.warn("Supervisor Strategy caught unexpected exception - resuming", t);
                return SupervisorStrategy.resume();
            }
        }
                );

    }

    @Override
    public String persistenceId() {
        return "shard-manager-" + type;
    }

    @VisibleForTesting
    ShardManagerInfoMBean getMBean(){
        return mBean;
    }

    private DatastoreContext getInitShardDataStoreContext() {
        return (DatastoreContext.newBuilderFrom(datastoreContext)
                .customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName())
                .build());
    }

    private void checkLocalShardExists(final String shardName, final ActorRef sender) {
        if (localShards.containsKey(shardName)) {
            String msg = String.format("Local shard %s already exists", shardName);
            LOG.debug ("{}: {}", persistenceId(), msg);
            sender.tell(new akka.actor.Status.Failure(new IllegalArgumentException(msg)), getSelf());
        }
    }

    private void onAddShardReplica (AddShardReplica shardReplicaMsg) {
        final String shardName = shardReplicaMsg.getShardName();

        // verify the local shard replica is already available in the controller node
        LOG.debug ("onAddShardReplica: {}", shardReplicaMsg);

        checkLocalShardExists(shardName, getSender());

        // verify the shard with the specified name is present in the cluster configuration
        if (!(this.configuration.isShardConfigured(shardName))) {
            String msg = String.format("No module configuration exists for shard %s", shardName);
            LOG.debug ("{}: {}", persistenceId(), msg);
            getSender().tell(new akka.actor.Status.Failure(new IllegalArgumentException(msg)), getSelf());
            return;
        }

        // Create the localShard
        if (schemaContext == null) {
            String msg = String.format(
                  "No SchemaContext is available in order to create a local shard instance for %s", shardName);
            LOG.debug ("{}: {}", persistenceId(), msg);
            getSender().tell(new akka.actor.Status.Failure(new IllegalStateException(msg)), getSelf());
            return;
        }

        Map<String, String> peerAddresses = getPeerAddresses(shardName);
        if (peerAddresses.isEmpty()) {
            String msg = String.format("Cannot add replica for shard %s because no peer is available", shardName);
            LOG.debug ("{}: {}", persistenceId(), msg);
            getSender().tell(new akka.actor.Status.Failure(new IllegalStateException(msg)), getSelf());
            return;
        }

        Timeout findPrimaryTimeout = new Timeout(datastoreContext.getShardInitializationTimeout().duration().$times(2));

        final ActorRef sender = getSender();
        Future<Object> futureObj = ask(getSelf(), new RemoteFindPrimary(shardName, true), findPrimaryTimeout);
        futureObj.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) {
                if (failure != null) {
                    LOG.debug ("{}: Received failure from FindPrimary for shard {}", persistenceId(), shardName, failure);
                    sender.tell(new akka.actor.Status.Failure(new RuntimeException(
                        String.format("Failed to find leader for shard %s", shardName), failure)),
                        getSelf());
                } else {
                    if (!(response instanceof RemotePrimaryShardFound)) {
                        String msg = String.format("Failed to find leader for shard %s: received response: %s",
                                shardName, response);
                        LOG.debug ("{}: {}", persistenceId(), msg);
                        sender.tell(new akka.actor.Status.Failure(new RuntimeException(msg)), getSelf());
                        return;
                    }

                    RemotePrimaryShardFound message = (RemotePrimaryShardFound)response;
                    addShard (shardName, message, sender);
                }
            }
        }, new Dispatchers(context().system().dispatchers()).getDispatcher(Dispatchers.DispatcherType.Client));
    }

    private void addShard(final String shardName, final RemotePrimaryShardFound response, final ActorRef sender) {
        checkLocalShardExists(shardName, sender);

        ShardIdentifier shardId = getShardIdentifier(cluster.getCurrentMemberName(), shardName);
        String localShardAddress = peerAddressResolver.getShardActorAddress(shardName, cluster.getCurrentMemberName());
        final ShardInformation shardInfo = new ShardInformation(shardName, shardId,
                          getPeerAddresses(shardName), getInitShardDataStoreContext(),
                          new DefaultShardPropsCreator(), peerAddressResolver);
        shardInfo.setShardInitializedState(false);
        localShards.put(shardName, shardInfo);
        shardInfo.setActor(newShardActor(schemaContext, shardInfo));

        //inform ShardLeader to add this shard as a replica by sending an AddServer message
        LOG.debug ("{}: Sending AddServer message to peer {} for shard {}", persistenceId(),
                response.getPrimaryPath(), shardId);

        Timeout addServerTimeout = new Timeout(datastoreContext
                       .getShardLeaderElectionTimeout().duration().$times(4));
        Future<Object> futureObj = ask(getContext().actorSelection(response.getPrimaryPath()),
            new AddServer(shardId.toString(), localShardAddress, true), addServerTimeout);

        futureObj.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object addServerResponse) {
                if (failure != null) {
                    LOG.debug ("{}: AddServer request to {} for {} failed", persistenceId(),
                            response.getPrimaryPath(), shardName, failure);

                    // Remove the shard
                    localShards.remove(shardName);
                    if (shardInfo.getActor() != null) {
                        shardInfo.getActor().tell(PoisonPill.getInstance(), getSelf());
                    }

                    sender.tell(new akka.actor.Status.Failure(new RuntimeException(
                        String.format("AddServer request to leader %s for shard %s failed",
                            response.getPrimaryPath(), shardName), failure)), getSelf());
                } else {
                    AddServerReply reply = (AddServerReply)addServerResponse;
                    onAddServerReply(shardName, shardInfo, reply, sender, response.getPrimaryPath());
                }
            }
        }, new Dispatchers(context().system().dispatchers()).
            getDispatcher(Dispatchers.DispatcherType.Client));
        return;
    }

    private void onAddServerReply (String shardName, ShardInformation shardInfo,
                                   AddServerReply replyMsg, ActorRef sender, String leaderPath) {
        LOG.debug ("{}: Received {} for shard {} from leader {}", persistenceId(), replyMsg, shardName, leaderPath);

        if (replyMsg.getStatus() == ServerChangeStatus.OK) {
            LOG.debug ("{}: Leader shard successfully added the replica shard {}", persistenceId(), shardName);

            // Make the local shard voting capable
            shardInfo.setDatastoreContext(datastoreContext, getSelf());
            shardInfo.setShardInitializedState(true);
            persistShardList();

            mBean.addLocalShard(shardInfo.getShardId().toString());
            sender.tell(new akka.actor.Status.Success(true), getSelf());
        } else {
            LOG.warn ("{}: Leader failed to add shard replica {} with status {} - removing the local shard",
                    persistenceId(), shardName, replyMsg.getStatus());

            //remove the local replica created
            localShards.remove(shardName);
            if (shardInfo.getActor() != null) {
                shardInfo.getActor().tell(PoisonPill.getInstance(), getSelf());
            }
            switch (replyMsg.getStatus()) {
                case TIMEOUT:
                    sender.tell(new akka.actor.Status.Failure(new RuntimeException(
                        String.format("The shard leader %s timed out trying to replicate the initial data to the new shard %s. Possible causes - there was a problem replicating the data or shard leadership changed while replicating the shard data",
                            leaderPath, shardName))), getSelf());
                    break;
                case NO_LEADER:
                    sender.tell(new akka.actor.Status.Failure(new RuntimeException(String.format(
                        "There is no shard leader available for shard %s", shardName))), getSelf());
                    break;
                default :
                    sender.tell(new akka.actor.Status.Failure(new RuntimeException(String.format(
                        "AddServer request to leader %s for shard %s failed with status %s",
                        leaderPath, shardName, replyMsg.getStatus()))), getSelf());
            }
        }
    }

    private void onRemoveShardReplica (RemoveShardReplica shardReplicaMsg) {
        String shardName = shardReplicaMsg.getShardName();

        // verify the local shard replica is available in the controller node
        if (!localShards.containsKey(shardName)) {
            String msg = String.format("Local shard %s does not", shardName);
            LOG.debug ("{}: {}", persistenceId(), msg);
            getSender().tell(new akka.actor.Status.Failure(new IllegalArgumentException(msg)), getSelf());
            return;
        }
        // call RemoveShard for the shardName
        getSender().tell(new akka.actor.Status.Success(true), getSelf());
        return;
    }

    private void persistShardList() {
        if (recoveryCompleteStatus == false) {
            //do nothing
            return;
        }

        List<String> shardList = new ArrayList(localShards.keySet());
        for (ShardInformation shardInfo : localShards.values()) {
            if (shardInfo.getShardInitializedState() == false) {
                shardList.remove(shardInfo.getShardName());
            }
        }
        LOG.debug ("persisting the shard list {}", shardList);
        saveSnapshot(new ShardManagerSnapshot(shardList));
    }

    private void handleShardRecovery(SnapshotOffer offer) {
        LOG.debug ("in handleShardRecovery ");
        ShardManagerSnapshot snapshot = (ShardManagerSnapshot)offer.snapshot();
        String currentMember = cluster.getCurrentMemberName();
        List<String> configuredShardList = new ArrayList<>(configuration.getMemberShardNames(currentMember));
        for (String shard : snapshot.getShardList()) {
            if (!configuredShardList.contains(shard)) {
                // add the current member as a replica for the shard
                LOG.debug ("adding shard {}", shard);
                configuration.updateMemberReplicasForShard(shard, currentMember, true);
            } else {
                configuredShardList.remove(shard);
            }
        }
        for (String shard : configuredShardList) {
            // remove the member as a replica for the shard
            LOG.debug ("removing shard {}", shard);
            configuration.updateMemberReplicasForShard(shard, currentMember, false);
        }
    }

    @VisibleForTesting
    protected static class ShardInformation {
        private final ShardIdentifier shardId;
        private final String shardName;
        private ActorRef actor;
        private ActorPath actorPath;
        private final Map<String, String> initialPeerAddresses;
        private Optional<DataTree> localShardDataTree;
        private boolean leaderAvailable = false;

        // flag that determines if the actor is ready for business
        private boolean actorInitialized = false;

        private boolean followerSyncStatus = false;

        private final Set<OnShardInitialized> onShardInitializedSet = Sets.newHashSet();
        private String role ;
        private String leaderId;
        private short leaderVersion;

        private DatastoreContext datastoreContext;
        private final ShardPropsCreator shardPropsCreator;
        private final ShardPeerAddressResolver addressResolver;
        private boolean shardInitializedState = true;

        private ShardInformation(String shardName, ShardIdentifier shardId,
                Map<String, String> initialPeerAddresses, DatastoreContext datastoreContext,
                ShardPropsCreator shardPropsCreator, ShardPeerAddressResolver addressResolver) {
            this.shardName = shardName;
            this.shardId = shardId;
            this.initialPeerAddresses = initialPeerAddresses;
            this.datastoreContext = datastoreContext;
            this.shardPropsCreator = shardPropsCreator;
            this.addressResolver = addressResolver;
        }

        Props newProps(SchemaContext schemaContext) {
            return shardPropsCreator.newProps(shardId, initialPeerAddresses, datastoreContext, schemaContext);
        }

        String getShardName() {
            return shardName;
        }

        ActorRef getActor(){
            return actor;
        }

        ActorPath getActorPath() {
            return actorPath;
        }

        void setActor(ActorRef actor) {
            this.actor = actor;
            this.actorPath = actor.path();
        }

        ShardIdentifier getShardId() {
            return shardId;
        }

        void setLocalDataTree(Optional<DataTree> localShardDataTree) {
            this.localShardDataTree = localShardDataTree;
        }

        Optional<DataTree> getLocalShardDataTree() {
            return localShardDataTree;
        }

        void updatePeerAddress(String peerId, String peerAddress, ActorRef sender){
            LOG.info("updatePeerAddress for peer {} with address {}", peerId, peerAddress);

            if(actor != null) {
                if(LOG.isDebugEnabled()) {
                    LOG.debug("Sending PeerAddressResolved for peer {} with address {} to {}",
                            peerId, peerAddress, actor.path());
                }

                actor.tell(new PeerAddressResolved(peerId, peerAddress), sender);
            }

            notifyOnShardInitializedCallbacks();
        }

        void peerDown(String memberName, String peerId, ActorRef sender) {
            if(actor != null) {
                actor.tell(new PeerDown(memberName, peerId), sender);
            }
        }

        void peerUp(String memberName, String peerId, ActorRef sender) {
            if(actor != null) {
                actor.tell(new PeerUp(memberName, peerId), sender);
            }
        }

        boolean isShardReady() {
            return !RaftState.Candidate.name().equals(role) && !Strings.isNullOrEmpty(role);
        }

        boolean isShardReadyWithLeaderId() {
            return leaderAvailable && isShardReady() && !RaftState.IsolatedLeader.name().equals(role) &&
                    (isLeader() || addressResolver.resolve(leaderId) != null);
        }

        boolean isShardInitialized() {
            return getActor() != null && actorInitialized;
        }

        boolean isLeader() {
            return Objects.equal(leaderId, shardId.toString());
        }

        String getSerializedLeaderActor() {
            if(isLeader()) {
                return Serialization.serializedActorPath(getActor());
            } else {
                return addressResolver.resolve(leaderId);
            }
        }

        void setActorInitialized() {
            LOG.debug("Shard {} is initialized", shardId);

            this.actorInitialized = true;

            notifyOnShardInitializedCallbacks();
        }

        private void notifyOnShardInitializedCallbacks() {
            if(onShardInitializedSet.isEmpty()) {
                return;
            }

            boolean ready = isShardReadyWithLeaderId();

            if(LOG.isDebugEnabled()) {
                LOG.debug("Shard {} is {} - notifying {} OnShardInitialized callbacks", shardId,
                        ready ? "ready" : "initialized", onShardInitializedSet.size());
            }

            Iterator<OnShardInitialized> iter = onShardInitializedSet.iterator();
            while(iter.hasNext()) {
                OnShardInitialized onShardInitialized = iter.next();
                if(!(onShardInitialized instanceof OnShardReady) || ready) {
                    iter.remove();
                    onShardInitialized.getTimeoutSchedule().cancel();
                    onShardInitialized.getReplyRunnable().run();
                }
            }
        }

        void addOnShardInitialized(OnShardInitialized onShardInitialized) {
            onShardInitializedSet.add(onShardInitialized);
        }

        void removeOnShardInitialized(OnShardInitialized onShardInitialized) {
            onShardInitializedSet.remove(onShardInitialized);
        }

        void setRole(String newRole) {
            this.role = newRole;

            notifyOnShardInitializedCallbacks();
        }

        void setFollowerSyncStatus(boolean syncStatus){
            this.followerSyncStatus = syncStatus;
        }

        boolean isInSync(){
            if(RaftState.Follower.name().equals(this.role)){
                return followerSyncStatus;
            } else if(RaftState.Leader.name().equals(this.role)){
                return true;
            }

            return false;
        }

        boolean setLeaderId(String leaderId) {
            boolean changed = !Objects.equal(this.leaderId, leaderId);
            this.leaderId = leaderId;
            if(leaderId != null) {
                this.leaderAvailable = true;
            }
            notifyOnShardInitializedCallbacks();

            return changed;
        }

        String getLeaderId() {
            return leaderId;
        }

        void setLeaderAvailable(boolean leaderAvailable) {
            this.leaderAvailable = leaderAvailable;
        }

        short getLeaderVersion() {
            return leaderVersion;
        }

        void setLeaderVersion(short leaderVersion) {
            this.leaderVersion = leaderVersion;
        }

        void setDatastoreContext(DatastoreContext datastoreContext, ActorRef sender) {
            this.datastoreContext = datastoreContext;
            //notify the datastoreContextchange
            LOG.debug ("Notifying RaftPolicy change via datastoreContextChange for {}",
                 this.shardName);
            if (actor != null) {
                actor.tell(this.datastoreContext, sender);
            }
        }

        public void setShardInitializedState(boolean flag) {
            shardInitializedState = flag;
        }

        public boolean getShardInitializedState() {
            return shardInitializedState;
        }
    }

    private static class ShardManagerCreator implements Creator<ShardManager> {
        private static final long serialVersionUID = 1L;

        final ClusterWrapper cluster;
        final Configuration configuration;
        final DatastoreContext datastoreContext;
        private final CountDownLatch waitTillReadyCountdownLatch;
        private final PrimaryShardInfoFutureCache primaryShardInfoCache;

        ShardManagerCreator(ClusterWrapper cluster, Configuration configuration, DatastoreContext datastoreContext,
                CountDownLatch waitTillReadyCountdownLatch, PrimaryShardInfoFutureCache primaryShardInfoCache) {
            this.cluster = cluster;
            this.configuration = configuration;
            this.datastoreContext = datastoreContext;
            this.waitTillReadyCountdownLatch = waitTillReadyCountdownLatch;
            this.primaryShardInfoCache = primaryShardInfoCache;
        }

        @Override
        public ShardManager create() throws Exception {
            return new ShardManager(cluster, configuration, datastoreContext, waitTillReadyCountdownLatch,
                    primaryShardInfoCache);
        }
    }

    private static class OnShardInitialized {
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

    private static class OnShardReady extends OnShardInitialized {
        OnShardReady(Runnable replyRunnable) {
            super(replyRunnable);
        }
    }

    private static class ShardNotInitializedTimeout {
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

    /**
     * We no longer persist SchemaContextModules but keep this class around for now for backwards
     * compatibility so we don't get de-serialization failures on upgrade from Helium.
     */
    @Deprecated
    static class SchemaContextModules implements Serializable {
        private static final long serialVersionUID = -8884620101025936590L;

        private final Set<String> modules;

        SchemaContextModules(Set<String> modules){
            this.modules = modules;
        }

        public Set<String> getModules() {
            return modules;
        }
    }
}



