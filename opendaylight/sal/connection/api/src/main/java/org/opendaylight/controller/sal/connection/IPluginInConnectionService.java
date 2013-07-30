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
 * @file IPluginInConnectionService.java
 *
 * @brief Connection interface to be implemented by protocol plugins
 */
public interface IPluginInConnectionService {
    /**
     * Disconnect a Node that is connected to this Controller.
     *
     * @param node
     * @param flow
     */
    public Status disconnect(Node node);

    /**
     * Connect to a node
     *
     * @param connectionIdentifier Convenient identifier for the applications to make use of
     * @param params Connection Params in Map format. This is entirely handled by the south-bound
     * plugins and is an opaque value for SAL. Typical values keyed inside this params are
     * Management IP-Address, Username, Password, Security Keys, etc...
     *
     * @return Node
     */
    public Node connect (String connectionIdentifier, Map<ConnectionConstants, String> params);

    /**
     * View Change notification
     */
    public void notifyClusterViewChanged();

    /**
     * Node Disconnected from the node's master controller.
     */
    public void notifyNodeDisconnectFromMaster(Node node);

}