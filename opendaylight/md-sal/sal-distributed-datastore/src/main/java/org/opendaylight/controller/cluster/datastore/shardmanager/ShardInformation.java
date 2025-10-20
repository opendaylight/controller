/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.serialization.Serialization;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManager.OnShardInitialized;
import org.opendaylight.controller.cluster.datastore.shardmanager.ShardManager.OnShardReady;
import org.opendaylight.raft.api.RaftRole;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@VisibleForTesting
public final class ShardInformation {
    private static final Logger LOG = LoggerFactory.getLogger(ShardInformation.class);

    private final Set<OnShardInitialized> onShardInitializedSet = new HashSet<>();
    private final Map<String, String> initialPeerAddresses;
    private final ShardPeerAddressResolver addressResolver;
    private final ShardIdentifier shardId;
    private final @NonNull Path stateDir;
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

    private String role;
    private String leaderId;
    private short leaderVersion;

    private DatastoreContext datastoreContext;
    private Shard.AbstractBuilder<?, ?> builder;
    private boolean activeMember = true;


    ShardInformation(final Path stateDir, final String shardName, final ShardIdentifier shardId,
            final Map<String, String> initialPeerAddresses, final DatastoreContext datastoreContext,
            final Shard.AbstractBuilder<?, ?> builder, final ShardPeerAddressResolver addressResolver) {
        this.stateDir = requireNonNull(stateDir);
        this.shardName = shardName;
        this.shardId = shardId;
        this.initialPeerAddresses = initialPeerAddresses;
        this.datastoreContext = datastoreContext;
        this.builder = builder;
        this.addressResolver = addressResolver;
    }

    Props newProps() {
        Props props = requireNonNull(builder).id(shardId).peerAddresses(initialPeerAddresses)
                .datastoreContext(datastoreContext).schemaContextProvider(schemaContextProvider::modelContext)
                .props(stateDir);
        builder = null;
        return props;
    }

    String getShardName() {
        return shardName;
    }

    @VisibleForTesting
    public @Nullable ActorRef getActor() {
        return actor;
    }

    void setActor(final ActorRef actor) {
        this.actor = actor;
    }

    ShardIdentifier getShardId() {
        return shardId;
    }

    void setLocalDataTree(final ReadOnlyDataTree dataTree) {
        localShardDataTree = Optional.ofNullable(dataTree);
    }

    Optional<ReadOnlyDataTree> getLocalShardDataTree() {
        return localShardDataTree;
    }

    DatastoreContext getDatastoreContext() {
        return datastoreContext;
    }

    void setDatastoreContext(final DatastoreContext newDatastoreContext, final ActorRef sender) {
        datastoreContext = newDatastoreContext;
        if (actor != null) {
            LOG.debug("Sending new DatastoreContext to {}", shardId);
            actor.tell(datastoreContext, sender);
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

    boolean isShardReady() {
        return !RaftRole.Candidate.name().equals(role) && !Strings.isNullOrEmpty(role);
    }

    boolean isShardReadyWithLeaderId() {
        return leaderAvailable && isShardReady() && !RaftRole.IsolatedLeader.name().equals(role)
            && !RaftRole.PreLeader.name().equals(role) && (isLeader() || addressResolver.resolve(leaderId) != null);
    }

    boolean isShardInitialized() {
        return getActor() != null && actorInitialized;
    }

    boolean isLeader() {
        return Objects.equals(leaderId, shardId.toString());
    }

    String getSerializedLeaderActor() {
        return isLeader() ? Serialization.serializedActorPath(getActor()) : addressResolver.resolve(leaderId);
    }

    void setActorInitialized() {
        LOG.debug("Shard {} is initialized", shardId);

        actorInitialized = true;

        notifyOnShardInitializedCallbacks();
    }

    private void notifyOnShardInitializedCallbacks() {
        if (onShardInitializedSet.isEmpty()) {
            return;
        }

        final boolean ready = isShardReadyWithLeaderId();
        final String readyStr = ready ? "ready" : "initialized";
        LOG.debug("Shard {} is {} - notifying {} OnShardInitialized callbacks", shardId, readyStr,
            onShardInitializedSet.size());

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
        role = newRole;

        notifyOnShardInitializedCallbacks();
    }

    String getRole() {
        return role;
    }

    void setFollowerSyncStatus(final boolean syncStatus) {
        followerSyncStatus = syncStatus;
    }

    boolean isInSync() {
        if (RaftRole.Follower.name().equals(role)) {
            return followerSyncStatus;
        }
        if (RaftRole.Leader.name().equals(role)) {
            return true;
        }
        return false;
    }

    boolean setLeaderId(final String newLeaderId) {
        final boolean changed = !Objects.equals(leaderId, newLeaderId);
        leaderId = newLeaderId;
        if (newLeaderId != null) {
            leaderAvailable = true;
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
        activeMember = isActiveMember;
    }

    EffectiveModelContext getSchemaContext() {
        return schemaContextProvider.modelContext();
    }

    void setModelContext(final EffectiveModelContext modelContext) {
        schemaContextProvider.set(requireNonNull(modelContext));
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
