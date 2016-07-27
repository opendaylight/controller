/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.serialization.Serialization;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.PeerDown;
import org.opendaylight.controller.cluster.datastore.messages.PeerUp;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManager.OnShardInitialized;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManager.OnShardReady;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ShardInformation {
    private static final Logger LOG = LoggerFactory.getLogger(ShardInformation.class);

    private final Set<OnShardInitialized> onShardInitializedSet = new HashSet<>();
    private final Map<String, String> initialPeerAddresses;
    private final ShardPeerAddressResolver addressResolver;
    private final ShardIdentifier shardId;
    private final String shardName;
    private ActorRef actor;
    private Optional<DataTree> localShardDataTree;
    private boolean leaderAvailable = false;

    // flag that determines if the actor is ready for business
    private boolean actorInitialized = false;

    private boolean followerSyncStatus = false;

    private String role ;
    private String leaderId;
    private short leaderVersion;

    private DatastoreContext datastoreContext;
    private Shard.AbstractBuilder<?, ?> builder;
    private boolean isActiveMember = true;

    ShardInformation(String shardName, ShardIdentifier shardId,
            Map<String, String> initialPeerAddresses, DatastoreContext datastoreContext,
            Shard.AbstractBuilder<?, ?> builder, ShardPeerAddressResolver addressResolver) {
        this.shardName = shardName;
        this.shardId = shardId;
        this.initialPeerAddresses = initialPeerAddresses;
        this.datastoreContext = datastoreContext;
        this.builder = builder;
        this.addressResolver = addressResolver;
    }

    Props newProps(SchemaContext schemaContext) {
        Preconditions.checkNotNull(builder);
        Props props = builder.id(shardId).peerAddresses(initialPeerAddresses).datastoreContext(datastoreContext).
                schemaContext(schemaContext).props();
        builder = null;
        return props;
    }

    String getShardName() {
        return shardName;
    }

    @Nullable
    ActorRef getActor(){
        return actor;
    }

    void setActor(ActorRef actor) {
        this.actor = actor;
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

    DatastoreContext getDatastoreContext() {
        return datastoreContext;
    }

    void setDatastoreContext(DatastoreContext datastoreContext, ActorRef sender) {
        this.datastoreContext = datastoreContext;
        if (actor != null) {
            LOG.debug("Sending new DatastoreContext to {}", shardId);
            actor.tell(this.datastoreContext, sender);
        }
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

    void peerDown(MemberName memberName, String peerId, ActorRef sender) {
        if(actor != null) {
            actor.tell(new PeerDown(memberName, peerId), sender);
        }
    }

    void peerUp(MemberName memberName, String peerId, ActorRef sender) {
        if(actor != null) {
            actor.tell(new PeerUp(memberName, peerId), sender);
        }
    }

    boolean isShardReady() {
        return !RaftState.Candidate.name().equals(role) && !Strings.isNullOrEmpty(role);
    }

    boolean isShardReadyWithLeaderId() {
        return leaderAvailable && isShardReady() && !RaftState.IsolatedLeader.name().equals(role) &&
                !RaftState.PreLeader.name().equals(role) &&
                    (isLeader() || addressResolver.resolve(leaderId) != null);
    }

    boolean isShardInitialized() {
        return getActor() != null && actorInitialized;
    }

    boolean isLeader() {
        return Objects.equals(leaderId, shardId.toString());
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

        LOG.debug("Shard {} is {} - notifying {} OnShardInitialized callbacks", shardId,
            ready ? "ready" : "initialized", onShardInitializedSet.size());

        Iterator<OnShardInitialized> iter = onShardInitializedSet.iterator();
        while(iter.hasNext()) {
            OnShardInitialized onShardInitialized = iter.next();
            if (!(onShardInitialized instanceof OnShardReady) || ready) {
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
        boolean changed = !Objects.equals(this.leaderId, leaderId);
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

        if(leaderAvailable) {
            notifyOnShardInitializedCallbacks();
        }
    }

    short getLeaderVersion() {
        return leaderVersion;
    }

    void setLeaderVersion(short leaderVersion) {
        this.leaderVersion = leaderVersion;
    }

    boolean isActiveMember() {
        return isActiveMember;
    }

    void setActiveMember(boolean isActiveMember) {
        this.isActiveMember = isActiveMember;
    }
}