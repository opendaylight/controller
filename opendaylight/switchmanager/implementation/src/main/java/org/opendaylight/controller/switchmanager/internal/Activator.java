
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.internal;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.configuration.IConfigurationContainerAware;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.inventory.IInventoryService;
import org.opendaylight.controller.sal.inventory.IListenInventoryUpdates;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISpanAware;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.ISwitchManagerAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SwitchManager Bundle Activator
 *
 *
 */
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
        Object[] res = { SwitchManager.class };
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
        if (imp.equals(SwitchManager.class)) {
            // export the service
            c.setInterface(new String[] {
                    IListenInventoryUpdates.class.getName(),
                    ISwitchManager.class.getName(),
                    IConfigurationContainerAware.class.getName() }, null);

            // Now lets add a service dependency to make sure the
            // provider of service exists
            c.add(createContainerServiceDependency(containerName).setService(
                    IInventoryService.class).setCallbacks(
                    "setInventoryService", "unsetInventoryService")
                    .setRequired(false));
            c.add(createContainerServiceDependency(containerName).setService(
                    IStatisticsManager.class).setCallbacks(
                    "setStatisticsManager", "unsetStatisticsManager")
                    .setRequired(false));
            c.add(createContainerServiceDependency(containerName).setService(
                    ISwitchManagerAware.class).setCallbacks(
                    "setSwitchManagerAware", "unsetSwitchManagerAware")
                    .setRequired(false));
            c.add(createContainerServiceDependency(containerName).setService(
                    IInventoryListener.class).setCallbacks(
                    "setInventoryListener", "unsetInventoryListener")
                    .setRequired(false));
            c.add(createContainerServiceDependency(containerName).setService(
                    ISpanAware.class).setCallbacks("setSpanAware",
                    "unsetSpanAware").setRequired(false));
            c.add(createContainerServiceDependency(containerName).setService(
                    IClusterContainerServices.class).setCallbacks(
                    "setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));
        }
    }

    @Override
    protected Object[] getGlobalImplementations() {
        final Object[] res = { SwitchManagerCLI.class };
        return res;
    }
}
