/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import org.opendaylight.controller.sal.utils.ServiceHelper;

public class NeutronCRUDInterfaces {

    public static INeutronNetworkCRUD getINeutronNetworkCRUD(Object o) {
        INeutronNetworkCRUD answer = (INeutronNetworkCRUD) ServiceHelper.getGlobalInstance(INeutronNetworkCRUD.class, o);
        return answer;
    }

    public static INeutronSubnetCRUD getINeutronSubnetCRUD(Object o) {
        INeutronSubnetCRUD answer = (INeutronSubnetCRUD) ServiceHelper.getGlobalInstance(INeutronSubnetCRUD.class, o);
        return answer;
    }

    public static INeutronPortCRUD getINeutronPortCRUD(Object o) {
        INeutronPortCRUD answer = (INeutronPortCRUD) ServiceHelper.getGlobalInstance(INeutronPortCRUD.class, o);
        return answer;
    }

    public static INeutronRouterCRUD getINeutronRouterCRUD(Object o) {
        INeutronRouterCRUD answer = (INeutronRouterCRUD) ServiceHelper.getGlobalInstance(INeutronRouterCRUD.class, o);
        return answer;
    }

    public static INeutronFloatingIPCRUD getINeutronFloatingIPCRUD(Object o) {
        INeutronFloatingIPCRUD answer = (INeutronFloatingIPCRUD) ServiceHelper.getGlobalInstance(INeutronFloatingIPCRUD.class, o);
        return answer;
    }
}
