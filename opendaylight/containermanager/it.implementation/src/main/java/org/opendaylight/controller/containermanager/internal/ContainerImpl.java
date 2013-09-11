
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   ContainerImpl.java
 *
 * @brief  Class that instantiated per-container implements the
 * interface IContainer
 *
 *
 */
package org.opendaylight.controller.containermanager.internal;

import java.util.Dictionary;
import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.core.NodeConnector;
import java.util.Set;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.ContainerFlow;
import java.util.List;
import org.opendaylight.controller.sal.core.IContainer;

public class ContainerImpl implements IContainer {
    private String containerName = null;

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init(Component c) {
        Dictionary<?, ?> props = c.getServiceProperties();
        if (props != null) {
            this.containerName = (String) props.get("containerName");
        }
    }

    @Override
    public String getName() {
        return this.containerName;
    }

    @Override
    public List<ContainerFlow> getContainerFlows() {
        return null;
    }

    @Override
    public short getTag(Node n) {
        return (short) 0;
    }

    @Override
    public Set<NodeConnector> getNodeConnectors() {
        return null;
    }

    @Override
    public Set<Node> getNodes() {
        return null;
    }
}
