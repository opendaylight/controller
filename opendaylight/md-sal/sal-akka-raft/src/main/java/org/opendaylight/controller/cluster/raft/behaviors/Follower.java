/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;

/**
 * The behavior of a RaftActor in the Follower state
 */
public class Follower extends AbstractRaftActorBehavior {
    public Follower(RaftActorContext context) {
        super(context);
    }

    @Override public RaftState handleMessage(ActorRef sender, Object message) {
        return RaftState.Follower;
    }
}
