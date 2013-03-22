
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;

/**
 * Represents a flow applications request Forwarding Rules Manager to install
 * on a network node. A FlowEntry is constituted of a flow (match + actions),
 * the target network node, and the flow name. It also includes a group name. 
 * For instance the flows constituting a policy all share the same group name.
 */
public class FlowEntry implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;
    private String groupName; // group name
    private String flowName; // flow name (may be null)
    private Node node; // network node where to install the flow
    private Flow flow; // match + action

    public FlowEntry(String groupName, String flowName, Flow flow, Node node) {
        this.groupName = groupName;
        this.flow = flow;
        this.node = node;
        this.flowName = (flowName != null) ? flowName : constructFlowName();
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String name) {
        this.groupName = name;
    }

    /**
     * Return the actual Flow contained in this entry
     *
     * @return the flow
     */
    public Flow getFlow() {
        return flow;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node n) {
        this.node = n;
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String n) {
        this.flowName = n;
    }

    @Override
    public FlowEntry clone() {
        FlowEntry cloned = null;
        try {
            cloned = (FlowEntry) super.clone();
            cloned.flow = this.flow.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return cloned;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "FlowEntry[flowName = " + flowName + ", groupName = "
                + groupName + ",node = " + node + ", flow = " + flow + "]";
    }

    private String constructFlowName() {
        return this.groupName + "_" + new Date().toString();
    }

    public boolean equalsByNodeAndName(Node node, String flowName) {
        return this.node.equals(node) && this.flowName.equals(flowName);
    }

    /**
     * Merges the current Flow with the passed Container Flow
     *
     * Note: Container Flow merging is not an injective function.
     * Be m1 and m2 two different matches, and be f() the flow merge
     * function, such that y1 = f(m1) and y2 = f(m2) are the two merged
     * matches, we may have: y1 = y2
     *
     *
     * @param containerFlow
     * @return this merged FlowEntry
     */
    public FlowEntry mergeWith(ContainerFlow containerFlow) {
        Match myMatch = flow.getMatch();

        // Based on this flow direction, rearrange the match
        Match match = containerFlow.getMatch();

        // Merge
        myMatch.mergeWithFilter(match);

        // Replace this Flow's match with merged version
        flow.setMatch(myMatch);

        return this;
    }
}
