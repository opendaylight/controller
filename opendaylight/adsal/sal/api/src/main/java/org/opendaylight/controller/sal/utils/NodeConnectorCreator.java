
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class provides helper functions to create a node connector
 *
 *
 */
@Deprecated
public abstract class NodeConnectorCreator {
    protected static final Logger logger = LoggerFactory
    .getLogger(NodeConnectorCreator.class);
    /**
     * Generic NodeConnector creator
     * The nodeConnector type is inferred from the node type
     *
     * @param portId
     * @param node
     * @return
     */
    public static NodeConnector createNodeConnector(Object portId, Node node) {
        if (node.getType().equals(NodeIDType.OPENFLOW)) {
            try {
                return new NodeConnector(NodeConnectorIDType.OPENFLOW,
                        portId, node);
            } catch (ConstructionException e1) {
                logger.error("",e1);
                return null;
            }
        }
        return null;
    }

    /**
     * Generic NodeConnector creator
     * The nodeConnector type is inferred from the node type
     *
     * @param portId The string representing the port id
     * @param node The network node as {@link org.opendaylight.controller.sal.core.Node Node} object
     * @return The corresponding {@link org.opendaylight.controller.sal.core.NodeConnector NodeConnector} object
     */
    public static NodeConnector createNodeConnector(String portId, Node node) {
        return NodeConnector.fromString(String.format("%s|%s@%s", node.getType(), portId, node.toString()));
    }

    /**
     * NodeConnector creator where NodeConnector type can be specified
     * Needed to create special internal node connectors (like software stack)
     *
     * @param nodeConnectorType
     * @param portId
     * @param node
     * @return
     */
    public static NodeConnector createNodeConnector(
            String nodeConnectorType, Object portId, Node node) {
        try {
            if (nodeConnectorType.equals(Node.NodeIDType.OPENFLOW) && (portId.getClass() == String.class)) {
                return new NodeConnector(nodeConnectorType, Short.parseShort((String) portId), node);
            } else {
                return new NodeConnector(nodeConnectorType, portId, node);
            }
        } catch (ConstructionException e1) {
            logger.error("",e1);
            return null;
        }
    }

    public static NodeConnector createOFNodeConnector(Short portId, Node node) {
        try {
            return new NodeConnector(NodeConnectorIDType.OPENFLOW, portId, node);
        } catch (ConstructionException e1) {
            logger.error("",e1);
            return null;
        }
    }

    public static Set<NodeConnector> createOFNodeConnectorSet(
            Set<Short> portIds, Node n) {
        try {
            Set<NodeConnector> nodeConnectors = new HashSet<NodeConnector>();
            for (Short ofPortID : portIds) {
                NodeConnector p = new NodeConnector(
                        NodeConnector.NodeConnectorIDType.OPENFLOW, Short
                                .valueOf(ofPortID), n);
                nodeConnectors.add(p);
            }
            return nodeConnectors;
        } catch (ConstructionException e1) {
            logger.error("",e1);
            return null;
        }
    }
}
