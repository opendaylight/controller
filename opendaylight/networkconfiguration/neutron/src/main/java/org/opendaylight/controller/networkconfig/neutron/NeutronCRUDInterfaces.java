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
        return getCrudService(INeutronNetworkCRUD.class, o);
    }

    public static INeutronSubnetCRUD getINeutronSubnetCRUD(Object o) {
        return getCrudService(INeutronSubnetCRUD.class, o);
    }

    public static INeutronPortCRUD getINeutronPortCRUD(Object o) {
        return getCrudService(INeutronPortCRUD.class, o);
    }

    public static INeutronRouterCRUD getINeutronRouterCRUD(Object o) {
        return getCrudService(INeutronRouterCRUD.class, o);
    }

    public static INeutronFloatingIPCRUD getINeutronFloatingIPCRUD(Object o) {
        return getCrudService(INeutronFloatingIPCRUD.class, o);
    }

    public static INeutronSecurityGroupCRUD getNeutronSecurityGroupCRUD(Object o) {
        return getCrudService(INeutronSecurityGroupCRUD.class, o);
    }

    public static INeutronSecurityGroupRuleCRUD getNeutronSecurityGroupRuleCRUD(Object o) {
        return getCrudService(INeutronSecurityGroupRuleCRUD.class, o);
    }

    private static <S extends INeutronCRUD<?>> S getCrudService(Class<S> klass, Object o) {
        return klass.cast(ServiceHelper.getGlobalInstance(klass, o));
    }
}
