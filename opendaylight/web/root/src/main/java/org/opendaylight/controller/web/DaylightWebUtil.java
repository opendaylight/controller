package org.opendaylight.controller.web;

import org.opendaylight.controller.containermanager.IContainerAuthorization;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.IUserManager;

public class DaylightWebUtil {

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
}