package org.opendaylight.controller.cluster.raft.notifications;

import org.opendaylight.controller.cluster.raft.RaftState;

/**
 * Role Change message initiated from the Raft Actor when a the behavior/role changes.
 *
 */
public class RaftRoleChanged {
    private String memberId;
    private RaftState oldRole;
    private RaftState newRole;

    public RaftRoleChanged(String memberId, RaftState oldRole, RaftState newRole) {
        this.memberId = memberId;
        this.oldRole = oldRole;
        this.newRole = newRole;
    }

    public String getMemberId() {
        return memberId;
    }

    public RaftState getOldRole() {
        return oldRole;
    }

    public RaftState getNewRole() {
        return newRole;
    }
}
