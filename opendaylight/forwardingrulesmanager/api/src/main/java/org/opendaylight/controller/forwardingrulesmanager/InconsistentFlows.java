/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.forwardingrulesmanager;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.utils.Status;

/**
 * The class InconsistentFlows is the wrapper class for holding the list of
 * flows present on a node but not on controller and vice versa
 */
public class InconsistentFlows {

    private final Node node;

    private final List<FlowEntry> flowEntriesOnControllerButNotOnNode;

    private final List<Flow> flowsOnNodeButNotOnController;

    private final Status status;

    /**
     * CTOR
     * @param status
     * @param node
     * @param flowEntriesOnControllerButNotOnNode
     * @param flowsOnNodeButNotOnController
     */
    public InconsistentFlows(Status status, Node node, List<FlowEntry> flowEntriesOnControllerButNotOnNode,
            List<Flow> flowsOnNodeButNotOnController) {
        this.status = status;
        this.node = node;
        this.flowEntriesOnControllerButNotOnNode = flowEntriesOnControllerButNotOnNode;
        this.flowsOnNodeButNotOnController = flowsOnNodeButNotOnController;
    }

    /**
     * @return the node
     */
    public Node getNode() {
        return node;
    }

    /**
     * @return the flowEntriesOnControllerButNotOnNode
     */
    public List<FlowEntry> getFlowEntriesOnControllerButNotOnNode() {
        return flowEntriesOnControllerButNotOnNode;
    }

    /**
     * @return the flowsOnControllerButNotOnNode
     */
    public List<Flow> getFlowsOnControllerButNotOnNode() {
        List<Flow> flowList = new ArrayList<Flow>();
        for (FlowEntry flowEntry : flowEntriesOnControllerButNotOnNode) {
            flowList.add(flowEntry.getFlow());
        }
        return flowList;
    }

    /**
     * @return the flowsOnNodeButNotOnController
     */
    public List<Flow> getFlowsOnNodeButNotOnController() {
        return flowsOnNodeButNotOnController;
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("InconsistentFlows [node=");
        builder.append(node);
        builder.append(", flowEntriesOnControllerButNotOnNode=");
        builder.append(flowEntriesOnControllerButNotOnNode);
        builder.append(", flowsOnNodeButNotOnController=");
        builder.append(flowsOnNodeButNotOnController);
        builder.append(", status=");
        builder.append(status);
        builder.append("]");
        return builder.toString();
    }

}
