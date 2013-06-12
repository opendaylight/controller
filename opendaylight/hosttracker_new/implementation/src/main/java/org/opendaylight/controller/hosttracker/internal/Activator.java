/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.hosttracker.IDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.ISwitchManagerAware;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);

    @Override
    protected void init() {

    }

    @Override
    protected void destroy() {

    }

    /**
     * Function that is used to communicate to dependency manager the list of
     * known implementations for services inside a container
     * 
     * 
     * @return An array containing all the CLASS objects that will be
     *         instantiated in order to get an fully working implementation
     *         Object
     */
    @Override
    public Object[] getImplementations() {
        Object[] res = { DeviceManagerImpl.class };
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies is
     * required.
     * 
     * @param c
     *            dependency manager Component object, used for configuring the
     *            dependencies exported and imported
     * @param imp
     *            Implementation class that is being configured, needed as long
     *            as the same routine can configure multiple implementations
     * @param containerName
     *            The containerName being configured, this allow also optional
     *            per-container different behavior if needed, usually should not
     *            be the case though.
     */
    @Override
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(DeviceManagerImpl.class)) {
            // export the service
            // XXX - TODO merge with existing APIs
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "devicemanager");

            c.setInterface(new String[] { IDeviceService.class.getName(),
                    IListenDataPacket.class.getName(),
                    ITopologyManagerAware.class.getName() }, props);

            c.add(createContainerServiceDependency(containerName)
                    .setService(ISwitchManager.class)
                    .setCallbacks("setSwitchManager", "unsetSwitchManager")
                    .setRequired(false));

            c.add(createContainerServiceDependency(containerName)
                    .setService(IDataPacketService.class)
                    .setCallbacks("setDataPacketService",
                            "unsetDataPacketService").setRequired(true));

            // c.add(createContainerServiceDependency(containerName).setService(
            // IClusterContainerServices.class).setCallbacks(
            // "setClusterContainerService",
            // "unsetClusterContainerService").setRequired(true));
            c.add(createContainerServiceDependency(containerName)
                    .setService(ITopologyManager.class)
                    .setCallbacks("setTopologyManager", "unsetTopologyManager")
                    .setRequired(false));

            c.add(createContainerServiceDependency(containerName)
                    .setService(IDataPacketService.class)
                    .setCallbacks("setDataPacketService",
                            "unsetDataPacketService").setRequired(true));
        }
    }

    /**
     * Method which tells how many Global implementations are supported by the
     * bundle. This way we can tune the number of components created. This
     * components will be created ONLY at the time of bundle startup and will be
     * destroyed only at time of bundle destruction, this is the major
     * difference with the implementation retrieved via getImplementations where
     * all of them are assumed to be in a container !
     * 
     * 
     * @return The list of implementations the bundle will support, in Global
     *         version
     */
    @Override
    protected Object[] getGlobalImplementations() {
        return null;
    }

    /**
     * Configure the dependency for a given instance Global
     * 
     * @param c
     *            Component assigned for this instance, this will be what will
     *            be used for configuration
     * @param imp
     *            implementation to be configured
     * @param containerName
     *            container on which the configuration happens
     */
    @Override
    protected void configureGlobalInstance(Component c, Object imp) {

    }

}
