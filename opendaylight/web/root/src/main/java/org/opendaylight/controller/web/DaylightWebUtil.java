package org.opendaylight.controller.web;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.opendaylight.controller.containermanager.IContainerAuthorization;
import org.opendaylight.controller.sal.authorization.Resource;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;

public class DaylightWebUtil {
    private static String defaultName = GlobalConstants.DEFAULT.toString();
    
    /**
     * Returns the container that this user is authorized to access. If the user is not authorized to the requested
     * container, then this method will return the default container.
     * 
     * @param request - HttpServletRequest object to retrieve username
     * @param container - requested container
     * @param bundle - respective bundle
     * @return container name if cleared, else it will always be 'default'
     */
    public static String getAuthorizedContainer(HttpServletRequest request, String container, Object bundle) {
        if (container == null) {
            return defaultName;
        }
        
        String username = request.getUserPrincipal().getName();
        IContainerAuthorization containerAuthorization = (IContainerAuthorization)
                ServiceHelper.getGlobalInstance(IContainerAuthorization.class, bundle);
        if (containerAuthorization != null) {
            Set<Resource> resources = containerAuthorization.getAllResourcesforUser(username);
            for(Resource resource : resources) {
                String name = (String) resource.getResource();
                if(container.equals(name)) {
                    return name;
                }
            }
        }
        return defaultName;
    }
}