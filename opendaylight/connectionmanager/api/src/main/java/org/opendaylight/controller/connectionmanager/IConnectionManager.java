/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.connectionmanager;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.connection.ConnectionLocality;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Connection Manager provides south-bound connectivity services. The APIs are
 * currently focused towards Active-Active Clustering support wherein the node
 * can connect to any of the Active Controller in the Cluster. This component
 * can also host the necessary logic for south-bound connectivity when partial
 * cluster is identified during Partition scenarios.
 *
 * This (and its corresponding implementation) component can also be enhanced
 * further for more fancy algorithms/criteria for connection acceptance.
 */

public interface IConnectionManager {
    /**
     * This method returns Connectivity Algorithm (Scheme) that is currently
     * being used.
     *
     * @return ConnectionMgmtScheme Enum that represents the active scheme.
     */
    public ConnectionMgmtScheme getActiveScheme();

    /**
     * Method that will retrieve and return a Set of Nodes that is currently
     * connected to the given controller.
     *
     * @param controller
     *            InetAddress of the Controller that is currently connected to a
     *            set of Nodes.
     *
     * @return Set<Node> Set of Nodes connected to a controller.
     */
    public Set<Node> getNodes(InetAddress controller);

    /**
     * Method that will retrieve and return a Set of Nodes that is currently
     * connected to the controller on which this method is executed.
     *
     * @return Set<Node> Set of Nodes connected to this controller.
     */
    public Set<Node> getLocalNodes();

    /**
     * @deprecated Use getLocalityStatus(Node node) instead.
     *
     *             Method to test if a node is local to a controller.
     *
     * @param node
     *            The node for which the locality is being tested
     * @return true if node is local to this controller.<br>
     *         false if either node is not connected to this controller or not
     *         connected to any other controllers in the cluster.
     */
    public boolean isLocal(Node node);

    /**
     * getLocalityStatus provides the tri-state connectivity status as opposed
     * to the binary status returned by isLocal. ConnectionLocality enum that is
     * returned by this method also includes the case of a Node not connected to
     * any of the controllers in the cluster.
     *
     * @param node
     *            The node for which the locality is being verified
     * @return ConnectionLocality
     */
    public ConnectionLocality getLocalityStatus(Node node);

    /**
     * Disconnect a Node from the controller.
     *
     * @return Status of the disconnect Operation.
     */
    public Status disconnect(Node node);

    /**
     * Connect to a node
     *
     * @param connectionIdentifier
     *            identifier with which the application would refer to a given
     *            connection.
     * @param params
     *            Connection Params in Map format. This is entirely handled by
     *            the south-bound plugins and is an opaque value for SAL or
     *            Connection Manager. Typical values keyed inside this params
     *            are Management IP-Address, Username, Password, Security Keys,
     *            etc...
     *
     * @return Node Node connected to.
     */
    public Node connect(String connectionIdentifier,
            Map<ConnectionConstants, String> params);

    /**
     * Connect to a node
     *
     * @param type
     *            Type of the node representing NodeIDType.
     * @param connectionIdentifier
     *            identifier with which the application would refer to a given
     *            connection.
     * @param params
     *            Connection Params in Map format. This is entirely handled by
     *            the south-bound plugins and is an opaque value for SAL or
     *            Connection Manager. Typical values keyed inside this params
     *            are Management IP-Address, Username, Password, Security Keys,
     *            etc...
     *
     * @return Status of the Connect Operation.
     */
    public Node connect(String type, String connectionIdentifier,
            Map<ConnectionConstants, String> params);

    /**
     * Retrieve list of cluster-members to which Node is connected to
     *
     * @param node
     *            Node for which cluster-members to be retrieved
     *
     * @return Set<InetAddress> List of cluster-member addresses to which the
     *         node is connected
     */
    public Set<InetAddress> getControllers(Node node);
}
