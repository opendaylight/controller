
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.implementation.internal;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IInventoryService;
import org.opendaylight.controller.sal.inventory.IListenInventoryUpdates;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class describes SAL service to bridge inventory protocol plugin and upper
 * applications. One instance per container of the network.
 */
public class Inventory implements IPluginOutInventoryService, IInventoryService {
    protected static final Logger logger = LoggerFactory
            .getLogger(Inventory.class);
    private IListenInventoryUpdates updateService = null;
    private IPluginInInventoryService pluginService = null;

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        logger.trace("INIT called!");
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        logger.trace("DESTROY called!");
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        logger.trace("START called!");

        if (pluginService == null) {
            logger.debug("plugin service not avaiable");
            return;
        }
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        logger.trace("STOP called!");
    }

    public void setPluginService(IPluginInInventoryService service) {
        logger.trace("Got plugin service set request {}", service);
        this.pluginService = service;
    }

    public void unsetPluginService(IPluginInInventoryService service) {
        logger.trace("Got plugin service UNset request");
        this.pluginService = null;
    }

    public void setUpdateService(IListenInventoryUpdates service) {
        logger.trace("Got update service set request {}", service);
        this.updateService = service;
    }

    public void unsetUpdateService(IListenInventoryUpdates service) {
        logger.trace("Got a service UNset request");
        this.updateService = null;
    }

    @Override
    public void updateNode(Node node, UpdateType type, Set<Property> props) {
        logger.trace("{} {}", node, type);
        if (updateService != null) {
            updateService.updateNode(node, type, props);
        }
    }

    @Override
    public void updateNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Set<Property> props) {
        logger.trace("{} {}", nodeConnector, type);

        if ((updateService != null) && (type != null)) {
            updateService.updateNodeConnector(nodeConnector, type, props);
        }
    }

    @Override
    public ConcurrentMap<Node, Map<String, Property>> getNodeProps() {
        if (pluginService != null)
            return pluginService.getNodeProps();
        else
            return null;
    }

    @Override
    public ConcurrentMap<NodeConnector, Map<String, Property>> getNodeConnectorProps() {
        if (pluginService != null)
            return pluginService.getNodeConnectorProps(true);
        else
            return null;
    }
}
