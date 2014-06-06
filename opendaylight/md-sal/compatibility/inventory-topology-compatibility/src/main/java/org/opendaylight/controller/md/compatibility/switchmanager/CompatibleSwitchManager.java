/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.compatibility.switchmanager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.ForwardingMode;
import org.opendaylight.controller.sal.core.MacAddress;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.Tier;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Subnet;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompatibleSwitchManager extends ConfigurableSwitchManager implements ISwitchManager {
    private static final  Logger LOG = LoggerFactory.getLogger(CompatibleSwitchManager.class);

    private DataBrokerService _dataService;

    public DataBrokerService getDataService() {
        return this._dataService;
    }

    public void setDataService(final DataBrokerService dataService) {
        this._dataService = dataService;
    }

    @Override
    public Status addNodeConnectorProp(final NodeConnector nodeConnector, final Property prop) {
        final DataModificationTransaction it = getDataService().beginTransaction();
        final NodeConnectorRef path = NodeMapping.toNodeConnectorRef(nodeConnector);
        return null;
    }

    @Override
    public Property createProperty(final String propName, final String propValue) {
        try {
            if (propName.equalsIgnoreCase(Description.propertyName)) {
                return new Description(propValue);
            } else if (propName.equalsIgnoreCase(Tier.TierPropName)) {
                return new Tier(Integer.parseInt(propValue));
            } else if (propName.equalsIgnoreCase(Bandwidth.BandwidthPropName)) {
                return new Bandwidth(Long.parseLong(propValue));
            } else if (propName.equalsIgnoreCase(ForwardingMode.name)) {
                return new ForwardingMode(Integer.parseInt(propValue));
            } else if (propName.equalsIgnoreCase(MacAddress.name)) {
                return new MacAddress(propValue);
            } else {
                LOG.debug("Not able to create {} property", propName);
            }
        } catch (Exception e) {
            LOG.debug("createProperty caught exception {}", e.getMessage());
        }

        return null;
    }

    @Override
    public boolean doesNodeConnectorExist(final NodeConnector nc) {
        return (getDataService().readOperationalData(NodeMapping.toNodeConnectorRef(nc).getValue()) != null);
    }

    @Override
    public byte[] getControllerMAC() {
        final Enumeration<NetworkInterface> nis;
        try {
            nis = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            LOG.error("Failed to acquire list of interfaces, cannot determine controller MAC", e);
            return null;
        }

        while (nis.hasMoreElements()) {
            final NetworkInterface ni = nis.nextElement();
            try {
                return ni.getHardwareAddress();
            } catch (SocketException e) {
                LOG.error("Failed to acquire controller MAC from interface {}", ni, e);
            }
        }

        // This happens when running controller on windows VM, for example
        // Try parsing the OS command output
        LOG.warn("Failed to acquire controller MAC: No physical interface found");
        return null;
    }

    @Override
    public Map<String,Property> getControllerProperties() {
        return Collections.<String, Property>emptyMap();
    }

    @Override
    public Property getControllerProperty(final String propertyName) {
        return null;
    }

    @Override
    public List<Switch> getNetworkDevices() {
        final InstanceIdentifier<Nodes> path = InstanceIdentifier.builder(Nodes.class).toInstance();
        final Nodes data = ((Nodes) getDataService().readOperationalData(path));
        final ArrayList<Switch> ret = new ArrayList<>();
        for (final Node node : data.getNode()) {
            try {
                ret.add(toSwitch(node));
            } catch (ConstructionException e) {
                throw new IllegalStateException(String.format("Failed to create switch {}", node), e);
            }
        }
        return ret;
    }

    @Override
    public NodeConnector getNodeConnector(final org.opendaylight.controller.sal.core.Node node, final String nodeConnectorName) {
        final NodeConnectorKey key = new NodeConnectorKey(new NodeConnectorId(nodeConnectorName));
        try {
            return new NodeConnector(NodeMapping.MD_SAL_TYPE, key, node);
        } catch (ConstructionException e) {
            throw new IllegalStateException(String.format("Failed to create node connector for {} {}", node, nodeConnectorName), e);
        }
    }

    @Override
    public Property getNodeConnectorProp(final NodeConnector nodeConnector, final String propName) {
        return getNodeConnectorProps(nodeConnector).get(propName);
    }

    @Override
    public Map<String,Property> getNodeConnectorProps(final NodeConnector nodeConnector) {
        final NodeConnectorRef ref = NodeMapping.toNodeConnectorRef(nodeConnector);
        return toAdProperties(readNodeConnector(ref.getValue()));
    }

    @Override
    public Set<NodeConnector> getNodeConnectors(final org.opendaylight.controller.sal.core.Node node) {
        final Node data = this.readNode(NodeMapping.toNodeRef(node).getValue());
        final HashSet<NodeConnector> ret = new HashSet<>();
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector nc : data.getNodeConnector()) {
            try {
                ret.add(new NodeConnector(NodeMapping.MD_SAL_TYPE, nc.getKey(), node));
            } catch (ConstructionException e) {
                throw new IllegalStateException(String.format("Failed to create node {} connector", node, nc.getKey()), e);
            }
        }
        return ret;
    }

    @Override
    public String getNodeDescription(final org.opendaylight.controller.sal.core.Node node) {
        return ((Description) getNodeProps(node).get(Description.propertyName)).getValue();
    }

    @Override
    public byte[] getNodeMAC(final org.opendaylight.controller.sal.core.Node node) {
        return ((MacAddress) getNodeProps(node).get(MacAddress.name)).getMacAddress();
    }

    @Override
    public Property getNodeProp(final org.opendaylight.controller.sal.core.Node node, final String propName) {
        return getNodeProps(node).get(propName);
    }

    @Override
    public Map<String,Property> getNodeProps(final org.opendaylight.controller.sal.core.Node node) {
        final NodeRef ref = NodeMapping.toNodeRef(node);
        return toAdProperties(((Node) getDataService().readOperationalData(ref.getValue())));
    }

    @Override
    public Set<org.opendaylight.controller.sal.core.Node> getNodes() {
        final InstanceIdentifier<Nodes> path = InstanceIdentifier.builder(Nodes.class).toInstance();
        final Nodes data = ((Nodes) getDataService().readOperationalData(path));
        final HashSet<org.opendaylight.controller.sal.core.Node> ret = new HashSet<>();
        for (final Node node : data.getNode()) {
            try {
                ret.add(new org.opendaylight.controller.sal.core.Node(NodeMapping.MD_SAL_TYPE, node.getKey()));
            } catch (ConstructionException e) {
                throw new IllegalStateException(String.format("Failed to create node for {}", node), e);
            }
        }
        return ret;
    }

    private static Switch toSwitch(final Node node) throws ConstructionException {
        return new Switch(new org.opendaylight.controller.sal.core.Node(NodeMapping.MD_SAL_TYPE, node.getKey()));
    }

    @Override
    public Set<NodeConnector> getPhysicalNodeConnectors(final org.opendaylight.controller.sal.core.Node node) {
        final NodeRef ref = NodeMapping.toNodeRef(node);
        final Node data = readNode(ref.getValue());
        final HashSet<NodeConnector> ret = new HashSet<>();
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector nc : data.getNodeConnector()) {
            final FlowCapableNodeConnector flowConnector = nc.getAugmentation(FlowCapableNodeConnector.class);
            try {
                ret.add(new NodeConnector(NodeMapping.MD_SAL_TYPE, nc.getKey(), node));
            } catch (ConstructionException e) {
                throw new IllegalStateException(String.format("Failed to create connector for {} on node {}", nc.getKey(), node), e);
            }
        }
        return ret;
    }

    private static Map<String,Property> toAdProperties(final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector connector) {
        return Collections.emptyMap();
    }

    private static Map<String,Property> toAdProperties(final Node connector) {
        return Collections.emptyMap();
    }

    private Node readNode(final InstanceIdentifier<? extends Object> ref) {
        return (Node) getDataService().readOperationalData((ref));
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector readNodeConnector(final InstanceIdentifier<? extends Object> ref) {
        return ((org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector) getDataService().readOperationalData(ref));
    }

    @Override
    public List<NodeConnector> getSpanPorts(final org.opendaylight.controller.sal.core.Node node) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public Subnet getSubnetByNetworkAddress(final InetAddress networkAddress) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public Set<NodeConnector> getUpNodeConnectors(final org.opendaylight.controller.sal.core.Node node) {
        final Node data = readNode(NodeMapping.toNodeRef(node).getValue());
        final HashSet<NodeConnector> ret = new HashSet<>();
        for (final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector nc : data.getNodeConnector()) {
            final FlowCapableNodeConnector flowConn = nc.<FlowCapableNodeConnector>getAugmentation(FlowCapableNodeConnector.class);
            if (flowConn != null && flowConn.getState() != null && !flowConn.getState().isLinkDown()) {
                try {
                    ret.add(new NodeConnector(NodeMapping.MD_SAL_TYPE, nc.getKey(), node));
                } catch (ConstructionException e) {
                    throw new IllegalStateException(String.format("Failed to create node connector for node {} connector {}", node, nc), e);
                }
            }
        }
        return ret;
    }

    @Override
    public Boolean isNodeConnectorEnabled(final NodeConnector nodeConnector) {
        final NodeConnectorRef ref = NodeMapping.toNodeConnectorRef(nodeConnector);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector data = readNodeConnector(ref.getValue());
        return true;
    }

    @Override
    public boolean isSpecial(final NodeConnector p) {
        final NodeConnectorRef ref = NodeMapping.toNodeConnectorRef(p);
        final org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector data = readNodeConnector(ref.getValue());
        return true;
    }

    @Override
    public Status removeControllerProperty(final String propertyName) {
        return null;
    }

    @Override
    public Status removeNodeAllProps(final org.opendaylight.controller.sal.core.Node node) {
        return null;
    }

    @Override
    public Status removeNodeConnectorAllProps(final NodeConnector nodeConnector) {
        return null;
    }

    @Override
    public Status removeNodeConnectorProp(final NodeConnector nc, final String propName) {
        return null;
    }

    @Override
    public Status removeNodeProp(final org.opendaylight.controller.sal.core.Node node, final String propName) {
        return null;
    }

    @Override
    public Status removePortsFromSubnet(final String name, final List<String> nodeConnectors) {
        return null;
    }

    @Override
    public Status removeSubnet(final String name) {
        return null;
    }

    @Override
    public Status setControllerProperty(final Property property) {
        return null;
    }

    @Override
    public void setNodeProp(final org.opendaylight.controller.sal.core.Node node, final Property prop) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public Status addPortsToSubnet(final String name, final List<String> nodeConnectors) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public Set<Switch> getConfiguredNotConnectedSwitches() {
        return null;
    }
}
