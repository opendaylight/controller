/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import static com.google.common.base.Preconditions.checkState;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorContext;
import java.util.Map;
import org.slf4j.Logger;

public class RaftActorContextImpl implements RaftActorContext {

    private final ActorRef actor;

    private final UntypedActorContext context;

    private final String id;

    private final ElectionTerm termInformation;

    private long commitIndex;

    private long lastApplied;

    private ReplicatedLog replicatedLog;

    private final Map<String, String> peerAddresses;

    private final Logger LOG;

    private final ConfigParams configParams;

    private boolean snapshotCaptureInitiated;

    public RaftActorContextImpl(ActorRef actor, UntypedActorContext context,
        String id,
        ElectionTerm termInformation, long commitIndex,
        long lastApplied, ReplicatedLog replicatedLog,
        Map<String, String> peerAddresses, ConfigParams configParams,
        Logger logger) {
        this.actor = actor;
        this.context = context;
        this.id = id;
        this.termInformation = termInformation;
        this.commitIndex = commitIndex;
        this.lastApplied = lastApplied;
        this.replicatedLog = replicatedLog;
        this.peerAddresses = peerAddresses;
        this.configParams = configParams;
        this.LOG = logger;
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

    @Override public void setLastApplied(long lastApplied) {
        this.lastApplied = lastApplied;
    }

    @Override public void setReplicatedLog(ReplicatedLog replicatedLog) {
        this.replicatedLog = replicatedLog;
    }

    @Override public ReplicatedLog getReplicatedLog() {
        return replicatedLog;
    }

    @Override public ActorSystem getActorSystem() {
        return context.system();
    }

    @Override public Logger getLogger() {
        return this.LOG;
    }

    @Override public Map<String, String> getPeerAddresses() {
        return peerAddresses;
    }

    @Override public String getPeerAddress(String peerId) {
        return peerAddresses.get(peerId);
    }

    @Override public ConfigParams getConfigParams() {
        return configParams;
    }

    @Override
    public void setSnapshotCaptureInitiated(boolean snapshotCaptureInitiated) {
        this.snapshotCaptureInitiated = snapshotCaptureInitiated;
    }

    @Override
    public boolean isSnapshotCaptureInitiated() {
        return snapshotCaptureInitiated;
    }

    @Override public void addToPeers(String name, String address) {
        peerAddresses.put(name, address);
    }

    @Override public void removePeer(String name) {
        peerAddresses.remove(name);
    }

    @Override public ActorSelection getPeerActorSelection(String peerId) {
        String peerAddress = getPeerAddress(peerId);
        if(peerAddress != null){
            return actorSelection(peerAddress);
        }
        return null;
    }

    @Override public void setPeerAddress(String peerId, String peerAddress) {
        LOG.info("Peer address for peer {} set to {}", peerId, peerAddress);
        checkState(peerAddresses.containsKey(peerId), peerId + " is unknown");

        peerAddresses.put(peerId, peerAddress);
    }
}
