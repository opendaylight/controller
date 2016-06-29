package org.opendaylight.controller.cluster.raft.client.messages;

import org.opendaylight.controller.cluster.raft.RaftClusterState;

import java.io.Serializable;

public class PersistDataReply implements Serializable {
    private final boolean canPersist;
    private final RaftClusterState state;

    public PersistDataReply(boolean canPersist, RaftClusterState state) {
        this.canPersist = canPersist;
        this.state = state;
    }

    public boolean canPersist() {
        return canPersist;
    }

    public RaftClusterState getState() {
        return state;
    }
}
