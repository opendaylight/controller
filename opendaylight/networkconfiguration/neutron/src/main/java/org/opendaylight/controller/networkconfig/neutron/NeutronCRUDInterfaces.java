/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeutronCRUDInterfaces {
    private static final Logger logger = LoggerFactory
            .getLogger(NeutronCRUDInterfaces.class);

    public static INeutronNetworkCRUD getINeutronNetworkCRUD(Object o) {
        INeutronNetworkCRUD answer = (INeutronNetworkCRUD) getInstances(INeutronNetworkCRUD.class, o);
        return answer;
    }

    public static INeutronSubnetCRUD getINeutronSubnetCRUD(Object o) {
        INeutronSubnetCRUD answer = (INeutronSubnetCRUD) getInstances(INeutronSubnetCRUD.class, o);
        return answer;
    }

    public static INeutronPortCRUD getINeutronPortCRUD(Object o) {
        INeutronPortCRUD answer = (INeutronPortCRUD) getInstances(INeutronPortCRUD.class, o);
        return answer;
    }

    public static INeutronRouterCRUD getINeutronRouterCRUD(Object o) {
        INeutronRouterCRUD answer = (INeutronRouterCRUD) getInstances(INeutronRouterCRUD.class, o);
        return answer;
    }

    public static INeutronFloatingIPCRUD getINeutronFloatingIPCRUD(Object o) {
        INeutronFloatingIPCRUD answer = (INeutronFloatingIPCRUD) getInstances(INeutronFloatingIPCRUD.class, o);
        return answer;
    }

    public static INeutronSecurityGroupCRUD getINeutronSecurityGroupCRUD(Object o) {
        INeutronSecurityGroupCRUD answer = (INeutronSecurityGroupCRUD) getInstances(INeutronSecurityGroupCRUD.class, o);
        return answer;
    }

    public static INeutronSecurityRuleCRUD getINeutronSecurityRuleCRUD(Object o) {
        INeutronSecurityRuleCRUD answer = (INeutronSecurityRuleCRUD) getInstances(INeutronSecurityRuleCRUD.class, o);
        return answer;
    }

    public static INeutronFirewallCRUD getINeutronFirewallCRUD(Object o) {
        INeutronFirewallCRUD answer = (INeutronFirewallCRUD) getInstances(INeutronFirewallCRUD.class, o);
        return answer;
    }

    public static INeutronFirewallPolicyCRUD getINeutronFirewallPolicyCRUD(Object o) {
        INeutronFirewallPolicyCRUD answer = (INeutronFirewallPolicyCRUD) getInstances(INeutronFirewallPolicyCRUD.class, o);
        return answer;
    }

    public static INeutronFirewallRuleCRUD getINeutronFirewallRuleCRUD(Object o) {
        INeutronFirewallRuleCRUD answer = (INeutronFirewallRuleCRUD) getInstances(INeutronFirewallRuleCRUD.class, o);
        return answer;
    }

    public static INeutronLoadBalancerCRUD getINeutronLoadBalancerCRUD(Object o) {
        INeutronLoadBalancerCRUD answer = (INeutronLoadBalancerCRUD) getInstances(INeutronLoadBalancerCRUD.class, o);
        return answer;
    }

    public static INeutronLoadBalancerPoolCRUD getINeutronLoadBalancerPoolCRUD(Object o) {
        INeutronLoadBalancerPoolCRUD answer = (INeutronLoadBalancerPoolCRUD) getInstances(INeutronLoadBalancerPoolCRUD.class, o);
        return answer;
    }

    public static INeutronLoadBalancerListenerCRUD getINeutronLoadBalancerListenerCRUD(Object o) {
        INeutronLoadBalancerListenerCRUD answer = (INeutronLoadBalancerListenerCRUD) getInstances(INeutronLoadBalancerListenerCRUD.class, o);
        return answer;
    }

    public static INeutronLoadBalancerHealthMonitorCRUD getINeutronLoadBalancerHealthMonitorCRUD(Object o) {
        INeutronLoadBalancerHealthMonitorCRUD answer = (INeutronLoadBalancerHealthMonitorCRUD) getInstances(INeutronLoadBalancerHealthMonitorCRUD.class, o);
        return answer;
    }

    public static INeutronLoadBalancerPoolMemberCRUD getINeutronLoadBalancerPoolMemberCRUD(Object o) {
        INeutronLoadBalancerPoolMemberCRUD answer = (INeutronLoadBalancerPoolMemberCRUD) getInstances(INeutronLoadBalancerPoolMemberCRUD.class, o);
        return answer;
    }

    public static Object getInstances(Class<?> clazz,Object bundle) {
        try {
            BundleContext bCtx = FrameworkUtil.getBundle(bundle.getClass())
                    .getBundleContext();

            ServiceReference<?>[] services = null;
                services = bCtx.getServiceReferences(clazz.getName(),
                        null);
            if (services != null) {
                return bCtx.getService(services[0]);
            }
        } catch (Exception e) {
            logger.error("Instance reference is NULL");
        }
        return null;
    }
}