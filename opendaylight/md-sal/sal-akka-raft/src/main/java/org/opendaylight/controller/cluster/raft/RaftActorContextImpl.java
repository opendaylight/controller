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
import akka.actor.UntypedActorContext;
import akka.event.LoggingAdapter;

import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

public class RaftActorContextImpl implements RaftActorContext {

    private final ActorRef actor;

    private final UntypedActorContext context;

    private final String id;

    private final ElectionTerm termInformation;

    private long commitIndex;

    private long lastApplied;

    private ReplicatedLog replicatedLog;

    private final Map<String, String> peerAddresses;

    private final LoggingAdapter LOG;

    private final ConfigParams configParams;

    private long replicatedToAllIndex = -1;

    public RaftActorContextImpl(ActorRef actor, UntypedActorContext context,
        String id,
        ElectionTerm termInformation, long commitIndex,
        long lastApplied, ReplicatedLog replicatedLog,
        Map<String, String> peerAddresses, ConfigParams configParams,
        LoggingAdapter logger) {
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

    @Override public LoggingAdapter getLogger() {
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
    public long getReplicatedToAllIndex() {
        return replicatedToAllIndex;
    }

    @Override
    public void setReplicatedToAllIndex(long replicatedToAllIndex) {
        this.replicatedToAllIndex = replicatedToAllIndex;
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
