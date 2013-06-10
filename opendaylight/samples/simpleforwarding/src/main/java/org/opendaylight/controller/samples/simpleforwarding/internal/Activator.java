
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.samples.simpleforwarding.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.routing.IListenRoutingUpdates;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.controller.samples.simpleforwarding.IBroadcastHandler;
import org.opendaylight.controller.samples.simpleforwarding.IBroadcastPortSelector;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;

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
    public Object[] getImplementations() {
        Object[] res = { SimpleForwardingImpl.class,
                         SimpleBroadcastHandlerImpl.class };
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
        if (imp.equals(SimpleForwardingImpl.class)) {
            // export the service
            c.setInterface(new String[] { IInventoryListener.class.getName(),
                    IfNewHostNotify.class.getName(),
                    IListenRoutingUpdates.class.getName() }, null);

            c.add(createContainerServiceDependency(containerName).setService(
                    IClusterContainerServices.class).setCallbacks(
                    "setClusterContainerService",
                    "unsetClusterContainerService").setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                    ISwitchManager.class).setCallbacks("setSwitchManager",
                    "unsetSwitchManager").setRequired(false));

            c.add(createContainerServiceDependency(containerName).setService(
                    IfIptoHost.class).setCallbacks("setHostTracker",
                    "unsetHostTracker").setRequired(false));

            c.add(createContainerServiceDependency(containerName).setService(
                    IForwardingRulesManager.class).setCallbacks(
                    "setForwardingRulesManager", "unsetForwardingRulesManager")
                    .setRequired(false));

            c.add(createContainerServiceDependency(containerName).setService(
                    ITopologyManager.class).setCallbacks("setTopologyManager",
                    "unsetTopologyManager").setRequired(false));

            c.add(createContainerServiceDependency(containerName).setService(
                    IRouting.class).setCallbacks("setRouting", "unsetRouting")
                    .setRequired(false));
        }else if (imp.equals(SimpleBroadcastHandlerImpl.class)) {
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "simplebroadcasthandler");

            // export the service
            c.setInterface(new String[] { IBroadcastHandler.class.getName(),
                    IListenDataPacket.class.getName() }, props);

            c.add(createContainerServiceDependency(containerName).setService(
                    IDataPacketService.class).setCallbacks("setDataPacketService",
                   "unsetDataPacketService").setRequired(false));

            c.add(createContainerServiceDependency(containerName).setService(
                   ITopologyManager.class).setCallbacks("setTopologyManager",
                   "unsetTopologyManager").setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                   IBroadcastPortSelector.class).setCallbacks("setBroadcastPortSelector",
                   "unsetBroadcastPortSelector").setRequired(false));

            c.add(createContainerServiceDependency(containerName).setService(
                   ISwitchManager.class).setCallbacks("setSwitchManager",
                   "unsetSwitchManager").setRequired(false));
        }
    }
}
