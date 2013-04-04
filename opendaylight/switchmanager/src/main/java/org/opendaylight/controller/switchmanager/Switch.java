
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;

/**
 * The class describes switch related information including L2 address, ports,
 * span ports and associated node representation
 */
public class Switch implements Serializable {
    private static final long serialVersionUID = 1L;
    private byte[] dataLayerAddress;
    private Set<NodeConnector> nodeConnectors;
    private List<NodeConnector> spanPorts;
    private Node node;

    /*
     * As we are adding switches on per event basis in a map, we do not need a default constructor
     * This way we can keep the validations internal, in the proper constructor
    public Switch() {
        this.swPorts = new HashSet<SwitchPortTuple>();
        this.spanPorts = new ArrayList<Short>(2);
    }
     */

    public Switch(Node node) {
        this.node = node;
        this.nodeConnectors = new HashSet<NodeConnector>();
        this.spanPorts = new ArrayList<NodeConnector>(2);
        this.dataLayerAddress = deriveMacAddress();
    }

    /**
     * @return the dataLayerAddress
     */
    public byte[] getDataLayerAddress() {
        return dataLayerAddress;
    }

    /**
     * @param dataLayerAddress the dataLayerAddress to set
     */
    public void setDataLayerAddress(byte[] dataLayerAddress) {
        this.dataLayerAddress = (dataLayerAddress == null) ? null
                : dataLayerAddress.clone();
    }

    /**
     * @return the nodeConnectors
     */
    public Set<NodeConnector> getNodeConnectors() {
        return nodeConnectors;
    }

    /**
     * @param nodeConnectors nodeConnector set
     */
    public void setNodeConnectors(Set<NodeConnector> nodeConnectors) {
        this.nodeConnectors = nodeConnectors;
    }

    public void addNodeConnector(NodeConnector nodeConnector) {
        if (!nodeConnectors.contains(nodeConnector)) {
            nodeConnectors.add(nodeConnector);
        }
    }

    public void removeNodeConnector(NodeConnector nodeConnector) {
        nodeConnectors.remove(nodeConnector);
    }

    public List<NodeConnector> getSpanPorts() {
        return spanPorts;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    private byte[] deriveMacAddress() {
        long dpid = (Long) this.node.getID();
        byte[] mac = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

        for (short i = 0; i < 6; i++) {
            mac[5 - i] = (byte) dpid;
            dpid >>= 8;
        }

        return mac;
    }

    public void addSpanPorts(List<NodeConnector> portList) {
        for (NodeConnector port : portList) {
            spanPorts.add(port);
        }
    }

    public void removeSpanPorts(List<NodeConnector> portList) {
        for (NodeConnector port : portList) {
            spanPorts.remove(port);
        }
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
        return "Switch[" + ReflectionToStringBuilder.toString(this) + "]";
    }
}
