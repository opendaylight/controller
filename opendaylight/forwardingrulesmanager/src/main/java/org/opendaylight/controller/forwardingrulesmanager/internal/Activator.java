
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager.internal;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManagerAware;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.core.IContainerListener;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.ISwitchManagerAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.hosttracker.IfIptoHost;

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
        Object[] res = { ForwardingRulesManagerImpl.class };
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
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(ForwardingRulesManagerImpl.class)) {
            String interfaces[] = null;
            Dictionary<String, Set<String>> props = new Hashtable<String, Set<String>>();
            Set<String> propSet = new HashSet<String>();
            propSet.add("staticFlows");
            props.put("cachenames", propSet);

            // export the service
            if (containerName.equals(GlobalConstants.DEFAULT.toString())) {
                interfaces = new String[] { IContainerListener.class.getName(),
                        ISwitchManagerAware.class.getName(),
                        IForwardingRulesManager.class.getName(),
                        IInventoryListener.class.getName(),
                        ICacheUpdateAware.class.getName(),
                        IConfigurationContainerAware.class.getName() };
            } else {
                interfaces = new String[] {
                        ISwitchManagerAware.class.getName(),
                        IForwardingRulesManager.class.getName(),
                        IInventoryListener.class.getName(),
                        ICacheUpdateAware.class.getName(),
                        IConfigurationContainerAware.class.getName() };
            }

            c.setInterface(interfaces, props);

            c.add(createContainerServiceDependency(containerName).setService(
                    IFlowProgrammerService.class).setCallbacks(
                    "setFlowProgrammerService", "unsetFlowProgrammerService")
                    .setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                    IClusterContainerServices.class).setCallbacks(
                    "setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    ISwitchManager.class).setCallbacks("setSwitchManager",
                    "unsetSwitchManager").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IForwardingRulesManagerAware.class).setCallbacks(
                    "setFrmAware", "unsetFrmAware").setRequired(false));
            c.add(createContainerServiceDependency(containerName).setService(
                    IfIptoHost.class).setCallbacks("setHostFinder",
                    "unsetHostFinder").setRequired(true));
            c.add(createContainerServiceDependency(containerName).setService(
                    IContainer.class).setCallbacks("setIContainer",
                    "unsetIContainer").setRequired(true));
        }
    }
}
