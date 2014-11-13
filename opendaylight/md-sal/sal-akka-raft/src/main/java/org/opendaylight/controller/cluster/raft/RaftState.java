package org.opendaylight.controller.cluster.raft;

public enum RaftState {
    Candidate,
    Follower,
    Leader,
    IsolatedLeader;
}
