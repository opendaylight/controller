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
import akka.actor.Props;
import akka.actor.UntypedActorContext;

import java.util.concurrent.atomic.AtomicLong;

public class RaftActorContextImpl implements RaftActorContext{

    private final ActorRef actor;

    private final UntypedActorContext context;

    private final String id;

    private final ElectionTerm termInformation;

    private final AtomicLong commitIndex;

    private final AtomicLong lastApplied;

    private final ReplicatedLog replicatedLog;

    public RaftActorContextImpl(ActorRef actor, UntypedActorContext context,
        String id,
        ElectionTerm termInformation, AtomicLong commitIndex,
        AtomicLong lastApplied, ReplicatedLog replicatedLog) {
        this.actor = actor;
        this.context = context;
        this.id = id;
        this.termInformation = termInformation;
        this.commitIndex = commitIndex;
        this.lastApplied = lastApplied;
        this.replicatedLog = replicatedLog;
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

    public AtomicLong getCommitIndex() {
        return commitIndex;
    }

    public AtomicLong getLastApplied() {
        return lastApplied;
    }

    @Override public ReplicatedLog getReplicatedLog() {
        return replicatedLog;
    }
}
