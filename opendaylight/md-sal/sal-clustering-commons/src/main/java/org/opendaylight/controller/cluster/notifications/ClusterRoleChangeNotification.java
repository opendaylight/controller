package org.opendaylight.controller.cluster.notifications;

/**
 * Notification message representing a Role change of a cluster member
 *
 * Roles generally are Leader, Follower and Candidate. But can be based on the consensus strategy/implementation
 *
 * Currently these notifications are sent within the same node, hence are not Serializable
 */
public class ClusterRoleChangeNotification {
    private String memberId;
    private String oldRole;
    private String newRole;

    public ClusterRoleChangeNotification(String memberId, String oldRole, String newRole) {
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
