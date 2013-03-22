
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.inventory;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;

/**
 * The interface class that describes methods invoked from SAL toward the protocol
 * plugin to solicit existing inventory data.
 */
public interface IPluginInInventoryService {
    /**
     * The method retrieves all the existing nodes and properties attached
     *
     * @return map of {@link org.opendaylight.controller.sal.core.Node} and {@link org.opendaylight.controller.sal.core.Property}
     */
    public ConcurrentMap<Node, Map<String, Property>> getNodeProps();

    /**
     * The method retrieve all the existing nodeConnectors and their properties
     *
     * @param refresh true if it needs to solicit Openflow core; otherwise, retrieve from local cache.
     * @return map of {@link org.opendaylight.controller.sal.core.NodeConnector} and {@link org.opendaylight.controller.sal.core.Property}
     */
    public ConcurrentMap<NodeConnector, Map<String, Property>> getNodeConnectorProps(
            Boolean refresh);
}
