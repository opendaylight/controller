/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.yangtools.concepts.Identifier;

public class PersistenceComplete {

    private final ActorRef clientActor;
    private final Identifier identifier;
    private final ReplicatedLogEntry persistedLogEntry;

    public PersistenceComplete(final ActorRef clientActor, final Identifier identifier,
                               final ReplicatedLogEntry persistedLogEntry) {
        this.clientActor = clientActor;
        this.identifier = identifier;
        this.persistedLogEntry = persistedLogEntry;
    }

    public ReplicatedLogEntry getPersistedLogEntry() {
        return persistedLogEntry;
    }

    public ActorRef getClientActor() {
        return clientActor;
    }

    public Identifier getIdentifier() {
        return identifier;
    }
}
