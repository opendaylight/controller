/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.Optional;
import java.util.function.LongSupplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.slf4j.Logger;

/**
 * The RaftActorContext contains that portion of the RaftActors state that
 * needs to be shared with it's behaviors. A RaftActorContext should NEVER be
 * used in any actor context outside the RaftActor that constructed it.
 */
public interface RaftActorContext {
    /**
     * Creates a new local actor.
     *
     * @param props the Props used to create the actor.
     * @return a reference to the newly created actor.
     */
    ActorRef actorOf(Props props);

    /**
     * Creates an actor selection.
     *
     * @param path the path.
     * @return an actor selection for the given actor path.
     */
    ActorSelection actorSelection(String path);

    /**
     * Returns the identifier for the RaftActor. This identifier represents the
     * name of the actor whose common state is being shared.
     *
     * @return the identifier
     */
    String getId();

    /**
     * Returns the reference to the RaftActor.
     *
     * @return A reference to the RaftActor itself. This could be used to send messages
     * to the RaftActor
     */
    ActorRef getActor();

    /**
     * The akka Cluster singleton for the actor system if one is configured.
     *
     * @return an Optional containing the Cluster instance is present.
     */
    Optional<Cluster> getCluster();

    /**
     * Returns the current ElectionTerm information.
     *
     * @return the ElectionTerm.
     */
    @Nonnull
    ElectionTerm getTermInformation();

    /**
     * Returns the index of highest log entry known to be committed.
     *
     * @return index of highest log entry known to be committed.
     */
    long getCommitIndex();


    /**
     * Sets the index of highest log entry known to be committed.
     *
     * @param commitIndex new commit index
     */
    void setCommitIndex(long commitIndex);

    /**
     * Returns index of highest log entry applied to state machine.
     *
     * @return index of highest log entry applied to state machine.
     */
    long getLastApplied();

    /**
     * Sets index of highest log entry applied to state machine.
     *
     * @param lastApplied the new applied index.
     */
    void setLastApplied(long lastApplied);

    /**
     * Sets the ReplicatedLog instance.
     *
     * @param replicatedLog the ReplicatedLog instance.
     */
    void setReplicatedLog(@Nonnull ReplicatedLog replicatedLog);

    /**
     * Returns the ReplicatedLog instance.
     *
     * @return the ReplicatedLog instance.
     */
    @Nonnull
    ReplicatedLog getReplicatedLog();

    /**
     * Returns the The ActorSystem associated with this context.
     *
     * @return the ActorSystem.
     */
    @Nonnull
    ActorSystem getActorSystem();

    /**
     * Returns the logger to be used for logging messages.
     *
     * @return the logger.
     */
    @Nonnull
    Logger getLogger();

    /**
     * Gets the address of a peer as a String. This is the same format in which a consumer would provide the address.
     *
     * @param peerId the id of the peer.
     * @return the address of the peer or null if the address has not yet been resolved.
     */
    @Nullable
    String getPeerAddress(String peerId);

    /**
     * Updates the peers and information to match the given ServerConfigurationPayload.
     *
     * @param serverCfgPayload the ServerConfigurationPayload.
     */
    void updatePeerIds(ServerConfigurationPayload serverCfgPayload);

    /**
     * Returns the PeerInfo instances for each peer.
     *
     * @return list of PeerInfo
     */
    @Nonnull
    Collection<PeerInfo> getPeers();

    /**
     * Returns the id's for each peer.
     *
     * @return the list of peer id's.
     */
    @Nonnull
    Collection<String> getPeerIds();

    /**
     * Returns the PeerInfo for the given peer.
     *
     * @param peerId
     * @return the PeerInfo or null if not found.
     */
    @Nullable
    PeerInfo getPeerInfo(String peerId);

    /**
     * Adds a new peer.
     *
     * @param id the id of the new peer.
     * @param address the address of the new peer.
     * @param votingState the VotingState of the new peer.
     */
    void addToPeers(String id, String address, VotingState votingState);

    /**
     * Removes a peer.
     *
     * @param id the id of the peer to remove.
     */
    void removePeer(String id);

    /**
     * Returns an ActorSelection for a peer.
     *
     * @param peerId the id of the peer.
     * @return the actorSelection corresponding to the peer or null if the address has not yet been resolved.
     */
    @Nullable
    ActorSelection getPeerActorSelection(String peerId);

    /**
     * Sets the address of a peer.
     *
     * @param peerId the id of the peer.
     * @param peerAddress the address of the peer.
     */
    void setPeerAddress(String peerId, String peerAddress);

    /**
     * Returns the ConfigParams instance.
     *
     * @return the ConfigParams instance.
     */
    @Nonnull
    ConfigParams getConfigParams();

    /**
     * Returns the SnapshotManager instance.
     *
     * @return the SnapshotManager instance.
     */
    @Nonnull
    SnapshotManager getSnapshotManager();

    /**
     * Returns the DataPersistenceProvider instance.
     *
     * @return the DataPersistenceProvider instance.
     */
    @Nonnull
    DataPersistenceProvider getPersistenceProvider();

    /**
     * Determines if there are any peer followers.
     *
     * @return true if there are followers otherwise false.
     */
    boolean hasFollowers();

    /**
     * Returns the total available memory for use in calculations. Normally this returns JVM's max memory but can be
     * overridden for unit tests.
     *
     * @return the total memory.
     */
    long getTotalMemory();

    /**
     * Sets the retriever of the total memory metric.
     *
     * @param retriever a supplier of the total memory metric.
     */
    @VisibleForTesting
    void setTotalMemoryRetriever(LongSupplier retriever);

    /**
     * Returns the payload version to be used when replicating data.
     *
     * @return the payload version.
     */
    short getPayloadVersion();

    /**
     * Returns the RaftPolicy used to determine certain Raft behaviors.
     *
     * @return the RaftPolicy instance.
     */
    @Nonnull
    RaftPolicy getRaftPolicy();

    /**
     * Determines if there have been any dynamic server configuration changes applied.
     *
     * @return true if dynamic server configuration changes have been applied, false otherwise, meaning that static
     *         peer configuration is still in use.
     */
    boolean isDynamicServerConfigurationInUse();

    /**
     * Sets that dynamic server configuration changes have been applied.
     */
    void setDynamicServerConfigurationInUse();

    /**
     * Returns the peer information as a ServerConfigurationPayload if dynamic server configurations have been applied.
     *
     * @param includeSelf include this peer's info.
     * @return the peer information as a ServerConfigurationPayload or null if no dynamic server configurations have
     *         been applied.
     */
    @Nullable
    ServerConfigurationPayload getPeerServerInfo(boolean includeSelf);

    /**
     * Determines if this peer is a voting member of the cluster.
     *
     * @return true if this peer is a voting member, false otherwise.
     */
    boolean isVotingMember();

    /**
     * Determines if there are any voting peers.
     *
     * @return true if there are any voting peers, false otherwise.
     */
    boolean anyVotingPeers();

    /**
     * Returns the current behavior attached to the RaftActor.
     *
     * @return current behavior.
     */
    RaftActorBehavior getCurrentBehavior();
}
