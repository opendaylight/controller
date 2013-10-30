
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.routing.dijkstra_implementation.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.routing.IListenRoutingUpdates;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerClusterWideAware;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;

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
        Object[] res = { DijkstraImplementation.class };
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
    public void configureInstance(final Component c, final Object imp, final String containerName) {
        if (imp.equals(DijkstraImplementation.class)) {
            // export the service
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("topoListenerName", "routing.Dijkstra");

            c.setInterface(new String[] { ITopologyManagerClusterWideAware.class.getName(), IRouting.class.getName() },
                    props);

            // Now lets add a service dependency to make sure the
            // provider of service exists
            c.add(createContainerServiceDependency(containerName).setService(IListenRoutingUpdates.class)
                    .setCallbacks("setListenRoutingUpdates", "unsetListenRoutingUpdates")
                    .setRequired(false));

            c.add(createContainerServiceDependency(containerName).setService(ISwitchManager.class)
                    .setCallbacks("setSwitchManager", "unsetSwitchManager")
                    .setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(ITopologyManager.class)
                    .setCallbacks("setTopologyManager", "unsetTopologyManager")
                    .setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(IClusterContainerServices.class)
                    .setCallbacks("setClusterContainerService", "unsetClusterContainerService")
                    .setRequired(true));
        }
    }

    @Override
    protected Object[] getGlobalImplementations() {
        final Object[] res = { DijkstraImplementationCLI.class };
        return res;
    }
}
