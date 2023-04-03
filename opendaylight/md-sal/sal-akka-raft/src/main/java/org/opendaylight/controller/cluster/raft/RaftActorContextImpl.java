/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.io.FileBackedOutputStreamFactory;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.slf4j.Logger;

/**
 * Implementation of the RaftActorContext interface.
 *
 * @author Moiz Raja
 * @author Thomas Pantelis
 */
public class RaftActorContextImpl implements RaftActorContext {
    private static final LongSupplier JVM_MEMORY_RETRIEVER = () -> Runtime.getRuntime().maxMemory();

    private final ActorRef actor;

    private final ActorContext context;

    private final @NonNull Executor executor;

    private final String id;

    private final ElectionTerm termInformation;

    private long commitIndex;

    private long lastApplied;

    private ReplicatedLog replicatedLog;

    private final Map<String, PeerInfo> peerInfoMap = new HashMap<>();

    private final Logger log;

    private ConfigParams configParams;

    private boolean dynamicServerConfiguration = false;

    @VisibleForTesting
    private LongSupplier totalMemoryRetriever = JVM_MEMORY_RETRIEVER;

    // Snapshot manager will need to be created on demand as it needs raft actor context which cannot
    // be passed to it in the constructor
    private SnapshotManager snapshotManager;

    private final DataPersistenceProvider persistenceProvider;

    private short payloadVersion;

    private boolean votingMember = true;

    private RaftActorBehavior currentBehavior;

    private int numVotingPeers = -1;

    private Optional<Cluster> cluster;

    private final Consumer<ApplyState> applyStateConsumer;

    private final FileBackedOutputStreamFactory fileBackedOutputStreamFactory;

    private RaftActorLeadershipTransferCohort leadershipTransferCohort;

    public RaftActorContextImpl(final ActorRef actor, final ActorContext context, final String id,
            final @NonNull ElectionTerm termInformation, final long commitIndex, final long lastApplied,
            final @NonNull Map<String, String> peerAddresses,
            final @NonNull ConfigParams configParams, final @NonNull DataPersistenceProvider persistenceProvider,
            final @NonNull Consumer<ApplyState> applyStateConsumer, final @NonNull Logger logger,
            final @NonNull Executor executor) {
        this.actor = actor;
        this.context = context;
        this.id = id;
        this.termInformation = requireNonNull(termInformation);
        this.executor = requireNonNull(executor);
        this.commitIndex = commitIndex;
        this.lastApplied = lastApplied;
        this.configParams = requireNonNull(configParams);
        this.persistenceProvider = requireNonNull(persistenceProvider);
        log = requireNonNull(logger);
        this.applyStateConsumer = requireNonNull(applyStateConsumer);

        fileBackedOutputStreamFactory = new FileBackedOutputStreamFactory(
                configParams.getFileBackedStreamingThreshold(), configParams.getTempFileDirectory());

        for (Map.Entry<String, String> e : requireNonNull(peerAddresses).entrySet()) {
            peerInfoMap.put(e.getKey(), new PeerInfo(e.getKey(), e.getValue(), VotingState.VOTING));
        }
    }

    @VisibleForTesting
    public void setPayloadVersion(final short payloadVersion) {
        this.payloadVersion = payloadVersion;
    }

    @Override
    public short getPayloadVersion() {
        return payloadVersion;
    }

    public void setConfigParams(final ConfigParams configParams) {
        this.configParams = configParams;
    }

    @Override
    public ActorRef actorOf(final Props props) {
        return context.actorOf(props);
    }

