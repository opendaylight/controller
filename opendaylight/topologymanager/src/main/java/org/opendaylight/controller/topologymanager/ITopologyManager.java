
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topologymanager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Interface class that provides methods to interact with
 * network topology database
 */
public interface ITopologyManager {
    /**
     * Query to determine if the specified NodeConnector is connected
     * to another Node or is a leaf for the network
     * @param p The node connector
     * @return true if the NodeConnector is connected to another
     * switch false otherwise
     */
    public boolean isInternal(NodeConnector p);

    /**
     * Retrieves a map of all known link connections between nodes
     * including their properties
     * @return the map as specified
     */
    public Map<Edge, Set<Property>> getEdges();

    /**
     * Returns an unmodifiable map indexed by the Node and reporting
     * all the edges getting out/in from/to the Node
     * @return the map as specified
     */
    public Map<Node, Set<Edge>> getNodeEdges();

    /**
     * Add or Update an Host port in the topology manager DB
     *
     * @param p NodeConnector to which an host is connected
     * @param h the Host connected to the NodeConnector
     * @param t type of update if Add/Delete/Update
     */
    public void updateHostLink(NodeConnector p, Host h, UpdateType t,
            Set<Property> props);

    /**
     * Return a set of NodeConnector that have hosts attached to them
     *
     * @return A set with all the NodeConnectors known to have an host
     * attached
     */
    public Set<NodeConnector> getNodeConnectorWithHost();

    /**
     * Return the Host attached to a NodeConnector with Host
     *
     * @return The Host attached to a NodeConnector
     */
    public Host getHostAttachedToNodeConnector(NodeConnector p);

    /**
     * Returns a copy map which associates every node with all the
     * NodeConnectors of the node that have an Host attached to it
     *
     * @return A map of all the Nodes with NodeConnectors
     */
    public Map<Node, Set<NodeConnector>> getNodesWithNodeConnectorHost();

    /**
     * Adds user configured link
     *
     * @param link User configured link
     * @return "Success" or error reason
     */
    public Status addUserLink(TopologyUserLinkConfig link);

    /**
     * Deletes user configured link
     *
     * @param linkName The name of the user configured link
     * @return "Success" or error reason
     */
    public Status deleteUserLink(String linkName);

    /**
     * Saves user configured links into config file
     *
     * @return "Success" or error reason
     */
    public Status saveConfig();

    /**
     * Gets all the user configured links
     *
     * @return The map of the user configured links
     */
    public ConcurrentMap<String, TopologyUserLinkConfig> getUserLinks();
}
