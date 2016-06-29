package org.opendaylight.controller.cluster.raft.client.messages;

import org.opendaylight.controller.cluster.raft.RaftClusterState;

import java.io.Serializable;

public class RaftClusterStateChanged implements Serializable {
    private static final long serialVersionUID = 1L;
    private final RaftClusterState clusterState;

    public RaftClusterStateChanged(RaftClusterState clusterState) {
        this.clusterState = clusterState;
    }

    public RaftClusterState getClusterState() {
        return clusterState;
    }
}
