/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import org.opendaylight.yangtools.concepts.Identifier;

public class ClientRequestTrackerImpl implements ClientRequestTracker {

    private final ActorRef clientActor;
    private final Identifier identifier;
    private final long logIndex;

    public ClientRequestTrackerImpl(ActorRef clientActor, Identifier identifier, long logIndex) {

        this.clientActor = clientActor;

        this.identifier = identifier;

        this.logIndex = logIndex;
    }

    @Override
    public ActorRef getClientActor() {
        return clientActor;
    }

    @Override
    public long getIndex() {
        return logIndex;
    }

    @Override
    public Identifier getIdentifier() {
        return identifier;
    }
}
