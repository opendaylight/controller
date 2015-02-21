/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import akka.actor.ActorRef;
import java.io.Serializable;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

public class ApplyState implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ActorRef clientActor;
    private final String identifier;
    private final ReplicatedLogEntry replicatedLogEntry;
    private final long startTime;

    public ApplyState(ActorRef clientActor, String identifier,
        ReplicatedLogEntry replicatedLogEntry) {
        this.clientActor = clientActor;
        this.identifier = identifier;
        this.replicatedLogEntry = replicatedLogEntry;
        this.startTime = System.nanoTime();
    }

    public ActorRef getClientActor() {
        return clientActor;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ReplicatedLogEntry getReplicatedLogEntry() {
        return replicatedLogEntry;
    }

    public long getStartTime() {
        return startTime;
    }

    @Override
    public String toString() {
        return "ApplyState{" +
                "identifier='" + identifier + '\'' +
                ", replicatedLogEntry.index =" + replicatedLogEntry.getIndex() +
                ", startTime=" + startTime +
                '}';
    }
}
