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
