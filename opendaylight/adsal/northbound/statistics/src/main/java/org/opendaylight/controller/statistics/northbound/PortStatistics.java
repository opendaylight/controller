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
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;

@XmlRootElement(name = "nodePortStatistics")
@XmlAccessorType(XmlAccessType.NONE)
public class PortStatistics {
    @XmlElement
    private Node node;
    @XmlElement
    private List<NodeConnectorStatistics> portStatistic;

    // To satisfy JAXB
    @SuppressWarnings("unused")
        private PortStatistics() {
    }

    public PortStatistics(Node node, List<NodeConnectorStatistics> portStats) {
        super();
        this.node = node;
        this.portStatistic = portStats;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public List<NodeConnectorStatistics> getPortStats() {
        return portStatistic;
    }

    public void setFlowStats(List<NodeConnectorStatistics> portStats) {
        this.portStatistic = portStats;
    }

}
