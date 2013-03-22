
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.implementation.internal;

import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.flowprogrammer.IPluginInFlowProgrammerService;
import org.opendaylight.controller.sal.inventory.IInventoryService;
import org.opendaylight.controller.sal.inventory.IListenInventoryUpdates;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IPluginInDataPacketService;
import org.opendaylight.controller.sal.packet.IPluginOutDataPacketService;
import org.opendaylight.controller.sal.reader.IPluginInReadService;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.sal.topology.IListenTopoUpdates;
import org.opendaylight.controller.sal.topology.IPluginInTopologyService;
import org.opendaylight.controller.sal.topology.IPluginOutTopologyService;
import org.opendaylight.controller.sal.topology.ITopologyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.dm.Component;

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
        Object[] res = { Topology.class, Inventory.class,
                FlowProgrammerService.class, ReadService.class,
                DataPacketService.class };
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
        if (imp.equals(Topology.class)) {
            // export the service for Apps and Plugins
            c.setInterface(new String[] {
                    IPluginOutTopologyService.class.getName(),
                    ITopologyService.class.getName() }, null);

            // There can be multiple Topology listeners or there could
            // be none, hence the dependency is optional
            c.add(createContainerServiceDependency(containerName).setService(
                    IListenTopoUpdates.class).setCallbacks("setUpdateService",
                    "unsetUpdateService").setRequired(false));

            // There can be multiple southbound plugins or there could
            // be none, the dependency is optional
            c.add(createContainerServiceDependency(containerName).setService(
                    IPluginInTopologyService.class).setCallbacks(
                    "setPluginService", "unsetPluginService")
                    .setRequired(false));
        }

        if (imp.equals(Inventory.class)) {
            // export the service
            c.setInterface(new String[] {
                    IPluginOutInventoryService.class.getName(),
                    IInventoryService.class.getName() }, null);

            // Now lets add a service dependency to make sure the
            // provider of service exists
            c.add(createContainerServiceDependency(containerName).setService(
                    IListenInventoryUpdates.class).setCallbacks(
                    "setUpdateService", "unsetUpdateService")
                    .setRequired(false));
            c
                    .add(createContainerServiceDependency(containerName)
                            .setService(IPluginInInventoryService.class)
                            .setCallbacks("setPluginService",
                                    "unsetPluginService").setRequired(true));
        }

        if (imp.equals(FlowProgrammerService.class)) {
            // It is the provider of IFlowProgrammerService
            c.setInterface(IFlowProgrammerService.class.getName(), null);
            //It is also the consumer of IPluginInFlowProgrammerService
            c.add(createServiceDependency().setService(
                    IPluginInFlowProgrammerService.class).setCallbacks(
                    "setService", "unsetService").setRequired(true));
        }

        if (imp.equals(ReadService.class)) {
            // It is the provider of IReadService
            c.setInterface(IReadService.class.getName(), null);

            //It is also the consumer of IPluginInReadService
            c.add(createContainerServiceDependency(containerName).setService(
                    IPluginInReadService.class).setCallbacks("setService",
                    "unsetService").setRequired(true));
        }

        /************************/
        /* DATA PACKET SERVICES */
        /************************/
        if (imp.equals(DataPacketService.class)) {
            c.setInterface(new String[] {
                    IPluginOutDataPacketService.class.getName(),
                    IDataPacketService.class.getName() }, null);

            // Optionally use PluginInDataService if any southbound
            // protocol plugin exists
            c.add(createContainerServiceDependency(containerName).setService(
                    IPluginInDataPacketService.class).setCallbacks(
                    "setPluginInDataService", "unsetPluginInDataService")
                    .setRequired(false));

            // Optionally listed to IListenDataPacket services
            c.add(createContainerServiceDependency(containerName).setService(
                    IListenDataPacket.class).setCallbacks(
                    "setListenDataPacket", "unsetListenDataPacket")
                    .setRequired(false));
        }
    }
}
