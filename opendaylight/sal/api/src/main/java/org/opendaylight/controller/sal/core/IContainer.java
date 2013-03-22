
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   IContainer.java
 *
 * @brief  Interface used to retrieve the status of a given Container
 *
 *
 */

package org.opendaylight.controller.sal.core;

import java.util.List;
import java.util.Set;

/**
 *
 * Interface used to retrieve the status of a given Container
 */
public interface IContainer {
    /**
     * Returns the Name of the container described
     *
     * @return the container Name
     */
    public String getName();

    /**
     * The list of container flows associated with a container
     *
     * @return The list of FlowSpecs associated with the container
     */
    public List<ContainerFlow> getContainerFlows();

    /**
     * Return the tag on which a Node is expected to receive traffic
     * for a given container.
     *
     * @param n The node for which we want to get the Tag
     *
     * @return the tag on which we expect to receive traffic on a
     * given Node for a given container
     */
    public short getTag(Node n);

    /**
     * Return an array of all the NodeConnectors that are part of the
     * container
     *
     * @return The array of nodeConnectors part of the container
     */
    public Set<NodeConnector> getNodeConnectors();

    /**
     * Return an array of all the Nodes that are part of a container
     *
     * @return The array of Nodes that are part of the container
     */
    public Set<Node> getNodes();
}
