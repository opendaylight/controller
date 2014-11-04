/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connection;

import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Interface that defines the methods available to the functional modules that operate
 * above SAL for disconnecting or connecting to a particular node.
 */
public interface IConnectionService {
    /**
     * Connect to a node with a specified node type.
     *
     * @param type Type of the node representing NodeIDType.
     * @param connectionIdentifier Convenient identifier for the applications to make use of
     * @param params Connection Params in Map format. This is entirely handled by the south-bound
     * plugins and is an opaque value for SAL. Typical values keyed inside this params are
     * Management IP-Address, Username, Password, Security Keys, etc...
     *
     * @return Node {@link org.opendaylight.controller.sal.core.Node}
     */
    public Node connect (String type, String connectionIdentifier, Map<ConnectionConstants, String> params);

    /**
     * Discover the node type and Connect to the first plugin that is able to connect with the specified parameters.
     *
     * @param type Type of the node representing NodeIDType.
     * @param connectionIdentifier Convenient identifier for the applications to make use of
     * @param params Connection Params in Map format. This is entirely handled by the south-bound
     * plugins and is an opaque value for SAL. Typical values keyed inside this params are
     * Management IP-Address, Username, Password, Security Keys, etc...
     *
     * @return Node {@link org.opendaylight.controller.sal.core.Node}
     */
    public Node connect (String connectionIdentifier, Map<ConnectionConstants, String> params);

    /**
     * Disconnect a Node that is connected to this Controller.
     *
     * @param node
     *            the node {@link org.opendaylight.controller.sal.core.Node}
     * @return Status {@link org.opendaylight.controller.sal.utils.Status}
     */
    public Status disconnect(Node node);

    /**
     * View Change notification
     *
     * @param node
     *            the node {@link org.opendaylight.controller.sal.core.Node}
     */
    public void notifyNodeDisconnectFromMaster(Node node);

    /**
     * View Change notification
     */
    public void notifyClusterViewChanged();
}