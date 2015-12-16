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
 *
 * @author Thomas Pantelis
 */
public abstract class RaftActorLeadershipTransferCohort {
    private final RaftActor raftActor;

    protected RaftActorLeadershipTransferCohort(RaftActor raftActor) {
        this.raftActor = raftActor;
    }

    public void startTransfer() {
        RaftActorBehavior behavior = raftActor.getCurrentBehavior();
        if(behavior instanceof Leader) {
            ((Leader)behavior).transferLeadership(this);
        }
    }

    public void abortTransfer() {
        transferComplete();
    }

    public abstract void transferComplete();
}
