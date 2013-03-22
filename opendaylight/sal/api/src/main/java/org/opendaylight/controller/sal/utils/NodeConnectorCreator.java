
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

/**
 * The class provides helper functions to create a node connector
 *
 *
 */
public abstract class NodeConnectorCreator {
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
                        (Short) portId, node);
            } catch (ConstructionException e1) {
                e1.printStackTrace();
                return null;
            }
        }
        return null;
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
            return new NodeConnector(nodeConnectorType, portId, node);
        } catch (ConstructionException e1) {
            e1.printStackTrace();
            return null;
        }
    }

    public static NodeConnector createOFNodeConnector(Short portId, Node node) {
        try {
            return new NodeConnector(NodeConnectorIDType.OPENFLOW, portId, node);
        } catch (ConstructionException e1) {
            e1.printStackTrace();
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
            e1.printStackTrace();
            return null;
        }
    }
}
