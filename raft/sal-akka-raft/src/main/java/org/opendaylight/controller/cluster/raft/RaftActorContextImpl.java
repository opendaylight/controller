/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.LongSupplier;
import org.apache.pekko.actor.ActorContext;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.cluster.Cluster;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.opendaylight.controller.cluster.raft.spi.EntryStore;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.TermInfoStore;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.raft.spi.FileBackedOutputStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the RaftActorContext interface.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
// Non-final for testing
public class RaftActorContextImpl implements RaftActorContext {
    private static final Logger LOG = LoggerFactory.getLogger(RaftActorContextImpl.class);
    private static final LongSupplier JVM_MEMORY_RETRIEVER = () -> Runtime.getRuntime().maxMemory();

    private final ActorRef actor;

    private final ActorContext context;

    private final @NonNull Executor executor;

    // Cached from LocalAccess instance
    private final @NonNull String id;
    private final @NonNull TermInfoStore termInformation;

    private ReplicatedLog replicatedLog;

    private final @NonNull PeerInfos peers;

    private ConfigParams configParams;

    private boolean dynamicServerConfiguration = false;

    @VisibleForTesting
    private LongSupplier totalMemoryRetriever = JVM_MEMORY_RETRIEVER;

    // Snapshot manager will need to be created on demand as it needs raft actor context which cannot
    // be passed to it in the constructor
    private SnapshotManager snapshotManager;

    private final @NonNull PersistenceProvider persistenceProvider;

    private final short payloadVersion;

    private boolean votingMember = true;

    private RaftActorBehavior currentBehavior;

    private Optional<Cluster> cluster;

    private final @NonNull ApplyEntryMethod applyEntryMethod;

    private final FileBackedOutputStreamFactory fileBackedOutputStreamFactory;

    private RaftActorLeadershipTransferCohort leadershipTransferCohort;

    public RaftActorContextImpl(final ActorRef actor, final ActorContext context, final @NonNull LocalAccess localStore,
            final @NonNull Map<String, String> peerAddresses, final @NonNull ConfigParams configParams,
            final short payloadVersion, final @NonNull PersistenceProvider persistenceProvider,
            final @NonNull ApplyEntryMethod applyEntryMethod, final @NonNull Executor executor) {
        this.actor = actor;
        this.context = context;
        id = localStore.memberId();
        termInformation = localStore.termInfoStore();
        this.executor = requireNonNull(executor);
        this.configParams = requireNonNull(configParams);
        this.payloadVersion = payloadVersion;
        this.persistenceProvider = requireNonNull(persistenceProvider);
        this.applyEntryMethod = requireNonNull(applyEntryMethod);

        fileBackedOutputStreamFactory = new FileBackedOutputStreamFactory(
                configParams.getFileBackedStreamingThreshold(), configParams.getTempFileDirectory());

        peers = PeerInfos.ofMembers(id, peerAddresses);
        replicatedLog = new ReplicatedLogImpl(this);
    }

    @Override
    public short getPayloadVersion() {
        return payloadVersion;
    }

    public void setConfigParams(final ConfigParams configParams) {
        this.configParams = configParams;
    }

