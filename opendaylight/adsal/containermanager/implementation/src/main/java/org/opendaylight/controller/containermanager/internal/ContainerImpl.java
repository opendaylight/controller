
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
 */
package org.opendaylight.controller.containermanager.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.containermanager.ContainerData;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;

public class ContainerImpl implements IContainer {
    private String containerName = null;
    private IContainerInternal iContainerInternal = null;

    public void setIContainerInternal(IContainerInternal s) {
        this.iContainerInternal = s;
    }

    public void unsetIContainerInternal(IContainerInternal s) {
        if (this.iContainerInternal == s) {
            this.iContainerInternal = null;
        }
    }

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
        List<ContainerFlow> list = new ArrayList<ContainerFlow>();

        ContainerData d = this.iContainerInternal.getContainerData(this.containerName);
        if (d != null) {
            list.addAll(d.getContainerFlowSpecs());
        }
        return list;
    }

    @Override
    public short getTag(Node n) {
        ContainerData d = this.iContainerInternal.getContainerData(this.containerName);
        if (d != null) {
            return d.getStaticVlan();
        }
        // Return 0 because in containerData that means an unassigned tag
        return (short) 0;
    }

    @Override
    public Set<NodeConnector> getNodeConnectors() {
        Set<NodeConnector> set = new HashSet<NodeConnector>();

        ContainerData d = this.iContainerInternal.getContainerData(this.containerName);
        if (d != null) {
            ConcurrentMap<Node, Set<NodeConnector>> m = d.getSwPorts();
            if (m != null) {
                for (Map.Entry<Node, Set<NodeConnector>> entry : m.entrySet()) {
                    set.addAll(entry.getValue());
                }
            }
        }
        return set;
    }

    @Override
    public Set<Node> getNodes() {
        Set<Node> set = new HashSet<Node>();

        ContainerData d = this.iContainerInternal.getContainerData(this.containerName);
        if (d != null) {
            ConcurrentMap<Node, Set<NodeConnector>> m = d.getSwPorts();
            if (m != null) {
                set.addAll(m.keySet());
            }
        }
        return set;
    }

    @Override
    public String getContainerAdminRole() {
        return iContainerInternal.getContainerData(containerName).getContainerAdminRole();
    }

    @Override
    public String getContainerOperatorRole() {
        return iContainerInternal.getContainerData(containerName).getContainerOperatorRole();
    }
}
