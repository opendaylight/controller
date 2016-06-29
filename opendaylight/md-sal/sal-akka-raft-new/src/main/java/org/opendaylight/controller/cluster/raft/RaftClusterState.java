package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorSelection;

import java.io.Serializable;

public class RaftClusterState implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean isCurrentNodeLeader;
    private boolean isClusterLeaderActive;
    private String leaderId;
    private ActorSelection Leader;

    public RaftClusterState(boolean isCurrentNodeLeader,
                            boolean isClusterLeaderActive,
                            String leaderId, ActorSelection leader) {
        this.isCurrentNodeLeader = isCurrentNodeLeader;
        this.isClusterLeaderActive = isClusterLeaderActive;
        this.leaderId = leaderId;
        Leader = leader;
    }

    public boolean isCurrentNodeLeader() {
        return isCurrentNodeLeader;
    }

    public boolean isClusterLeaderActive() {
        return isClusterLeaderActive;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public ActorSelection getLeader() {
        return Leader;
    }
}
