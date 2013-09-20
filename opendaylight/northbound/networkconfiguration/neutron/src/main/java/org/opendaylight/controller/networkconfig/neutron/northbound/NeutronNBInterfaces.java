/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import java.util.List;

import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.networkconfig.neutron.INeutronFloatingIPCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronRouterCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronCRUDInterfaces;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.sal.utils.ServiceHelper;

public class NeutronNBInterfaces {

    // return a class that implements the IfNBNetworkCRUD interface
    static INeutronNetworkCRUD getIfNBNetworkCRUD(String containerName, Object o) {
 /*       IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, o);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
            }
        }

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " "
                    + RestMessages.NOCONTAINER.toString());
        }

        IfNBNetworkCRUD answer = (IfNBNetworkCRUD) ServiceHelper.getInstance(
                IfNBNetworkCRUD.class, containerName, o); */

        INeutronNetworkCRUD answer = NeutronCRUDInterfaces.getINeutronNetworkCRUD(o);

        if (answer == null) {
            throw new ServiceUnavailableException("Network CRUD Service "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return answer;
    }

    // return a class that implements the IfNBSubnetCRUD interface
    static INeutronSubnetCRUD getIfNBSubnetCRUD(String containerName, Object o) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, o);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
            }
        }

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " "
                    + RestMessages.NOCONTAINER.toString());
        }

        INeutronSubnetCRUD answer = (INeutronSubnetCRUD) ServiceHelper.getInstance(
                INeutronSubnetCRUD.class, containerName, o);

        if (answer == null) {
            throw new ServiceUnavailableException("Network CRUD Service "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return answer;
    }

    // return a class that implements the IfNBPortCRUD interface
    static INeutronPortCRUD getIfNBPortCRUD(String containerName, Object o) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, o);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
            }
        }

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " "
                    + RestMessages.NOCONTAINER.toString());
        }

        INeutronPortCRUD answer = (INeutronPortCRUD) ServiceHelper.getInstance(
                INeutronPortCRUD.class, containerName, o);

        if (answer == null) {
            throw new ServiceUnavailableException("Network CRUD Service "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return answer;
    }

    // return a class that implements the IfNBRouterCRUD interface
    static INeutronRouterCRUD getIfNBRouterCRUD(String containerName, Object o) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, o);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
            }
        }

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " "
                    + RestMessages.NOCONTAINER.toString());
        }

        INeutronRouterCRUD answer = (INeutronRouterCRUD) ServiceHelper.getInstance(
                INeutronRouterCRUD.class, containerName, o);

        if (answer == null) {
            throw new ServiceUnavailableException("Network CRUD Service "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return answer;
    }

    // return a class that implements the IfNBFloatingIPCRUD interface
    static INeutronFloatingIPCRUD getIfNBFloatingIPCRUD(String containerName, Object o) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, o);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
            }
        }

        if (found == false) {
            throw new ResourceNotFoundException(containerName + " "
                    + RestMessages.NOCONTAINER.toString());
        }

        INeutronFloatingIPCRUD answer = (INeutronFloatingIPCRUD) ServiceHelper.getInstance(
                INeutronFloatingIPCRUD.class, containerName, o);

        if (answer == null) {
            throw new ServiceUnavailableException("Network CRUD Service "
                    + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        return answer;
    }
}
