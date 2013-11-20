
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topologymanager.internal;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.ICacheUpdateAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.topology.IListenTopoUpdates;
import org.opendaylight.controller.sal.topology.ITopologyService;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;
import org.opendaylight.controller.topologymanager.ITopologyManagerClusterWideAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);


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
        Object[] res = { TopologyManagerImpl.class };
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
        if (imp.equals(TopologyManagerImpl.class)) {
            // export the service needed to listen to topology updates
            Dictionary<String, Set<String>> props = new Hashtable<String, Set<String>>();
            Set<String> propSet = new HashSet<String>();
            propSet.add(TopologyManagerImpl.TOPOEDGESDB);
            props.put("cachenames", propSet);

            c.setInterface(new String[] { IListenTopoUpdates.class.getName(),
                    ITopologyManager.class.getName(),
                    IConfigurationContainerAware.class.getName(),
                    ICacheUpdateAware.class.getName() }, props);

            c.add(createContainerServiceDependency(containerName).setService(
                    ITopologyService.class).setCallbacks("setTopoService",
                    "unsetTopoService").setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                    ISwitchManager.class).setCallbacks("setSwitchManager",
                    "unsetSwitchManager").setRequired(true));

            // These are all the listeners for a topology manager
            // updates, there could be many or none
            c.add(createContainerServiceDependency(containerName).setService(
                    ITopologyManagerAware.class).setCallbacks(
                    "setTopologyManagerAware", "unsetTopologyManagerAware")
                    .setRequired(false));

            // These are all the listeners for a topology manager for the
            // cluster wide events
            // updates, there could be many or none
            c.add(createContainerServiceDependency(containerName).setService(ITopologyManagerClusterWideAware.class)
                    .setCallbacks("setTopologyManagerClusterWideAware", "unsetTopologyManagerClusterWideAware")
                    .setRequired(false));

            c.add(createContainerServiceDependency(containerName).setService(
                    IClusterContainerServices.class).setCallbacks(
                    "setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
        }
    }
}