    ActorSelection actorSelection(final String path) {
        return context.actorSelection(path);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ActorRef getActor() {
        return actor;
    }

    @Override
    public final Executor getExecutor() {
        return executor;
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
    public TermInfo termInfo() {
        return termInformation.currentTerm();
    }

    @Override
    public void setTermInfo(final TermInfo newElectionInfo) {
        termInformation.setTerm(newElectionInfo);
    }

    @Override
    public void persistTermInfo(final TermInfo newElectionInfo) throws IOException {
        termInformation.storeAndSetTerm(newElectionInfo);
    }

    @VisibleForTesting
    @Deprecated(forRemoval = true)
    public final void resetReplicatedLog(final @NonNull ReplicatedLog newState) {
        replicatedLog = requireNonNull(newState);
    }

    @Override
    public ReplicatedLog getReplicatedLog() {
        return replicatedLog;
    }

    @Override
    public ActorSystem getActorSystem() {
        return context.system();
    }

    @Override
    public Collection<String> getPeerIds() {
        return peers.peerIds();
    }

    @Override
    public Collection<PeerInfo> getPeers() {
        return peers.peerInfos();
    }

    @Override
    public PeerInfo getPeerInfo(final String peerId) {
        return peers.lookupPeerInfo(peerId);
    }

    @Override
    public String getPeerAddress(final String peerId) {
        final var peerInfo = getPeerInfo(peerId);
        if (peerInfo == null) {
            return configParams.getPeerAddressResolver().resolve(peerId);
        }

        final var existing = peerInfo.getAddress();
        if (existing != null) {
            return existing;
        }

        final var resolved = configParams.getPeerAddressResolver().resolve(peerId);
        peerInfo.setAddress(resolved);
        return resolved;
    }

    @Override
    public void updateVotingConfig(final VotingConfig votingConfig) {
        votingMember = peers.applyVotingConfig(votingConfig.serverInfo());
        LOG.debug("{}: Updated server config: isVoting: {}, peers: {}", id, votingMember, peers.peerInfos());

        setDynamicServerConfigurationInUse();
    }

    @Override
    public ConfigParams getConfigParams() {
        return configParams;
    }

    @Override
    public void addToPeers(final String peerId, final String address, final VotingState votingState) {
        peers.setPeerInfo(peerId, address, votingState);
    }

    @Override
    public void removePeer(final String name) {
        if (id.equals(name)) {
            votingMember = false;
        } else {
            peers.removePeerInfo(name);
        }
    }

    @Override
    public final ActorSelection getPeerActorSelection(final String peerId) {
        final var peerAddress = getPeerAddress(peerId);
        return peerAddress != null ? actorSelection(peerAddress) : null;
    }

    @Override
    public void setPeerAddress(final String peerId, final String peerAddress) {
        final var peerInfo = peers.lookupPeerInfo(peerId);
        if (peerInfo != null) {
            LOG.info("{}: Peer address for peer {} set to {}", id, peerId, peerAddress);
            peerInfo.setAddress(peerAddress);
        }
    }

    @Override
    public SnapshotManager getSnapshotManager() {
        if (snapshotManager == null) {
            snapshotManager = new SnapshotManager(this);
        }
        return snapshotManager;
    }

    @Override
    public long getTotalMemory() {
        return totalMemoryRetriever.getAsLong();
    }

    @Override
    public void setTotalMemoryRetriever(final LongSupplier retriever) {
        totalMemoryRetriever = retriever == null ? JVM_MEMORY_RETRIEVER : retriever;
    }

    @Override
    public boolean hasFollowers() {
        return !peers.isEmpty();
    }

    @Override
    @Deprecated(since = "11.0.0", forRemoval = true)
    public PersistenceProvider getPersistenceProvider() {
        return persistenceProvider;
    }

    @Override
    public EntryStore entryStore() {
        return persistenceProvider.entryStore();
    }

    @Override
    public SnapshotStore snapshotStore() {
        return persistenceProvider.snapshotStore();
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
    public VotingConfig getPeerServerInfo(final boolean includeSelf) {
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

        return new VotingConfig(newConfig.build());
    }

    @Override
    public boolean isVotingMember() {
        return votingMember;
    }

    @Override
    public boolean anyVotingPeers() {
        return peers.anyVotingPeers();
    }

    @Override
    public RaftActorBehavior getCurrentBehavior() {
        return currentBehavior;
    }

    void setCurrentBehavior(final RaftActorBehavior behavior) {
        currentBehavior = requireNonNull(behavior);
    }

    @Override
    public ApplyEntryMethod applyEntryMethod() {
        return applyEntryMethod;
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
    @SuppressWarnings("checkstyle:hiddenField")
    public void setRaftActorLeadershipTransferCohort(final RaftActorLeadershipTransferCohort leadershipTransferCohort) {
        this.leadershipTransferCohort = leadershipTransferCohort;
    }
}
