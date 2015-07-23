/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import org.opendaylight.controller.cluster.raft.behaviors.Candidate;
import org.opendaylight.controller.cluster.raft.behaviors.Follower;
import org.opendaylight.controller.cluster.raft.behaviors.IsolatedLeader;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;

public enum RaftState {
    Candidate {
        @Override
        public RaftActorBehavior createBehavior(RaftActorContext context) {
            return new Candidate(context);
        }
    },
    Follower {
        @Override
        public RaftActorBehavior createBehavior(RaftActorContext context) {
            return new Follower(context);
        }
    },
    Leader {
        @Override
        public RaftActorBehavior createBehavior(RaftActorContext context) {
            return new Leader(context);
        }
    },
    IsolatedLeader {
        @Override
        public RaftActorBehavior createBehavior(RaftActorContext context) {
            return new IsolatedLeader(context);
        }
    };

    public abstract RaftActorBehavior createBehavior(RaftActorContext context);
}
