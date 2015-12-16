/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;

/**
 * A helper class that participates in raft actor leadership transfer. An instance is created upon
 * initialization of leadership transfer.
 * <p>
 * NOTE: All methods on this class must be called on the actor's thread dispatcher as they modify internal state.
 *
 * @author Thomas Pantelis
 */
public abstract class RaftActorLeadershipTransferCohort {
    private final RaftActor raftActor;

    protected RaftActorLeadershipTransferCohort(RaftActor raftActor) {
        this.raftActor = raftActor;
    }

    /**
     * This method is invoked to start leadership transfer.
     */
    public void startTransfer() {
        RaftActorBehavior behavior = raftActor.getCurrentBehavior();
        if(behavior instanceof Leader) {
            ((Leader)behavior).transferLeadership(this);
        }
    }

    /**
     * This method is invoked to abort leadership transfer.
     */
    public void abortTransfer() {
        transferComplete();
    }

    /**
     * This method is invoked when leadership transfer is complete.
     */
    public abstract void transferComplete();
}
