/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongSupplier;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
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

    public RaftActorContextImpl(ActorRef actor, ActorContext context, String id,
            ElectionTerm termInformation, long commitIndex, long lastApplied, Map<String, String> peerAddresses,
            ConfigParams configParams, DataPersistenceProvider persistenceProvider, Logger logger) {
        this.actor = actor;
        this.context = context;
        this.id = id;
        this.termInformation = termInformation;
        this.commitIndex = commitIndex;
        this.lastApplied = lastApplied;
        this.configParams = configParams;
        this.persistenceProvider = persistenceProvider;
        this.log = logger;

        for(Map.Entry<String, String> e: peerAddresses.entrySet()) {
            peerInfoMap.put(e.getKey(), new PeerInfo(e.getKey(), e.getValue(), VotingState.VOTING));
        }
    }

    @VisibleForTesting
    public void setPayloadVersion(short payloadVersion) {
        this.payloadVersion = payloadVersion;
    }

    @Override
    public short getPayloadVersion() {
        return payloadVersion;
    }

    public void setConfigParams(ConfigParams configParams) {
        this.configParams = configParams;
    }

    @Override
    public ActorRef actorOf(Props props){
        return context.actorOf(props);
    }

    @Override
    public ActorSelection actorSelection(String path){
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
    public Optional<Cluster> getCluster() {
        if(cluster == null) {
            try {
                cluster = Optional.of(Cluster.get(getActorSystem()));
            } catch(Exception e) {
                // An exception means there's no cluster configured. This will only happen in unit tests.
                log.debug("{}: Could not obtain Cluster: {}", getId(), e);
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

    @Override public void setCommitIndex(long commitIndex) {
        this.commitIndex = commitIndex;
    }

    @Override
    public long getLastApplied() {
        return lastApplied;
    }

    @Override
    public void setLastApplied(long lastApplied) {
        this.lastApplied = lastApplied;
    }

    @Override
    public void setReplicatedLog(ReplicatedLog replicatedLog) {
        this.replicatedLog = replicatedLog;
    }

    @Override
    public ReplicatedLog getReplicatedLog() {
        return replicatedLog;
    }

    @Override public ActorSystem getActorSystem() {
        return context.system();
    }

    @Override public Logger getLogger() {
        return this.log;
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
    public PeerInfo getPeerInfo(String peerId) {
        return peerInfoMap.get(peerId);
    }

    @Override
    public String getPeerAddress(String peerId) {
        String peerAddress;
        PeerInfo peerInfo = peerInfoMap.get(peerId);
        if(peerInfo != null) {
            peerAddress = peerInfo.getAddress();
            if(peerAddress == null) {
                peerAddress = configParams.getPeerAddressResolver().resolve(peerId);
                peerInfo.setAddress(peerAddress);
            }
        } else {
            peerAddress = configParams.getPeerAddressResolver().resolve(peerId);
        }

        return peerAddress;
    }

    @Override
    public void updatePeerIds(ServerConfigurationPayload serverConfig){
        votingMember = true;
        boolean foundSelf = false;
        Set<String> currentPeers = new HashSet<>(this.getPeerIds());
        for(ServerInfo server: serverConfig.getServerConfig()) {
            if(getId().equals(server.getId())) {
                foundSelf = true;
                if(!server.isVoting()) {
                    votingMember = false;
                }
            } else {
                VotingState votingState = server.isVoting() ? VotingState.VOTING: VotingState.NON_VOTING;
                if(!currentPeers.contains(server.getId())) {
                    this.addToPeers(server.getId(), null, votingState);
                } else {
                    this.getPeerInfo(server.getId()).setVotingState(votingState);
                    currentPeers.remove(server.getId());
                }
            }
        }

        for(String peerIdToRemove: currentPeers) {
            this.removePeer(peerIdToRemove);
        }

        if(!foundSelf) {
            votingMember = false;
        }

        log.debug("{}: Updated server config: isVoting: {}, peers: {}", id, votingMember, peerInfoMap.values());

        setDynamicServerConfigurationInUse();
    }

    @Override public ConfigParams getConfigParams() {
        return configParams;
    }

    @Override
    public void addToPeers(String peerId, String address, VotingState votingState) {
        peerInfoMap.put(peerId, new PeerInfo(peerId, address, votingState));
        numVotingPeers = -1;
    }

    @Override
    public void removePeer(String name) {
        if(getId().equals(name)) {
            votingMember = false;
        } else {
            peerInfoMap.remove(name);
            numVotingPeers = -1;
        }
    }

    @Override public ActorSelection getPeerActorSelection(String peerId) {
        String peerAddress = getPeerAddress(peerId);
        if(peerAddress != null){
            return actorSelection(peerAddress);
        }
        return null;
    }

    @Override
    public void setPeerAddress(String peerId, String peerAddress) {
        PeerInfo peerInfo = peerInfoMap.get(peerId);
        if(peerInfo != null) {
            log.info("Peer address for peer {} set to {}", peerId, peerAddress);
            peerInfo.setAddress(peerAddress);
        }
    }

    @Override
    public SnapshotManager getSnapshotManager() {
        if(snapshotManager == null){
            snapshotManager = new SnapshotManager(this, log);
        }
        return snapshotManager;
    }

    @Override
    public long getTotalMemory() {
        return totalMemoryRetriever.getAsLong();
    }

    @Override
    public void setTotalMemoryRetriever(LongSupplier retriever) {
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
        this.dynamicServerConfiguration = true;
    }

    @Override
    public ServerConfigurationPayload getPeerServerInfo(boolean includeSelf) {
        if (!isDynamicServerConfigurationInUse()) {
            return null;
        }
        Collection<PeerInfo> peers = getPeers();
        List<ServerInfo> newConfig = new ArrayList<>(peers.size() + 1);
        for(PeerInfo peer: peers) {
            newConfig.add(new ServerInfo(peer.getId(), peer.isVoting()));
        }

        if(includeSelf) {
            newConfig.add(new ServerInfo(getId(), votingMember));
        }

        return new ServerConfigurationPayload(newConfig);
    }

    @Override
    public boolean isVotingMember() {
        return votingMember;
    }

    @Override
    public boolean anyVotingPeers() {
        if(numVotingPeers < 0) {
            numVotingPeers = 0;
            for(PeerInfo info: getPeers()) {
                if(info.isVoting()) {
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
        this.currentBehavior = Preconditions.checkNotNull(behavior);
    }

    void close() {
        if (currentBehavior != null) {
            try {
                currentBehavior.close();
            } catch (Exception e) {
                log.debug("{}: Error closing behavior {}", getId(), currentBehavior.state(), e);
            }
        }
    }
}
