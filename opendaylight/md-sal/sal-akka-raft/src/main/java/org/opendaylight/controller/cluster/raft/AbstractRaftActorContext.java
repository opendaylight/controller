/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.cluster.Cluster;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.controller.cluster.raft.spi.TermInfoStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for {@link RaftActorContext} implementations.
 */
@VisibleForTesting
abstract class AbstractRaftActorContext implements RaftActorContext {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRaftActorContext.class);
    private static final @NonNull LongSupplier JVM_MEMORY_RETRIEVER = () -> Runtime.getRuntime().maxMemory();

    private final Map<String, PeerInfo> peerInfoMap = new HashMap<>();
    private final @NonNull Executor executor;
    private final @NonNull DataPersistenceProvider persistenceProvider;
    private final @NonNull Consumer<ApplyState> applyStateConsumer;
    private final @NonNull FileBackedOutputStreamFactory fileBackedOutputStreamFactory;
    private final short payloadVersion;

    // Cached from LocalAccess instance
    private final @NonNull String id;
    private final @NonNull TermInfoStore termInformation;

    @VisibleForTesting
    private @NonNull LongSupplier totalMemoryRetriever = JVM_MEMORY_RETRIEVER;
    private @NonNull ReplicatedLog replicatedLog;
    private @NonNull ConfigParams configParams;

    private boolean dynamicServerConfiguration = false;
    private boolean votingMember = true;
    private int numVotingPeers = -1;

    // Snapshot manager will need to be created on demand as it needs raft actor context which cannot
    // be passed to it in the constructor
    private SnapshotManager snapshotManager;
    private RaftActorBehavior currentBehavior;
    private Optional<Cluster> cluster;
    private RaftActorLeadershipTransferCohort leadershipTransferCohort;

    AbstractRaftActorContext(final @NonNull LocalAccess localStore, final @NonNull Map<String, String> peerAddresses,
            final @NonNull ConfigParams configParams, final short payloadVersion,
            final @NonNull DataPersistenceProvider persistenceProvider,
            final @NonNull Consumer<ApplyState> applyStateConsumer, final @NonNull Executor executor) {
        id = localStore.memberId();
        termInformation = localStore.termInfoStore();
        this.executor = requireNonNull(executor);
        this.configParams = requireNonNull(configParams);
        this.payloadVersion = payloadVersion;
        this.persistenceProvider = requireNonNull(persistenceProvider);
        this.applyStateConsumer = requireNonNull(applyStateConsumer);

        fileBackedOutputStreamFactory = new FileBackedOutputStreamFactory(
            configParams.getFileBackedStreamingThreshold(), configParams.getTempFileDirectory());

        for (Map.Entry<String, String> e : requireNonNull(peerAddresses).entrySet()) {
            peerInfoMap.put(e.getKey(), new PeerInfo(e.getKey(), e.getValue(), VotingState.VOTING));
        }

        replicatedLog = new ReplicatedLogImpl(this);
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final Executor getExecutor() {
        return executor;
    }

    @Override
    public final short getPayloadVersion() {
        return payloadVersion;
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public final Cluster cluster() {
        var local = cluster;
        if (local == null) {
            try {
                local = Optional.of(Cluster.get(getActorSystem()));
            } catch (Exception e) {
                // An exception means there's no cluster configured. This will only happen in unit tests.
                LOG.debug("{}: Could not obtain Cluster", id, e);
                local = Optional.empty();
            }
            cluster = local;
        }
        return local.orElse(null);
    }

    @Override
    public final TermInfo termInfo() {
        return termInformation.currentTerm();
    }

    @Override
    public final void setTermInfo(final TermInfo newElectionInfo) {
        termInformation.setTerm(newElectionInfo);
    }

    @Override
    public final void persistTermInfo(final TermInfo newElectionInfo) throws IOException {
        termInformation.storeAndSetTerm(newElectionInfo);
    }

    @Override
    @Deprecated(forRemoval = true)
    public final void setReplicatedLog(final ReplicatedLog replicatedLog) {
        this.replicatedLog = replicatedLog;
    }

    @Override
    public final ReplicatedLog getReplicatedLog() {
        return replicatedLog;
    }

    @Override
    public final Collection<String> getPeerIds() {
        return peerInfoMap.keySet();
    }

    @Override
    public final Collection<PeerInfo> getPeers() {
        return peerInfoMap.values();
    }

    @Override
    public final PeerInfo getPeerInfo(final String peerId) {
        return peerInfoMap.get(peerId);
    }

    @Override
    public final String getPeerAddress(final String peerId) {
        String peerAddress;
        PeerInfo peerInfo = peerInfoMap.get(peerId);
        if (peerInfo != null) {
            peerAddress = peerInfo.getAddress();
            if (peerAddress == null) {
                peerAddress = configParams.getPeerAddressResolver().resolve(peerId);
                peerInfo.setAddress(peerAddress);
            }
        } else {
            peerAddress = configParams.getPeerAddressResolver().resolve(peerId);
        }

        return peerAddress;
    }

    @Override
    public final void updatePeerIds(final ClusterConfig serverConfig) {
        boolean newVotingMember = false;
        var currentPeers = new HashSet<>(getPeerIds());
        for (var server : serverConfig.serverInfo()) {
            if (id.equals(server.peerId())) {
                newVotingMember = server.isVoting();
            } else {
                final var votingState = server.isVoting() ? VotingState.VOTING : VotingState.NON_VOTING;
                if (currentPeers.contains(server.peerId())) {
                    getPeerInfo(server.peerId()).setVotingState(votingState);
                    currentPeers.remove(server.peerId());
                } else {
                    addToPeers(server.peerId(), null, votingState);
                }
            }
        }

        for (String peerIdToRemove : currentPeers) {
            removePeer(peerIdToRemove);
        }

        votingMember = newVotingMember;
        LOG.debug("{}: Updated server config: isVoting: {}, peers: {}", id, votingMember, peerInfoMap.values());

        setDynamicServerConfigurationInUse();
    }

    @Override
    public final ConfigParams getConfigParams() {
        return configParams;
    }

    @Override
    public final void addToPeers(final String peerId, final String address, final VotingState votingState) {
        peerInfoMap.put(peerId, new PeerInfo(peerId, address, votingState));
        numVotingPeers = -1;
    }

    @Override
    public final void removePeer(final String name) {
        if (id.equals(name)) {
            votingMember = false;
        } else {
            peerInfoMap.remove(name);
            numVotingPeers = -1;
        }
    }

    @Override
    public final ActorSelection getPeerActorSelection(final String peerId) {
        final var peerAddress = getPeerAddress(peerId);
        return peerAddress != null ? actorSelection(peerAddress) : null;
    }

    abstract ActorSelection actorSelection(@NonNull String path);

    @Override
    public final void setPeerAddress(final String peerId, final String peerAddress) {
        final var peerInfo = peerInfoMap.get(peerId);
        if (peerInfo != null) {
            LOG.info("{}: Peer address for peer {} set to {}", id, peerId, peerAddress);
            peerInfo.setAddress(peerAddress);
        }
    }

    // Non-final for testing
    @Override
    public SnapshotManager getSnapshotManager() {
        if (snapshotManager == null) {
            snapshotManager = new SnapshotManager(this);
        }
        return snapshotManager;
    }

    @Override
    public final Consumer<ApplyState> getApplyStateConsumer() {
        return applyStateConsumer;
    }

    @Override
    public FileBackedOutputStreamFactory getFileBackedOutputStreamFactory() {
        return fileBackedOutputStreamFactory;
    }

    @Override
    public RaftActorLeadershipTransferCohort getRaftActorLeadershipTransferCohort() {
        return leadershipTransferCohort;
    }

    @Override
    public final long getTotalMemory() {
        return totalMemoryRetriever.getAsLong();
    }

    @Override
    public final void setTotalMemoryRetriever(final LongSupplier retriever) {
        totalMemoryRetriever = retriever == null ? JVM_MEMORY_RETRIEVER : retriever;
    }

    @Override
    public final boolean hasFollowers() {
        return !getPeerIds().isEmpty();
    }

    @Override
    public DataPersistenceProvider getPersistenceProvider() {
        return persistenceProvider;
    }

    @Override
    public RaftPolicy getRaftPolicy() {
        return configParams.getRaftPolicy();
    }

    @Override
    public boolean isDynamicServerConfigurationInUse() {
        return dynamicServerConfiguration;
    }

    @Override
    public void setDynamicServerConfigurationInUse() {
        dynamicServerConfiguration = true;
    }

    @Override
    public ClusterConfig getPeerServerInfo(final boolean includeSelf) {
        if (!isDynamicServerConfigurationInUse()) {
            return null;
        }
        final var peers = getPeers();
        final var newConfig = ImmutableList.<ServerInfo>builderWithExpectedSize(peers.size() + (includeSelf ? 1 : 0));
        for (var peer : peers) {
            newConfig.add(new ServerInfo(peer.getId(), peer.isVoting()));
        }

        if (includeSelf) {
            newConfig.add(new ServerInfo(id, votingMember));
        }

        return new ClusterConfig(newConfig.build());
    }

    @Override
    public final boolean isVotingMember() {
        return votingMember;
    }

    @Override
    public final boolean anyVotingPeers() {
        if (numVotingPeers < 0) {
            numVotingPeers = 0;
            for (var info : getPeers()) {
                if (info.isVoting()) {
                    numVotingPeers++;
                }
            }
        }

        return numVotingPeers > 0;
    }

    @Override
    public final RaftActorBehavior getCurrentBehavior() {
        return currentBehavior;
    }

    // Non-final for testing
    void setCurrentBehavior(final RaftActorBehavior behavior) {
        currentBehavior = requireNonNull(behavior);
    }

    @Override
    @SuppressWarnings("checkstyle:hiddenField")
    public void setRaftActorLeadershipTransferCohort(final RaftActorLeadershipTransferCohort leadershipTransferCohort) {
        this.leadershipTransferCohort = leadershipTransferCohort;
    }

    @VisibleForTesting
    public final void setConfigParams(final ConfigParams configParams) {
        this.configParams = requireNonNull(configParams);
    }
}
