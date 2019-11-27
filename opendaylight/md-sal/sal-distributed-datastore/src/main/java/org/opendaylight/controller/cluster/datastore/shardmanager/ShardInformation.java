/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.serialization.Serialization;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jdt.annotation.Nullable;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.shard.persistence.Persistence;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ReadOnlyDataTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class ShardInformation {
    private static final Logger LOG = LoggerFactory.getLogger(ShardInformation.class);

    private final Set<OnShardInitialized> onShardInitializedSet = new HashSet<>();
    private final Persistence persistence;
    private final Map<String, String> initialPeerAddresses;
    private final ShardPeerAddressResolver addressResolver;
    private final ShardIdentifier shardId;
    private final String shardName;

    // This reference indirection is required to have the ability to update the SchemaContext
    // inside actor props. Otherwise we would be keeping an old SchemaContext there, preventing
    // it from becoming garbage.
    private final AtomicShardContextProvider schemaContextProvider = new AtomicShardContextProvider();
    private ActorRef actor;

    private Optional<ReadOnlyDataTree> localShardDataTree;
    private boolean leaderAvailable = false;

    // flag that determines if the actor is ready for business
    private boolean actorInitialized = false;

    private boolean followerSyncStatus = false;

    private String role ;
    private String leaderId;
    private short leaderVersion;

    private DatastoreContext datastoreContext;
    private Shard.AbstractBuilder<?, ?> builder;
    private boolean activeMember = true;

    ShardInformation(final String shardName, final ShardIdentifier shardId,
                     final Map<String, String> initialPeerAddresses, final DatastoreContext datastoreContext,
                     final Shard.AbstractBuilder<?, ?> builder, final ShardPeerAddressResolver addressResolver) {
        this(shardName, shardId, null, initialPeerAddresses, datastoreContext, builder, addressResolver);
    }

    ShardInformation(final String shardName, final ShardIdentifier shardId, final Persistence persistence,
            final Map<String, String> initialPeerAddresses, final DatastoreContext datastoreContext,
            final Shard.AbstractBuilder<?, ?> builder, final ShardPeerAddressResolver addressResolver) {
        this.shardName = shardName;
        this.shardId = shardId;
        this.persistence = persistence;
        this.initialPeerAddresses = initialPeerAddresses;
        this.datastoreContext = datastoreContext;
        this.builder = builder;
        this.addressResolver = addressResolver;
    }

    Props newProps() {
        Props props = requireNonNull(builder).id(shardId).peerAddresses(initialPeerAddresses)
                .datastoreContext(datastoreContext).schemaContextProvider(schemaContextProvider)
                .setPersistence(persistence).props();
        builder = null;
        return props;
    }

    String getShardName() {
        return shardName;
    }

    Persistence getPersistence() {
        return persistence;
    }

    @VisibleForTesting
    @Nullable public ActorRef getActor() {
        return actor;
    }

    void setActor(final ActorRef actor) {
        this.actor = actor;
    }

    ShardIdentifier getShardId() {
        return shardId;
    }

    void setLocalDataTree(final Optional<ReadOnlyDataTree> dataTree) {
        this.localShardDataTree = dataTree;
    }

    Optional<ReadOnlyDataTree> getLocalShardDataTree() {
        return localShardDataTree;
    }

    DatastoreContext getDatastoreContext() {
        return datastoreContext;
    }

    void setDatastoreContext(final DatastoreContext newDatastoreContext, final ActorRef sender) {
        this.datastoreContext = newDatastoreContext;
        if (actor != null) {
            LOG.debug("Sending new DatastoreContext to {}", shardId);
            actor.tell(this.datastoreContext, sender);
        }
    }

    void updatePeerAddress(final String peerId, final String peerAddress, final ActorRef sender) {
        LOG.info("updatePeerAddress for peer {} with address {}", peerId, peerAddress);

        if (actor != null) {
            LOG.debug("Sending PeerAddressResolved for peer {} with address {} to {}", peerId,
                    peerAddress, actor.path());

            actor.tell(new PeerAddressResolved(peerId, peerAddress), sender);
        }

        notifyOnShardInitializedCallbacks();
    }

    void peerDown(final MemberName memberName, final String peerId, final ActorRef sender) {
        if (actor != null) {
            actor.tell(new PeerDown(memberName, peerId), sender);
        }
    }

    void peerUp(final MemberName memberName, final String peerId, final ActorRef sender) {
        if (actor != null) {
            actor.tell(new PeerUp(memberName, peerId), sender);
        }
    }

    boolean isShardReady() {
        return !RaftState.Candidate.name().equals(role) && !Strings.isNullOrEmpty(role);
    }

    boolean isShardReadyWithLeaderId() {
        return leaderAvailable && isShardReady() && !RaftState.IsolatedLeader.name().equals(role)
                && !RaftState.PreLeader.name().equals(role)
                && (isLeader() || addressResolver.resolve(leaderId) != null);
    }

    boolean isShardInitialized() {
        return getActor() != null && actorInitialized;
    }

    boolean isLeader() {
        return Objects.equals(leaderId, shardId.toString());
    }

    String getSerializedLeaderActor() {
        if (isLeader()) {
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
        if (onShardInitializedSet.isEmpty()) {
            return;
        }

        boolean ready = isShardReadyWithLeaderId();

        LOG.debug("Shard {} is {} - notifying {} OnShardInitialized callbacks", shardId,
            ready ? "ready" : "initialized", onShardInitializedSet.size());

        Iterator<OnShardInitialized> iter = onShardInitializedSet.iterator();
        while (iter.hasNext()) {
            OnShardInitialized onShardInitialized = iter.next();
            if (!(onShardInitialized instanceof OnShardReady) || ready) {
                iter.remove();
                onShardInitialized.getTimeoutSchedule().cancel();
                onShardInitialized.getReplyRunnable().run();
            }
        }
    }

    void addOnShardInitialized(final OnShardInitialized onShardInitialized) {
        onShardInitializedSet.add(onShardInitialized);
    }

    void removeOnShardInitialized(final OnShardInitialized onShardInitialized) {
        onShardInitializedSet.remove(onShardInitialized);
    }

    void setRole(final String newRole) {
        this.role = newRole;

        notifyOnShardInitializedCallbacks();
    }

    String getRole() {
        return role;
    }

    void setFollowerSyncStatus(final boolean syncStatus) {
        this.followerSyncStatus = syncStatus;
    }

    boolean isInSync() {
        if (RaftState.Follower.name().equals(this.role)) {
            return followerSyncStatus;
        } else if (RaftState.Leader.name().equals(this.role)) {
            return true;
        }

        return false;
    }

    boolean setLeaderId(final String newLeaderId) {
        final boolean changed = !Objects.equals(this.leaderId, newLeaderId);
        this.leaderId = newLeaderId;
        if (newLeaderId != null) {
            this.leaderAvailable = true;
        }
        notifyOnShardInitializedCallbacks();

        return changed;
    }

    String getLeaderId() {
        return leaderId;
    }

    void setLeaderAvailable(final boolean leaderAvailable) {
        this.leaderAvailable = leaderAvailable;

        if (leaderAvailable) {
            notifyOnShardInitializedCallbacks();
        }
    }

    short getLeaderVersion() {
        return leaderVersion;
    }

    void setLeaderVersion(final short leaderVersion) {
        this.leaderVersion = leaderVersion;
    }

    boolean isActiveMember() {
        return activeMember;
    }

    void setActiveMember(final boolean isActiveMember) {
        this.activeMember = isActiveMember;
    }

    SchemaContext getSchemaContext() {
        return schemaContextProvider.getSchemaContext();
    }

    void setSchemaContext(final SchemaContext schemaContext) {
        schemaContextProvider.set(requireNonNull(schemaContext));
    }

    @VisibleForTesting
    Shard.AbstractBuilder<?, ?> getBuilder() {
        return builder;
    }

    @Override
    public String toString() {
        return "ShardInformation [shardId=" + shardId + ", leaderAvailable=" + leaderAvailable + ", actorInitialized="
                + actorInitialized + ", followerSyncStatus=" + followerSyncStatus + ", role=" + role + ", leaderId="
                + leaderId + ", activeMember=" + activeMember + "]";
    }


}
