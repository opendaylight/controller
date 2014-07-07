/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import org.opendaylight.controller.cluster.raft.RaftActorContext;

/**
 * Abstract class that represents the behavior of a RaftActor
 * <p>
 * All Servers:
 * <ul>
 * <li> If commitIndex > lastApplied: increment lastApplied, apply
 * log[lastApplied] to state machine (§5.3)
 * <li> If RPC request or response contains term T > currentTerm:
 * set currentTerm = T, convert to follower (§5.1)
 */
public abstract class AbstractRaftActorBehavior implements RaftActorBehavior {

    /**
     * Information about the RaftActor whose behavior this class represents
     */
    protected final RaftActorContext context;


    protected AbstractRaftActorBehavior(RaftActorContext context) {
        this.context = context;
    }
}
