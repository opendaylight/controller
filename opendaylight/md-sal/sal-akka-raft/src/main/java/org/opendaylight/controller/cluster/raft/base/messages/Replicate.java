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
import org.opendaylight.yangtools.concepts.Identifier;

public class Replicate {
    private final ActorRef clientActor;
    private final Identifier identifier;
    private final ReplicatedLogEntry replicatedLogEntry;
    private final boolean sendImmediate;

    public Replicate(ActorRef clientActor, Identifier identifier, ReplicatedLogEntry replicatedLogEntry,
            boolean sendImmediate) {
        this.clientActor = clientActor;
        this.identifier = identifier;
        this.replicatedLogEntry = replicatedLogEntry;
        this.sendImmediate = sendImmediate;
    }

    public ActorRef getClientActor() {
        return clientActor;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public ReplicatedLogEntry getReplicatedLogEntry() {
        return replicatedLogEntry;
    }

    public boolean isSendImmediate() {
        return sendImmediate;
    }
}
