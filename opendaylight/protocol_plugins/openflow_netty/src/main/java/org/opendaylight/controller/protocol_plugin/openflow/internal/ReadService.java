
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.Dictionary;
import java.util.List;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.protocol_plugin.openflow.IPluginReadServiceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IPluginInReadService;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;

/**
 * Container Instance of IPluginInReadService implementation class
 *
 *
 *
 */
public class ReadService implements IPluginInReadService {
    private static final Logger logger = LoggerFactory
            .getLogger(ReadService.class);
    private IPluginReadServiceFilter filter;
    private String containerName;

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    @SuppressWarnings("unchecked")
    void init(Component c) {
        Dictionary<Object, Object> props = c.getServiceProperties();
        containerName = (props != null) ? (String) props.get("containerName")
                : null;
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
    }

    public void setService(IPluginReadServiceFilter filter) {
        this.filter = filter;
    }

    public void unsetService(IPluginReadServiceFilter filter) {
        this.filter = null;
    }

    @Override
    public FlowOnNode readFlow(Node node, Flow flow, boolean cached) {
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            logger.error("Invalid node type");
            return null;
        }

        return filter.readFlow(containerName, node, flow, cached);
    }

    @Override
    public List<FlowOnNode> readAllFlow(Node node, boolean cached) {
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            logger.error("Invalid node type");
            return null;
        }

        return filter.readAllFlow(containerName, node, cached);
    }

    @Override
    public NodeDescription readDescription(Node node, boolean cached) {
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            logger.error("Invalid node type");
            return null;
        }

        return filter.readDescription(node, cached);
    }

    @Override
    public NodeConnectorStatistics readNodeConnector(NodeConnector connector,
            boolean cached) {
        if (!connector.getNode().getType()
            .equals(NodeIDType.OPENFLOW)) {
            logger.error("Invalid node type");
            return null;
        }
        return filter.readNodeConnector(containerName, connector, cached);
    }

    @Override
    public List<NodeConnectorStatistics> readAllNodeConnector(Node node,
            boolean cached) {
        if (!node.getType().equals(NodeIDType.OPENFLOW)) {
            logger.error("Invalid node type");
            return null;
        }

        return filter.readAllNodeConnector(containerName, node, cached);
    }

    @Override
    public long getTransmitRate(NodeConnector connector) {
        if (!connector.getNode().getType()
            .equals(NodeIDType.OPENFLOW)) {
            logger.error("Invalid node type");
            return 0;
        }
        return filter.getTransmitRate(containerName, connector);
    }
}