    @Override
    public ActorSelection actorSelection(final String path) {
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
    public Optional<Cluster> getCluster() {
        if (cluster == null) {
            try {
                cluster = Optional.of(Cluster.get(getActorSystem()));
            } catch (Exception e) {
                // An exception means there's no cluster configured. This will only happen in unit tests.
                log.debug("{}: Could not obtain Cluster", getId(), e);
                cluster = Optional.empty();
            }
        }

        return cluster;
    }

    @Override
    public ElectionTerm getTermInformation() {
        return termInformation;
    }

    @Override
    public long getCommitIndex() {
        return commitIndex;
    }

    @Override public void setCommitIndex(final long commitIndex) {
        this.commitIndex = commitIndex;
    }

    @Override
    public long getLastApplied() {
        return lastApplied;
    }

    @Override
    public void setLastApplied(final long lastApplied) {
        final Throwable stackTrace = log.isTraceEnabled() ? new Throwable() : null;
        log.debug("{}: Moving last applied index from {} to {}", id, this.lastApplied, lastApplied, stackTrace);
        this.lastApplied = lastApplied;
    }

    @Override
    public void setReplicatedLog(final ReplicatedLog replicatedLog) {
        this.replicatedLog = replicatedLog;
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
    public Logger getLogger() {
        return log;
    }

    @Override
    public Collection<String> getPeerIds() {
        return peerInfoMap.keySet();
    }

    @Override
    public Collection<PeerInfo> getPeers() {
        return peerInfoMap.values();
    }

    @Override
    public PeerInfo getPeerInfo(final String peerId) {
        return peerInfoMap.get(peerId);
    }

    @Override
    public String getPeerAddress(final String peerId) {
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
    public void updatePeerIds(final ServerConfigurationPayload serverConfig) {
        votingMember = true;
        boolean foundSelf = false;
        Set<String> currentPeers = new HashSet<>(getPeerIds());
        for (ServerInfo server : serverConfig.getServerConfig()) {
            if (getId().equals(server.getId())) {
                foundSelf = true;
                if (!server.isVoting()) {
                    votingMember = false;
                }
            } else {
                VotingState votingState = server.isVoting() ? VotingState.VOTING : VotingState.NON_VOTING;
                if (!currentPeers.contains(server.getId())) {
                    addToPeers(server.getId(), null, votingState);
                } else {
                    getPeerInfo(server.getId()).setVotingState(votingState);
                    currentPeers.remove(server.getId());
                }
            }
        }

        for (String peerIdToRemove : currentPeers) {
            removePeer(peerIdToRemove);
        }

        if (!foundSelf) {
            votingMember = false;
        }

        log.debug("{}: Updated server config: isVoting: {}, peers: {}", id, votingMember, peerInfoMap.values());

        setDynamicServerConfigurationInUse();
    }

    @Override public ConfigParams getConfigParams() {
        return configParams;
    }

    @Override
    public void addToPeers(final String peerId, final String address, final VotingState votingState) {
        peerInfoMap.put(peerId, new PeerInfo(peerId, address, votingState));
        numVotingPeers = -1;
    }

    @Override
    public void removePeer(final String name) {
        if (getId().equals(name)) {
            votingMember = false;
        } else {
            peerInfoMap.remove(name);
            numVotingPeers = -1;
        }
    }

    @Override public ActorSelection getPeerActorSelection(final String peerId) {
        String peerAddress = getPeerAddress(peerId);
        if (peerAddress != null) {
            return actorSelection(peerAddress);
        }
        return null;
    }

    @Override
    public void setPeerAddress(final String peerId, final String peerAddress) {
        PeerInfo peerInfo = peerInfoMap.get(peerId);
        if (peerInfo != null) {
            log.info("Peer address for peer {} set to {}", peerId, peerAddress);
            peerInfo.setAddress(peerAddress);
        }
    }

    @Override
    public SnapshotManager getSnapshotManager() {
        if (snapshotManager == null) {
            snapshotManager = new SnapshotManager(this, log);
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
    public ServerConfigurationPayload getPeerServerInfo(final boolean includeSelf) {
        if (!isDynamicServerConfigurationInUse()) {
            return null;
        }
        final var peers = getPeers();
        final var newConfig = ImmutableList.<ServerInfo>builderWithExpectedSize(peers.size() + (includeSelf ? 1 : 0));
        for (PeerInfo peer : peers) {
            newConfig.add(new ServerInfo(peer.getId(), peer.isVoting()));
        }

        if (includeSelf) {
            newConfig.add(new ServerInfo(getId(), votingMember));
        }

        return new ServerConfigurationPayload(newConfig.build());
    }

    @Override
    public boolean isVotingMember() {
        return votingMember;
    }

    @Override
    public boolean anyVotingPeers() {
        if (numVotingPeers < 0) {
            numVotingPeers = 0;
            for (PeerInfo info: getPeers()) {
                if (info.isVoting()) {
                    numVotingPeers++;
                }
            }
        }

        return numVotingPeers > 0;
    }

    @Override
    public RaftActorBehavior getCurrentBehavior() {
        return currentBehavior;
    }

    void setCurrentBehavior(final RaftActorBehavior behavior) {
        currentBehavior = requireNonNull(behavior);
    }

    @Override
    public Consumer<ApplyState> getApplyStateConsumer() {
        return applyStateConsumer;
    }

    @Override
    public FileBackedOutputStreamFactory getFileBackedOutputStreamFactory() {
        return fileBackedOutputStreamFactory;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    void close() {
        if (currentBehavior != null) {
            try {
                currentBehavior.close();
            } catch (Exception e) {
                log.debug("{}: Error closing behavior {}", getId(), currentBehavior.state(), e);
            }
        }
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
