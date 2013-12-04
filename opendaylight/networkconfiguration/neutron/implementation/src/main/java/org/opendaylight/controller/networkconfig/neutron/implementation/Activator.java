/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Dictionary;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.networkconfig.neutron.INeutronFloatingIPCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronRouterCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
    .getLogger(Activator.class);

    /**
     * Function called when the activator starts just after some
     * initializations are done by the
     * ComponentActivatorAbstractBase.
     *
     */
    public void init() {

    }

    /**
     * Function called when the activator stops just before the
     * cleanup done by ComponentActivatorAbstractBase
     *
     */
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
    public Object[] getImplementations() {
        return new Object[]{ NeutronFloatingIPInterface.class,
                NeutronRouterInterface.class,
                NeutronPortInterface.class,
                NeutronSubnetInterface.class,
                NeutronNetworkInterface.class,
                NeutronSecurityGroupInterface.class,
                NeutronSecurityGroupRuleInterface.class};
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
    public void configureInstance(Component c, Object imp, String containerName) {
        if (ifToCRUD.containsKey(imp)) {
            // export the service
            Dictionary<String, String> props = new Hashtable<>();
            props.put("salListenerName", "neutron");
            c.setInterface(new String[] { ifToCRUD.get(imp).getName() }, props);
            c.add(createContainerServiceDependency(containerName)
                    .setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService",
                            "unsetClusterContainerService").setRequired(true));
        }
    }

    private static HashMap<Class<?>, Class<?>> ifToCRUD = new HashMap<Class<?>, Class<?>>() {{
        put(NeutronFloatingIPInterface.class, INeutronFloatingIPCRUD.class);
        put(NeutronRouterInterface.class, INeutronRouterCRUD.class);
        put(NeutronPortInterface.class, INeutronPortCRUD.class);
        put(NeutronSubnetInterface.class, INeutronSubnetCRUD.class);
        put(NeutronNetworkInterface.class, INeutronNetworkCRUD.class);
        put(NeutronSecurityGroupInterface.class, INeutronSecurityGroupCRUD.class);
        put(NeutronSecurityGroupRuleInterface.class, INeutronSecurityGroupRuleCRUD.class);
    }};
}
