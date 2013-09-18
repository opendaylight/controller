package org.opendaylight.controller.web;

import org.opendaylight.controller.containermanager.IContainerAuthorization;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.Name;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.usermanager.IUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaylightWebUtil {

    private static final String AUDIT = "audit";
    private static final Logger logger = LoggerFactory.getLogger(AUDIT);

    /**
     * Returns the access privilege the user has on the specified container
     *
     * @param userName
     *            The user name
     * @param container
     *            The container name. If null, the default container will be assumed
     * @param bundle
     *            The bundle originating the request
     * @return The access privilege the user is granted on the container
     */
    public static Privilege getContainerPrivilege(String userName,
            String container, Object bundle) {
        // Derive the target resource
        String resource = (container == null) ? GlobalConstants.DEFAULT.toString() : container;

        // Retrieve the Container Authorization service
        IContainerAuthorization auth = (IContainerAuthorization) ServiceHelper
                .getGlobalInstance(IContainerAuthorization.class, bundle);
        if (auth != null) {
            return auth.getResourcePrivilege(userName, resource);
        }

        /*
         * Container Authorization service not available. We can only derive the
         * access privilege to the default container based on user level
         */
        if (resource.equals(GlobalConstants.DEFAULT.toString())) {
            IUserManager userManager = (IUserManager) ServiceHelper
                    .getGlobalInstance(IUserManager.class, bundle);
            if (userManager != null) {
                switch (userManager.getUserLevel(userName)) {
                case NETWORKADMIN:
                    return Privilege.WRITE;
                case NETWORKOPERATOR:
                    return Privilege.READ;
                default:
                    return Privilege.NONE;
                }
            }
        }

        return Privilege.NONE;
    }

    public static void auditlog(String moduleName, String user, String action, String resource,
            String containerName) {
        String auditMsg = "";
        String mode = "UI";
        if (containerName != null) {
            auditMsg = "Mode: " + mode + " User " + user + " "  + action + " " + moduleName + " " + resource + " in container "
                    + containerName;
        } else {
            auditMsg = "Mode: " + mode + " User " + user + " "  + action + " " + moduleName + " " + resource;
        }
        logger.info(auditMsg);
    }

    public static void auditlog(String moduleName, String user, String action, String resource) {
        auditlog(moduleName, user, action, resource, null);
    }

    public static String getNodeDesc(Node node, ISwitchManager switchManager) {
        Description desc = (Description) switchManager.getNodeProp(node,
                Description.propertyName);
        String description = (desc == null) ? "" : desc.getValue();
        return (description.isEmpty() || description.equalsIgnoreCase("none")) ? node
                .toString() : description;
    }

    public static String getNodeDesc(Node node, String containerName,
            Object bundle) {
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, containerName, bundle);
        if (switchManager == null) {
            return null;
        }

        return getNodeDesc(node, switchManager);
    }

    public static String getNodeDesc(Node node, Object bundle) {
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class,
                        GlobalConstants.DEFAULT.toString(), bundle);
        if (switchManager == null) {
            return null;
        }

        return getNodeDesc(node, switchManager);
    }

    public static String getPortName(NodeConnector nodeConnector,
            String container, Object bundle) {
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
                .getInstance(ISwitchManager.class, container, bundle);
        return getPortName(nodeConnector, switchManager);
    }

    public static String getPortName(NodeConnector nodeConnector, Object bundle) {
        return getPortName(nodeConnector, GlobalConstants.DEFAULT.toString(), bundle);
    }

    public static String getPortName(NodeConnector nodeConnector,
            ISwitchManager switchManager) {
        Name ncName = ((Name) switchManager.getNodeConnectorProp(nodeConnector,
                Name.NamePropName));
        String nodeConnectorName = (ncName != null) ? ncName.getValue() : nodeConnector.getNodeConnectorIdAsString();
        nodeConnectorName = nodeConnectorName + "@"
                + getNodeDesc(nodeConnector.getNode(), switchManager);
        return nodeConnectorName.substring(0, nodeConnectorName.length());
    }
}