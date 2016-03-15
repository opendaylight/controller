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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import java.util.Collection;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.slf4j.Logger;

/**
 * The RaftActorContext contains that portion of the RaftActors state that
 * needs to be shared with it's behaviors. A RaftActorContext should NEVER be
 * used in any actor context outside the RaftActor that constructed it.
 */
public interface RaftActorContext {
    /**
     * Create a new local actor
     * @param props
     * @return a reference to the newly created actor
     */
    ActorRef actorOf(Props props);

    /**
     * Create a actor selection
     * @param path
     * @return an actor selection for the given actor path
     */
    ActorSelection actorSelection(String path);

    /**
     * Get the identifier for the RaftActor. This identifier represents the
     * name of the actor whose common state is being shared. For example the
     * id could be 'inventory'
     *
     * @return the identifier
     */
    String getId();

    /**
     * @return A reference to the RaftActor itself. This could be used to send messages
     * to the RaftActor
     */
    ActorRef getActor();

    /**
     * @return the ElectionTerm information
     */
    ElectionTerm getTermInformation();

    /**
     * @return index of highest log entry known to be committed (initialized to 0, increases monotonically)
     */
    long getCommitIndex();


    /**
     * @param commitIndex new commit index
     */
    void setCommitIndex(long commitIndex);

    /**
     * @return index of highest log entry applied to state machine (initialized to 0, increases monotonically)
     */
    long getLastApplied();


    /**
     * @param lastApplied the index of the last log entry that was applied to the state
     */
    void setLastApplied(long lastApplied);

    /**
     *
     * @param replicatedLog the replicated log of the current RaftActor
     */
    void setReplicatedLog(ReplicatedLog replicatedLog);

    /**
     * @return A representation of the log
     */
    ReplicatedLog getReplicatedLog();

    /**
     * @return The ActorSystem associated with this context
     */
    ActorSystem getActorSystem();

    /**
     * @return the logger to be used for logging messages to a log file
     */
    Logger getLogger();

    /**
     * Get the address of the peer as a String. This is the same format in
     * which a consumer would provide the address
     *
     * @param peerId
     * @return The address of the peer or null if the address has not yet been
     *         resolved
     */
    String getPeerAddress(String peerId);

    /**
     * @param serverCfgPayload
     */
    void updatePeerIds(ServerConfigurationPayload serverCfgPayload);

    /**
     * @return list of PeerInfo
     */
    Collection<PeerInfo> getPeers();

    /**
     * @return the list of peer IDs.
     */
    Collection<String> getPeerIds();

    /**
     * Get the PeerInfo for the given peer.
     *
     * @param peerId
     * @return the PeerInfo
     */
    PeerInfo getPeerInfo(String peerId);

    /**
     * Add to actor peers
     *
     * @param name
     * @param address
     */
    void addToPeers(String name, String address, VotingState votingState);

    /**
     *
     * @param name
     */
    void removePeer(String name);

    /**
     * Given a peerId return the corresponding actor
     * <p>
     *
     *
     * @param peerId
     * @return The actorSelection corresponding to the peer or null if the
     *         address has not yet been resolved
     */
    ActorSelection getPeerActorSelection(String peerId);

    /**
     * Set Peer Address can be called at a later time to change the address of
     * a known peer.
     *
     * <p>
     * Throws an IllegalStateException if the peer is unknown
     *
     * @param peerId
     * @param peerAddress
     */
    void setPeerAddress(String peerId, String peerAddress);

    /**
     * @return ConfigParams
     */
    ConfigParams getConfigParams();

    /**
     *
     * @return the SnapshotManager for this RaftActor
     */
    SnapshotManager getSnapshotManager();

    /**
     *
     * @return the DataPersistenceProvider for this RaftActor
     */
    DataPersistenceProvider getPersistenceProvider();

    /**
     *
     * @return true if the RaftActor has followers else false
     */
    boolean hasFollowers();

    /**
     *
     * @return the total memory used by the ReplicatedLog
     */
    long getTotalMemory();

    /**
     *
     * @param retriever a supplier of the total memory metric
     */
    @VisibleForTesting
    void setTotalMemoryRetriever(Supplier<Long> retriever);

    /**
     *
     * @return the payload version to be used when replicating data
     */
    short getPayloadVersion();

    /**
     * @return an implementation of the RaftPolicy so that the Raft code can be adapted
     */
    RaftPolicy getRaftPolicy();

    /**
     * @return true if there are any dynamic server configuration changes available,
     *  false if static peer configurations are still in use
     */
    boolean isDynamicServerConfigurationInUse();

    /**
     * Configures the dynamic server configurations are avaialble for the RaftActor
     */
    void setDynamicServerConfigurationInUse();

    /**
     * @return the RaftActor's peer information as a ServerConfigurationPayload if the
     * dynamic server configurations are available, otherwise returns null
     */
    @Nullable ServerConfigurationPayload getPeerServerInfo(boolean includeSelf);

    /**
     * @return true if this RaftActor is a voting member of the cluster, false otherwise.
     */
    boolean isVotingMember();

    /**
     * @return current behavior attached to the raft actor.
     */
    RaftActorBehavior getCurrentBehavior();
}
