/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.function.LongSupplier;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.cluster.Cluster;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.opendaylight.controller.cluster.raft.spi.EntryStore;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.TermInfoStore;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.raft.spi.FileBackedOutputStreamFactory;

/**
 * The RaftActorContext contains that portion of the RaftActors state that
 * needs to be shared with it's behaviors. A RaftActorContext should NEVER be
 * used in any actor context outside the RaftActor that constructed it.
 */
public interface RaftActorContext {
    /**
     * Returns the identifier for the RaftActor. This identifier represents the
     * name of the actor whose common state is being shared.
     *
     * @return the identifier
     */
    @NonNull String getId();

    /**
     * Returns the reference to the RaftActor.
     *
     * @return the reference to the RaftActor itself. This can be used to send messages to the RaftActor
     */
    ActorRef getActor();

    /**
     * Return an Executor which is guaranteed to run tasks in the context of {@link #getActor()}.
     *
     * @return An executor.
     */
    @NonNull Executor getExecutor();

    /**
     * The Pekko {@link Cluster} singleton for the actor system if one is configured.
     *
     * @return the Cluster instance, or {@code null}
     */
    @Nullable Cluster cluster();

    /**
     * Return current term. This method is equivalent to {@code termInfo().term()}.
     *
     * @return current term
     */
    default long currentTerm() {
        return termInfo().term();
    }

    /**
     * Returns the current term {@link TermInfo}.
     *
     * @return the {@link TermInfo}
     */
    @NonNull TermInfo termInfo();

    /**
     * Sets, but does not persist, a {@link TermInfo}, so that {@link #termInfo()} returns it, unless we undergo
     * recovery, in which case a prior {@link TermInfo} may be returned.
     *
     * @implSpec
     *     Implementations need to route this request to the underlying {@link TermInfoStore#setTerm(TermInfo)}.
     *
     * @param termInfo {@link TermInfo} to set
     */
    void setTermInfo(@NonNull TermInfo termInfo);

    /**
     * Sets and persists a {@link TermInfo}, so that {@link #termInfo()} returns it, even if we undergo recovery.
     *
     * @implSpec
     *     Implementations need to route this request to the underlying {@link TermInfoStore#storeAndSetTerm(TermInfo)}.
     *
     * @param termInfo {@link TermInfo} to persist
     * @throws IOException when an I/O error occurs
     */
    void persistTermInfo(@NonNull TermInfo termInfo) throws IOException;

    /**
     * Returns the ReplicatedLog instance.
     *
     * @return the ReplicatedLog instance.
     */
    @NonNull ReplicatedLog getReplicatedLog();

    /**
     * Returns the The ActorSystem associated with this context.
     *
     * @return the ActorSystem.
     */
    @NonNull ActorSystem getActorSystem();

    /**
     * Gets the address of a peer as a String. This is the same format in which a consumer would provide the address.
     *
     * @param peerId the id of the peer.
     * @return the address of the peer or null if the address has not yet been resolved.
     */
    @NonNullByDefault
    @Nullable String getPeerAddress(String peerId);

    /**
     * Updates the peers and information to match the given {@link VotingConfig}.
     *
     * @param votingConfig the {@link VotingConfig}
     */
    @NonNullByDefault
    void updateVotingConfig(VotingConfig votingConfig);

    /**
     * Returns the PeerInfo instances for each peer.
     *
     * @return list of PeerInfo
     */
    @NonNullByDefault
    Collection<PeerInfo> getPeers();

    /**
     * Returns the id's for each peer.
     *
     * @return the list of peer id's.
     */
    @NonNullByDefault
    Collection<String> getPeerIds();

    /**
     * Returns the PeerInfo for the given peer.
     *
     * @param peerId the id of the peer
     * @return the PeerInfo or null if not found
     */
    @NonNullByDefault
    @Nullable PeerInfo getPeerInfo(String peerId);

    /**
     * Adds a new peer.
     *
     * @param peerId the id of the new peer.
     * @param address the address of the new peer.
     * @param votingState the VotingState of the new peer.
     */
    @NonNullByDefault
    void addToPeers(String peerId, @Nullable String address, VotingState votingState);

    /**
     * Removes a peer.
     *
     * @param peerId the id of the peer to remove.
     */
    @NonNullByDefault
    void removePeer(String peerId);

    /**
     * Returns an ActorSelection for a peer.
     *
     * @param peerId the id of the peer.
     * @return the actorSelection corresponding to the peer or null if the address has not yet been resolved.
     */
    @Nullable ActorSelection getPeerActorSelection(String peerId);

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
    @NonNull ConfigParams getConfigParams();

    /**
     * Returns the SnapshotManager instance.
     *
     * @return the SnapshotManager instance.
     */
    @NonNull SnapshotManager getSnapshotManager();

    /**
     * Returns the DataPersistenceProvider instance.
     *
     * @return the DataPersistenceProvider instance.
     */
    @Deprecated(since = "11.0.0", forRemoval = true)
    @VisibleForTesting
    @NonNull PersistenceProvider getPersistenceProvider();

    /**
     * Returns the {@link EntryStore}.
     *
     * @return the {@link EntryStore}
     */
    @NonNull EntryStore entryStore();

    /**
     * Returns the {@link SnapshotStore}.
     *
     * @return the {@link SnapshotStore}
     */
    @NonNull SnapshotStore snapshotStore();

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
    @NonNull RaftPolicy getRaftPolicy();

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
     * Returns the peer information as a ClusterConfig if dynamic server configurations have been applied.
     *
     * @param includeSelf include this peer's info.
     * @return the peer information as a ClusterConfig or null if no dynamic server configurations have been applied.
     */
    @Nullable VotingConfig getPeerServerInfo(boolean includeSelf);

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

    /**
     * Returns the consumer of ApplyState operations. This is invoked by a behavior when a log entry needs to be
     * applied to the state.
     *
     * @return the {@link ApplyEntryMethod}
     */
    ApplyEntryMethod applyEntryMethod();

    /**
     * Returns the {@link FileBackedOutputStreamFactory} instance with a common configuration.
     *
     * @return the {@link FileBackedOutputStreamFactory}
     */
    @NonNull FileBackedOutputStreamFactory getFileBackedOutputStreamFactory();

    /**
     * Returns the RaftActorLeadershipTransferCohort if leadership transfer is in progress.
     *
     * @return the RaftActorLeadershipTransferCohort if leadership transfer is in progress, null otherwise
     */
    @Nullable RaftActorLeadershipTransferCohort getRaftActorLeadershipTransferCohort();

    /**
     * Sets the RaftActorLeadershipTransferCohort for transferring leadership.
     *
     * @param leadershipTransferCohort the RaftActorLeadershipTransferCohort or null to clear the existing one
     */
    void setRaftActorLeadershipTransferCohort(@Nullable RaftActorLeadershipTransferCohort leadershipTransferCohort);
}
