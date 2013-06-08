package org.opendaylight.controller.northbound.commons.utils;

import org.opendaylight.controller.containermanager.IContainerAuthorization;
import org.opendaylight.controller.sal.authorization.Privilege;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.IUserManager;

public class NorthboundUtils {

    
    /**
     * Returns whether the current user has the required privilege on the
     * specified container
     * 
     * @param userName
     *            The user name
     * @param containerName
     *            The container name
     * @param required
     *            Operation to be performed - READ/WRITE
     * @param bundle
     *            Class from where the function is invoked           
     * @return The Status of the request, either Success or Unauthorized
     */
    public static boolean isAuthorized(String userName, String containerName,
            Privilege required,Object bundle) {
        
         if (containerName.equals(GlobalConstants.DEFAULT.toString())) {
            IUserManager auth = (IUserManager) ServiceHelper.getGlobalInstance(
                    IUserManager.class, bundle);
            
            switch (required) {
            case WRITE:
                return (auth.getUserLevel(userName).ordinal() <= UserLevel.NETWORKADMIN.ordinal());
            case READ:
                return (auth.getUserLevel(userName).ordinal() <= UserLevel.NETWORKOPERATOR.ordinal());                    
            default:
                return false;
            }

        } else {
            IContainerAuthorization auth = (IContainerAuthorization) ServiceHelper
                    .getGlobalInstance(IContainerAuthorization.class, bundle);

            if (auth == null) {
                return false;
            }

            Privilege current = auth.getResourcePrivilege(userName,
                    containerName);
            if (required.ordinal() > current.ordinal()) {
                return false;
            }
        }
        return true;
    }
    
}
