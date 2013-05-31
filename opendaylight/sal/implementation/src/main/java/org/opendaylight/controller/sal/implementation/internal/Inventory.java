
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.implementation.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private List<IListenInventoryUpdates> updateService = new CopyOnWriteArrayList<IListenInventoryUpdates>();
    private List<IPluginInInventoryService> pluginService = new CopyOnWriteArrayList<IPluginInInventoryService>();

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
        this.pluginService.add(service);
    }

    public void unsetPluginService(IPluginInInventoryService service) {
        logger.trace("Got plugin service UNset request");
        this.pluginService.remove(service);
    }

    public void setUpdateService(IListenInventoryUpdates service) {
        logger.trace("Got update service set request {}", service);
        this.updateService.add(service);
    }

    public void unsetUpdateService(IListenInventoryUpdates service) {
        logger.trace("Got a service UNset request");
        this.updateService.remove(service);
    }

    @Override
    public void updateNode(Node node, UpdateType type, Set<Property> props) {
        if (type == null) {
            logger.trace("Input type is null");
            return;
        }

        logger.trace("{} {}", node, type);

        for (IListenInventoryUpdates s : this.updateService) {
            s.updateNode(node, type, props);
        }
    }

    @Override
    public void updateNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Set<Property> props) {
        if (type == null) {
            logger.trace("Input type is null");
            return;
        }

        logger.trace("{} {}", nodeConnector, type);

        for (IListenInventoryUpdates s : this.updateService) {
            s.updateNodeConnector(nodeConnector, type, props);
        }
    }

    @Override
    public ConcurrentMap<Node, Map<String, Property>> getNodeProps() {
        ConcurrentMap<Node, Map<String, Property>> nodeProps =
            new ConcurrentHashMap<Node, Map<String, Property>>(), rv;

        for (IPluginInInventoryService s : this.pluginService) {
            rv = s.getNodeProps();
            if (rv != null) {
                nodeProps.putAll(rv);
            }
        }

        return nodeProps;
    }

    @Override
    public ConcurrentMap<NodeConnector, Map<String, Property>> getNodeConnectorProps() {
        ConcurrentMap<NodeConnector, Map<String, Property>> ncProps =
            new ConcurrentHashMap<NodeConnector, Map<String, Property>>(), rv;

        for (IPluginInInventoryService s : this.pluginService) {
            rv = s.getNodeConnectorProps(true);
            if (rv != null) {
                ncProps.putAll(rv);
            }
        }

        return ncProps;
    }
}
