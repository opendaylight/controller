/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallPolicyCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerHealthMonitorCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerListenerCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronRouterCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {
    protected static final Logger logger = LoggerFactory
    .getLogger(Activator.class);
    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    @Override
    public void start(BundleContext context) throws Exception {
        NeutronRouterInterface neutronRouterInterface = new NeutronRouterInterface();
        ServiceRegistration<INeutronRouterCRUD> neutronRouterInterfaceRegistration = context.registerService(INeutronRouterCRUD.class, neutronRouterInterface, null);
        if(neutronRouterInterfaceRegistration != null) {
            registrations.add(neutronRouterInterfaceRegistration);
        }
        NeutronPortInterface neutronPortInterface = new NeutronPortInterface();
        ServiceRegistration<INeutronPortCRUD> neutronPortInterfaceRegistration = context.registerService(INeutronPortCRUD.class, neutronPortInterface, null);
        if(neutronPortInterfaceRegistration != null) {
            registrations.add(neutronPortInterfaceRegistration);
        }

        NeutronSubnetInterface neutronSubnetInterface = new NeutronSubnetInterface();
        ServiceRegistration<INeutronSubnetCRUD> neutronSubnetInterfaceRegistration = context.registerService(INeutronSubnetCRUD.class, neutronSubnetInterface, null);
        if(neutronSubnetInterfaceRegistration != null) {
            registrations.add(neutronSubnetInterfaceRegistration);
        }

        NeutronNetworkInterface neutronNetworkInterface = new NeutronNetworkInterface();
        ServiceRegistration<INeutronNetworkCRUD> neutronNetworkInterfaceRegistration = context.registerService(INeutronNetworkCRUD.class, neutronNetworkInterface, null);
        if(neutronNetworkInterfaceRegistration != null) {
            registrations.add(neutronNetworkInterfaceRegistration);
        }

        NeutronSecurityGroupInterface neutronSecurityGroupInterface = new NeutronSecurityGroupInterface();
        ServiceRegistration<INeutronSecurityGroupCRUD> neutronSecurityGroupInterfaceRegistration = context.registerService(INeutronSecurityGroupCRUD.class, neutronSecurityGroupInterface, null);
        if(neutronSecurityGroupInterfaceRegistration != null) {
            registrations.add(neutronSecurityGroupInterfaceRegistration);
        }

        NeutronSecurityRuleInterface neutronSecurityRuleInterface = new NeutronSecurityRuleInterface();
        ServiceRegistration<INeutronSecurityRuleCRUD> neutronSecurityRuleInterfaceRegistration = context.registerService(INeutronSecurityRuleCRUD.class, neutronSecurityRuleInterface, null);
        if(neutronSecurityRuleInterfaceRegistration != null) {
            registrations.add(neutronSecurityRuleInterfaceRegistration);
        }

        NeutronFirewallInterface neutronFirewallInterface = new NeutronFirewallInterface();
        ServiceRegistration<INeutronFirewallCRUD> neutronFirewallInterfaceRegistration = context.registerService(INeutronFirewallCRUD.class, neutronFirewallInterface, null);
        if(neutronFirewallInterfaceRegistration != null) {
            registrations.add(neutronFirewallInterfaceRegistration);
        }

        NeutronFirewallPolicyInterface neutronFirewallPolicyInterface = new NeutronFirewallPolicyInterface();
        ServiceRegistration<INeutronFirewallPolicyCRUD> neutronFirewallPolicyInterfaceRegistration = context.registerService(INeutronFirewallPolicyCRUD.class, neutronFirewallPolicyInterface, null);
        if(neutronFirewallPolicyInterfaceRegistration != null) {
            registrations.add(neutronFirewallPolicyInterfaceRegistration);
        }

        NeutronFirewallRuleInterface neutronFirewallRuleInterface = new NeutronFirewallRuleInterface();
        ServiceRegistration<INeutronFirewallRuleCRUD> neutronFirewallRuleInterfaceRegistration = context.registerService(INeutronFirewallRuleCRUD.class, neutronFirewallRuleInterface, null);
        if(neutronFirewallRuleInterfaceRegistration != null) {
            registrations.add(neutronFirewallRuleInterfaceRegistration);
        }

        NeutronLoadBalancerInterface neutronLoadBalancerInterface = new NeutronLoadBalancerInterface();
        ServiceRegistration<INeutronLoadBalancerCRUD> neutronLoadBalancerInterfaceRegistration = context.registerService(INeutronLoadBalancerCRUD.class, neutronLoadBalancerInterface, null);
        if(neutronFirewallInterfaceRegistration != null) {
            registrations.add(neutronLoadBalancerInterfaceRegistration);
        }

        NeutronLoadBalancerPoolInterface neutronLoadBalancerPoolInterface = new NeutronLoadBalancerPoolInterface();
        ServiceRegistration<INeutronLoadBalancerPoolCRUD> neutronLoadBalancerPoolInterfaceRegistration = context.registerService(INeutronLoadBalancerPoolCRUD.class, neutronLoadBalancerPoolInterface, null);
        if(neutronLoadBalancerPoolInterfaceRegistration != null) {
            registrations.add(neutronLoadBalancerPoolInterfaceRegistration);
        }

        NeutronLoadBalancerListenerInterface neutronLoadBalancerListenerInterface = new NeutronLoadBalancerListenerInterface();
        ServiceRegistration<INeutronLoadBalancerListenerCRUD> neutronLoadBalancerListenerInterfaceRegistration = context.registerService(INeutronLoadBalancerListenerCRUD.class, neutronLoadBalancerListenerInterface, null);
        if(neutronLoadBalancerListenerInterfaceRegistration != null) {
            registrations.add(neutronLoadBalancerListenerInterfaceRegistration);
        }

        NeutronLoadBalancerHealthMonitorInterface neutronLoadBalancerHealthMonitorInterface = new NeutronLoadBalancerHealthMonitorInterface();
        ServiceRegistration<INeutronLoadBalancerHealthMonitorCRUD> neutronLoadBalancerHealthMonitorInterfaceRegistration = context.registerService(INeutronLoadBalancerHealthMonitorCRUD.class, neutronLoadBalancerHealthMonitorInterface, null);
        if(neutronLoadBalancerHealthMonitorInterfaceRegistration != null) {
            registrations.add(neutronLoadBalancerHealthMonitorInterfaceRegistration);
        }

        NeutronLoadBalancerPoolMemberInterface neutronLoadBalancerPoolMemberInterface = new NeutronLoadBalancerPoolMemberInterface();
        ServiceRegistration<INeutronLoadBalancerPoolMemberCRUD> neutronLoadBalancerPoolMemberInterfaceRegistration = context.registerService(INeutronLoadBalancerPoolMemberCRUD.class, neutronLoadBalancerPoolMemberInterface, null);
        if(neutronLoadBalancerPoolMemberInterfaceRegistration != null) {
            registrations.add(neutronLoadBalancerPoolMemberInterfaceRegistration);
        }

    }

    @Override
    public void stop(BundleContext context) throws Exception {
       for(ServiceRegistration registration : registrations) {
           if(registration != null) {
               registration.unregister();
           }
       }

    }
}