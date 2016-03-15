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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.ServerConfigurationPayload.ServerInfo;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.opendaylight.controller.cluster.raft.policy.RaftPolicy;
import org.slf4j.Logger;

public class RaftActorContextImpl implements RaftActorContext {

    private final ActorRef actor;

    private final ActorContext context;

    private final String id;

    private final ElectionTerm termInformation;

    private long commitIndex;

    private long lastApplied;

    private ReplicatedLog replicatedLog;

    private final Map<String, PeerInfo> peerInfoMap = new HashMap<>();

    private final Logger LOG;

    private ConfigParams configParams;

    private boolean dynamicServerConfiguration = false;

    @VisibleForTesting
    private Supplier<Long> totalMemoryRetriever;

    // Snapshot manager will need to be created on demand as it needs raft actor context which cannot
    // be passed to it in the constructor
    private SnapshotManager snapshotManager;

    private final DataPersistenceProvider persistenceProvider;

    private short payloadVersion;

    private boolean votingMember = true;

    private RaftActorBehavior currentBehavior;

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
        this.LOG = logger;

        for(Map.Entry<String, String> e: peerAddresses.entrySet()) {
            peerInfoMap.put(e.getKey(), new PeerInfo(e.getKey(), e.getValue(), VotingState.VOTING));
        }
    }

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
        return this.LOG;
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
        String peerAddress = null;
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

        setDynamicServerConfigurationInUse();
    }

    @Override public ConfigParams getConfigParams() {
        return configParams;
    }

    @Override
    public void addToPeers(String id, String address, VotingState votingState) {
        peerInfoMap.put(id, new PeerInfo(id, address, votingState));
    }

    @Override
    public void removePeer(String name) {
        if(getId().equals(name)) {
            votingMember = false;
        } else {
            peerInfoMap.remove(name);
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
            LOG.info("Peer address for peer {} set to {}", peerId, peerAddress);
            peerInfo.setAddress(peerAddress);
        }
    }

    @Override
    public SnapshotManager getSnapshotManager() {
        if(snapshotManager == null){
            snapshotManager = new SnapshotManager(this, LOG);
        }
        return snapshotManager;
    }

    @Override
    public long getTotalMemory() {
        return totalMemoryRetriever != null ? totalMemoryRetriever.get() : Runtime.getRuntime().totalMemory();
    }

    @Override
    public void setTotalMemoryRetriever(Supplier<Long> retriever) {
        totalMemoryRetriever = retriever;
    }

    @Override
    public boolean hasFollowers() {
        return getPeerIds().size() > 0;
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

        return (new ServerConfigurationPayload(newConfig));
    }

    @Override
    public boolean isVotingMember() {
        return votingMember;
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
                LOG.debug("{}: Error closing behavior {}", getId(), currentBehavior.state());
            }
        }
    }
}
