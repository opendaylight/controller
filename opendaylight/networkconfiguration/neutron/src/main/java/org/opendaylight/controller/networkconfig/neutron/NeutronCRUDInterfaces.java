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

    public static INeutronSecurityGroupCRUD getINeutronSecurityGroupCRUD(Object o) {
        INeutronSecurityGroupCRUD answer = (INeutronSecurityGroupCRUD) ServiceHelper.getGlobalInstance(INeutronSecurityGroupCRUD.class, o);
        return answer;
    }

    public static INeutronSecurityRuleCRUD getINeutronSecurityRuleCRUD(Object o) {
        INeutronSecurityRuleCRUD answer = (INeutronSecurityRuleCRUD) ServiceHelper.getGlobalInstance(INeutronSecurityRuleCRUD.class, o);
        return answer;
    }

    public static INeutronFirewallCRUD getINeutronFirewallCRUD(Object o) {
        INeutronFirewallCRUD answer = (INeutronFirewallCRUD) ServiceHelper.getGlobalInstance(INeutronFirewallCRUD.class, o);
        return answer;
    }

    public static INeutronFirewallPolicyCRUD getINeutronFirewallPolicyCRUD(Object o) {
        INeutronFirewallPolicyCRUD answer = (INeutronFirewallPolicyCRUD) ServiceHelper.getGlobalInstance(INeutronFirewallPolicyCRUD.class, o);
        return answer;
    }

    public static INeutronFirewallRuleCRUD getINeutronFirewallRuleCRUD(Object o) {
        INeutronFirewallRuleCRUD answer = (INeutronFirewallRuleCRUD) ServiceHelper.getGlobalInstance(INeutronFirewallRuleCRUD.class, o);
        return answer;
    }

    public static INeutronLoadBalancerCRUD getINeutronLoadBalancerCRUD(Object o) {
        INeutronLoadBalancerCRUD answer = (INeutronLoadBalancerCRUD) ServiceHelper.getGlobalInstance(INeutronLoadBalancerCRUD.class, o);
        return answer;
    }

    public static INeutronLoadBalancerPoolCRUD getINeutronLoadBalancerPoolCRUD(Object o) {
        INeutronLoadBalancerPoolCRUD answer = (INeutronLoadBalancerPoolCRUD) ServiceHelper.getGlobalInstance(INeutronLoadBalancerPoolCRUD.class, o);
        return answer;
    }

    public static INeutronLoadBalancerListenerCRUD getINeutronLoadBalancerListenerCRUD(Object o) {
        INeutronLoadBalancerListenerCRUD answer = (INeutronLoadBalancerListenerCRUD) ServiceHelper.getGlobalInstance(INeutronLoadBalancerListenerCRUD.class, o);
        return answer;
    }

    public static INeutronLoadBalancerHealthMonitorCRUD getINeutronLoadBalancerHealthMonitorCRUD(Object o) {
        INeutronLoadBalancerHealthMonitorCRUD answer = (INeutronLoadBalancerHealthMonitorCRUD) ServiceHelper.getGlobalInstance(INeutronLoadBalancerHealthMonitorCRUD.class, o);
        return answer;
    }

    public static INeutronLoadBalancerPoolMemberCRUD getINeutronLoadBalancerPoolMemberCRUD(Object o) {
        INeutronLoadBalancerPoolMemberCRUD answer = (INeutronLoadBalancerPoolMemberCRUD) ServiceHelper.getGlobalInstance(INeutronLoadBalancerPoolMemberCRUD.class, o);
        return answer;
    }
}