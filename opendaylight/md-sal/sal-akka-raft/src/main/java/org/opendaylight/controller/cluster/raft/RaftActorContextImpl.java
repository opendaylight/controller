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

public class RaftActorContextImpl implements RaftActorContext{

    private final ActorRef actor;

    private final UntypedActorContext context;

    private final String id;

    private final ElectionTerm termInformation;

    private long commitIndex;

    private long lastApplied;

    private final ReplicatedLog replicatedLog;

    private final Map<String, String> peerAddresses;

    private final LoggingAdapter LOG;


    public RaftActorContextImpl(ActorRef actor, UntypedActorContext context,
        String id,
        ElectionTerm termInformation, long commitIndex,
        long lastApplied, ReplicatedLog replicatedLog, Map<String, String> peerAddresses, LoggingAdapter logger) {
        this.actor = actor;
        this.context = context;
        this.id = id;
        this.termInformation = termInformation;
        this.commitIndex = commitIndex;
        this.lastApplied = lastApplied;
        this.replicatedLog = replicatedLog;
        this.peerAddresses = peerAddresses;
        this.LOG = logger;
    }

    public ActorRef actorOf(Props props){
        return context.actorOf(props);
    }

    public ActorSelection actorSelection(String path){
        return context.actorSelection(path);
    }

    public String getId() {
        return id;
    }

    public ActorRef getActor() {
        return actor;
    }

    public ElectionTerm getTermInformation() {
        return termInformation;
    }

    public long getCommitIndex() {
        return commitIndex;
    }

    @Override public void setCommitIndex(long commitIndex) {
        this.commitIndex = commitIndex;
    }

    public long getLastApplied() {
        return lastApplied;
    }

    @Override public void setLastApplied(long lastApplied) {
        this.lastApplied = lastApplied;
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
}
