/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.statistics.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.rest.flow.NodeFlowRTO;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class FlowStatistics {
    @XmlElement
    private Node node;
    @XmlElement(name="flowStat")
    private List<NodeFlowRTO> flowStat;

    // To satisfy JAXB
    @SuppressWarnings("unused")
	private FlowStatistics() {
    }

    public FlowStatistics(Node node, List<NodeFlowRTO> flowStat) {
        super();
        this.node = node;
        this.flowStat = flowStat;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public List<NodeFlowRTO> getFlowStats() {
        return flowStat;
    }

    public void setFlowStats(List<NodeFlowRTO> flowStats) {
        this.flowStat = flowStats;
    }
}
