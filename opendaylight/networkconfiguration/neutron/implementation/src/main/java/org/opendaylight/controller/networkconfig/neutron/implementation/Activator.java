/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.configuration.IConfigurationContainerService;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallPolicyCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronFloatingIPCRUD;
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
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
    .getLogger(Activator.class);

    /**
     * Function called when the activator starts just after some
     * initializations are done by the
     * ComponentActivatorAbstractBase.
     *
     */
    @Override
    public void init() {

    }

    /**
     * Function called when the activator stops just before the
     * cleanup done by ComponentActivatorAbstractBase
     *
     */
    @Override
    public void destroy() {

    }

    /**
     * Function that is used to communicate to dependency manager the
     * list of known implementations for services inside a container
     *
     *
     * @return An array containing all the CLASS objects that will be
     * instantiated in order to get an fully working implementation
     * Object
     */
    @Override
    public Object[] getImplementations() {
        Object[] res = { NeutronFloatingIPInterface.class,
                NeutronRouterInterface.class,
                NeutronPortInterface.class,
                NeutronSubnetInterface.class,
                NeutronNetworkInterface.class,
                NeutronSecurityGroupInterface.class,
                NeutronSecurityRuleInterface.class,
                NeutronFirewallInterface.class,
                NeutronFirewallPolicyInterface.class,
                NeutronFirewallRuleInterface.class,
                NeutronLoadBalancerInterface.class,
                NeutronLoadBalancerPoolInterface.class,
                NeutronLoadBalancerListenerInterface.class,
                NeutronLoadBalancerHealthMonitorInterface.class,
                NeutronLoadBalancerPoolMemberInterface.class};
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies
     * is required.
     *
     * @param c dependency manager Component object, used for
     * configuring the dependencies exported and imported
     * @param imp Implementation class that is being configured,
     * needed as long as the same routine can configure multiple
     * implementations
     * @param containerName The containerName being configured, this allow
     * also optional per-container different behavior if needed, usually
     * should not be the case though.
     */
    @Override
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(NeutronFloatingIPInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronFloatingIPCRUD.class.getName(),
                                   IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronRouterInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronRouterCRUD.class.getName(),
                                   IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronPortInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronPortCRUD.class.getName(),
                                   IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronSubnetInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronSubnetCRUD.class.getName(),
                                   IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronNetworkInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronNetworkCRUD.class.getName(),
                                   IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronSecurityGroupInterface.class)) {
            // export the service
            c.setInterface(
                new String[] { INeutronSecurityGroupCRUD.class.getName(),
                    IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                .setService(IClusterContainerServices.class)
                .setCallbacks("setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                IConfigurationContainerService.class).setCallbacks(
                "setConfigurationContainerService",
                "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronSecurityRuleInterface.class)) {
            // export the service
            c.setInterface(
                new String[] { INeutronSecurityRuleCRUD.class.getName(),
                    IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                .setService(IClusterContainerServices.class)
                .setCallbacks("setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                IConfigurationContainerService.class).setCallbacks(
                "setConfigurationContainerService",
                "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronFirewallInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronFirewallCRUD.class.getName(),
                            IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                            "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronFirewallPolicyInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronFirewallPolicyCRUD.class.getName(),
                            IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                            "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronFirewallRuleInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronFirewallRuleCRUD.class.getName(),
                            IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                            "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronLoadBalancerInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronLoadBalancerCRUD.class.getName(),
                            IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                            "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronLoadBalancerListenerInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronLoadBalancerListenerCRUD.class.getName(),
                            IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                            "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronLoadBalancerPoolInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronLoadBalancerPoolCRUD.class.getName(),
                            IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                            "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronLoadBalancerHealthMonitorInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronLoadBalancerHealthMonitorCRUD.class.getName(),
                            IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                            "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
        if (imp.equals(NeutronLoadBalancerPoolMemberInterface.class)) {
            // export the service
            c.setInterface(
                    new String[] { INeutronLoadBalancerPoolMemberCRUD.class.getName(),
                            IConfigurationContainerAware.class.getName()}, null);
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "neutron");
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                            "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IConfigurationContainerService.class).setCallbacks(
                    "setConfigurationContainerService",
                    "unsetConfigurationContainerService").setRequired(true));
        }
    }
}
