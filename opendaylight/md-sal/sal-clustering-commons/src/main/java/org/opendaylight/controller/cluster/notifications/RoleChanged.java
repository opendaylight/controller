package org.opendaylight.controller.cluster.notifications;

/**
 * Role Change message initiated internally from the  Raft Actor when a the behavior/role changes.
 *
 * Since its internal , need not be serialized
 *
 */
public class RoleChanged {
    private String memberId;
    private String oldRole;
    private String newRole;

    public RoleChanged(String memberId, String oldRole, String newRole) {
        this.memberId = memberId;
        this.oldRole = oldRole;
        this.newRole = newRole;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getOldRole() {
        return oldRole;
    }

    public String getNewRole() {
        return newRole;
    }
}
