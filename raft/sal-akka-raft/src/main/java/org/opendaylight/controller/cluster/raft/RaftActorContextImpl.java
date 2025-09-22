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
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
import org.apache.pekko.actor.ActorContext;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.cluster.Cluster;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.EntryStore;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.TermInfoStore;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.raft.spi.FileBackedOutputStreamFactory;
import org.opendaylight.raft.spi.RaftPolicy;
import org.opendaylight.raft.spi.RestrictedObjectStreams;
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

    // Cached from LocalAccess instance
    private final @NonNull String id;
    private final @NonNull TermInfoStore termInformation;

    private final @NonNull PeerInfos peerInfos;

    private ReplicatedLog replicatedLog;

    private ConfigParams configParams;

    @VisibleForTesting
    private LongSupplier totalMemoryRetriever = JVM_MEMORY_RETRIEVER;

    // Snapshot manager will need to be created on demand as it needs raft actor context which cannot
    // be passed to it in the constructor
    private SnapshotManager snapshotManager;

    private final @NonNull RestrictedObjectStreams objectStreams;

    private final @NonNull PersistenceProvider persistenceProvider;

    private final short payloadVersion;

    private RaftActorBehavior currentBehavior;

    private Optional<Cluster> cluster;

    private final @NonNull ApplyEntryMethod applyEntryMethod;

    private final FileBackedOutputStreamFactory fileBackedOutputStreamFactory;

    private RaftActorLeadershipTransferCohort leadershipTransferCohort;

    RaftActorContextImpl(final ActorRef actor, final ActorContext context, final @NonNull LocalAccess localStore,
            final @NonNull PeerInfos peerInfos, final @NonNull ConfigParams configParams, final short payloadVersion,
            final @NonNull RestrictedObjectStreams objectStreams,
            final @NonNull PersistenceProvider persistenceProvider, final @NonNull ApplyEntryMethod applyEntryMethod) {
        this.actor = actor;
        this.context = context;
        id = localStore.memberId();
        termInformation = localStore.termInfoStore();
        this.configParams = requireNonNull(configParams);
        this.payloadVersion = payloadVersion;
        this.objectStreams = requireNonNull(objectStreams);
        this.persistenceProvider = requireNonNull(persistenceProvider);
        this.applyEntryMethod = requireNonNull(applyEntryMethod);
        this.peerInfos = requireNonNull(peerInfos);

        fileBackedOutputStreamFactory = new FileBackedOutputStreamFactory(
                configParams.getFileBackedStreamingThreshold(), configParams.getTempFileDirectory());

        replicatedLog = new ReplicatedLogImpl(this);
    }

    @VisibleForTesting
    public RaftActorContextImpl(final ActorRef actor, final ActorContext context, final @NonNull LocalAccess localStore,
            final @NonNull Map<String, String> peerAddresses, final @NonNull ConfigParams configParams,
            final short payloadVersion, final @NonNull RestrictedObjectStreams objectStreams,
            final @NonNull PersistenceProvider persistenceProvider, final @NonNull ApplyEntryMethod applyEntryMethod) {
        this(actor, context, localStore, new PeerInfos(localStore.memberId(), peerAddresses), configParams,
            payloadVersion, objectStreams, persistenceProvider, applyEntryMethod);
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
        return peerInfos.peerIds();
    }

    @Override
    public Collection<PeerInfo> getPeers() {
        return peerInfos.peerInfos();
    }

    @Override
    public PeerInfo getPeerInfo(final String peerId) {
        return peerInfos.lookupPeerInfo(peerId);
    }

    @Override
    public String getPeerAddress(final String peerId) {
        final var peerInfo = peerInfos.lookupPeerInfo(peerId);
        if (peerInfo == null) {
            return configParams.getPeerAddressResolver().resolve(peerId);
        }

        var peerAddress = peerInfo.getAddress();
        if (peerAddress == null) {
            peerAddress = configParams.getPeerAddressResolver().resolve(peerId);
            peerInfo.setAddress(peerAddress);
        }
        return peerAddress;
    }

    @Override
    public void updateVotingConfig(final VotingConfig votingConfig) {
        peerInfos.updateVotingConfig(votingConfig);
        setDynamicServerConfigurationInUse();
    }

    @Override
    public ConfigParams getConfigParams() {
        return configParams;
    }

    @Override
    public void addToPeers(final String peerId, final String address, final VotingState votingState) {
        peerInfos.addPeer(peerId, address, votingState);
    }

    @Override
    public void removePeer(final String name) {
        peerInfos.removePeer(name);
    }

    @Override
    public final ActorSelection getPeerActorSelection(final String peerId) {
        final var peerAddress = getPeerAddress(peerId);
        return peerAddress != null ? actorSelection(peerAddress) : null;
    }

    @Override
    public void setPeerAddress(final String peerId, final String peerAddress) {
        peerInfos.setPeerAddress(peerId, peerAddress);
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
        return !getPeerIds().isEmpty();
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
    public RestrictedObjectStreams objectStreams() {
        return objectStreams;
    }

    @Override
    public RaftPolicy getRaftPolicy() {
        return configParams.getRaftPolicy();
    }

    @Override
    public boolean isDynamicServerConfigurationInUse() {
        return peerInfos.dynamicServerConfiguration();
    }

    @Override
    public void setDynamicServerConfigurationInUse() {
        peerInfos.setDynamicServerConfiguration();
    }

    @Override
    public VotingConfig getPeerServerInfo(final boolean includeSelf) {
        return peerInfos.votingConfig(includeSelf);
    }

    @Override
    public boolean isVotingMember() {
        return peerInfos.votingMember();
    }

    @Override
    public boolean anyVotingPeers() {
        return peerInfos.anyVotingPeers();
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
