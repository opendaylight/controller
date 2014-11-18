package org.opendaylight.controller.cluster.notifications;

/**
 * Reply message sent from a RoleChangeNotifier to the Role Change Listener
 *
 * Currently these  are sent within the same node, hence are not Serializable
 */
public class RegisterRoleChangeListenerReply {
    private String senderId;

    public RegisterRoleChangeListenerReply(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderId() {
        return senderId;
    }
}
