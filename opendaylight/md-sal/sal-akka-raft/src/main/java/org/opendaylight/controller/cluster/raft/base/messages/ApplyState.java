/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

import java.io.Serializable;

public class ApplyState implements Serializable {
    private final ActorRef clientActor;
    private final String identifier;
    private final ReplicatedLogEntry replicatedLogEntry;

    public ApplyState(ActorRef clientActor, String identifier,
        ReplicatedLogEntry replicatedLogEntry) {
        this.clientActor = clientActor;
        this.identifier = identifier;
        this.replicatedLogEntry = replicatedLogEntry;
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
}
