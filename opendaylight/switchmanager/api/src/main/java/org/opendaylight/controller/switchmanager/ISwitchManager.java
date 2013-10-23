
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Primary purpose of this interface is to provide methods for application to
 * access various system resources and inventory data including nodes, node
 * connectors and their properties, Layer3 configurations, Span configurations,
 * node configurations, network device representations viewed by Controller Web
 * applications.
 */
public interface ISwitchManager {
    /**
     * Add a subnet configuration
     *
     * @param  configObject refer to {@link Open Declaration org.opendaylight.controller.switchmanager.SubnetConfig}
     * @return the Status object representing the result of the request
     */
    public Status addSubnet(SubnetConfig configObject);

    /**
     * Remove a subnet configuration
     *
     * @param  configObject refer to {@link Open Declaration org.opendaylight.controller.switchmanager.SubnetConfig}
     * @return the Status object representing the result of the request
     */
    public Status removeSubnet(SubnetConfig configObject);

    /**
     * Modify a subnet configuration
     *
     * @param  configObject refer to {@link Open Declaration org.opendaylight.controller.switchmanager.SubnetConfig}
     * @return the Status object representing the result of the request
     */
    public Status modifySubnet(SubnetConfig configObject);

    /**
     * Remove a subnet configuration given the name
     *
     * @param   name      subnet name
     * @return  "Success" or failure reason
     */
    public Status removeSubnet(String name);

    /**
     * Return a list of all known devices in the system
     *
     * @return  returns a list of {@link org.opendaylight.controller.switchmanager.Switch}
     */
    public List<Switch> getNetworkDevices();

    /**
     * Return a list of subnet that were previously configured
     *
     * @return list of L3 interface {@link org.opendaylight.controller.switchmanager.SubnetConfig} configurations
     */
    public List<SubnetConfig> getSubnetsConfigList();

    /**
     * Return the subnet configuration
     *
     * @param   subnet      subnet
     * @return a L3 interface {@link org.opendaylight.controller.switchmanager.SubnetConfig} configuration
     */
    public SubnetConfig getSubnetConfig(String subnet);

    /**
     * Return a subnet configuration given the network address
     *
     * @param networkAddress    the ip address in long format
     * @return                                  the {@link org.opendaylight.controller.switchmanager.Subnet}
     */
    public Subnet getSubnetByNetworkAddress(InetAddress networkAddress);

    /**
     * Save the current switch configurations
     *
     * @return the status code
     */
    public Status saveSwitchConfig();

    /**
     * Add a span port configuration
     *
     * @param SpanConfig refer to {@link Open Declaration org.opendaylight.controller.switchmanager.SpanConfig}
     * @return              status code
     */
    public Status addSpanConfig(SpanConfig configObject);

    /**
     * Remove a span port configuration
     *
     * @param SpanConfig refer to {@link Open Declaration org.opendaylight.controller.switchmanager.SpanConfig}
     * @return              status code
     */
    public Status removeSpanConfig(SpanConfig cfgObject);

    /**
     * Return a list of span configurations that were configured previously
     *
     * @return list of {@link org.opendaylight.controller.switchmanager.SpanConfig} resources
     */
    public List<SpanConfig> getSpanConfigList();

    /**
     * Return the list of span ports of a given node
     *
     * @param node {@link org.opendaylight.controller.sal.core.Node}
     * @return the list of span {@link org.opendaylight.controller.sal.core.NodeConnector} of the node
     */
    public List<NodeConnector> getSpanPorts(Node node);

    /**
     * Update Switch specific configuration such as Switch Name and Tier
     *
     * @param cfgConfig refer to {@link Open Declaration org.opendaylight.controller.switchmanager.SwitchConfig}
     *
     * @deprecated replaced by updateNodeConfig(switchConfig)
     */
    @Deprecated
    public void updateSwitchConfig(SwitchConfig cfgObject);

    /**
     * Update Node specific configuration such as Node Name and Tier
     *
     * @param cfgConfig
     *            refer to {@link Open Declaration
     *            org.opendaylight.controller.switchmanager.SwitchConfig}
     * @return "Success" or failure reason
     */
    public Status updateNodeConfig(SwitchConfig switchConfig);

    /**
     * Removes node properties configured by the user
     *
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return "Success" or failure reason
     */
    public Status removeNodeConfig(String nodeId);

    /**
     * Return the previously configured Switch Configuration given the node id
     *
     * @param nodeId
     *            Node Identifier as specified by
     *            {@link org.opendaylight.controller.sal.core.Node}
     * @return {@link org.opendaylight.controller.switchmanager.SwitchConfig}
     *         resources
     */
    public SwitchConfig getSwitchConfig(String nodeId);

    /**
     * Add node connectors to a subnet
     *
     * @param name The configured subnet name
     * @param nodeConnectors list of string each representing a node connector as specified by {@link Open Declaration org.opendaylight.controller.sal.core.NodeConnector}
     * @return The Status object indicating the result of this request
     */
    public Status addPortsToSubnet(String name, List<String> nodeConnectors);

    /**
     * Remove node connectors from a subnet
     *
     * @param name              the configured subnet name
     * @param nodeConnectors    list of string each representing a node connector as specified by {@link Open Declaration org.opendaylight.controller.sal.core.NodeConnector}
     * @return The Status object indicating the result of this request
     */
    public Status removePortsFromSubnet(String name, List<String> nodeConnectors);

    /**
     * Return the set of all the nodes
     *
     * @return set of {@link org.opendaylight.controller.sal.core.Node}
     */
    public Set<Node> getNodes();

    /**
     * Return all the properties of a node
     *
     * @param node {@link org.opendaylight.controller.sal.core.Node}
     * @return map of {@link org.opendaylight.controller.sal.core.Property} such as
     *             {@link org.opendaylight.controller.sal.core.Description} and/or
     *             {@link org.opendaylight.controller.sal.core.Tier} etc.
     */
    public Map<String, Property> getNodeProps(Node node);

    /**
     * Return a specific property of a node given the property name
     *
     * @param node              {@link org.opendaylight.controller.sal.core.Node}
     * @param propName  the property name specified by {@link org.opendaylight.controller.sal.core.Property} and its extended classes
     * @return {@link org.opendaylight.controller.sal.core.Property}
     */
    public Property getNodeProp(Node node, String propName);

    /**
     * Set a specific property of a node
     *
     * @param node              {@link org.opendaylight.controller.sal.core.Node}
     * @param prop              {@link org.opendaylight.controller.sal.core.Property}
     */
    public void setNodeProp(Node node, Property prop);

    /**
     * Remove a property of a node
     *
     * @param nc                {@link org.opendaylight.controller.sal.core.Node}
     * @param propName  the property name specified by {@link org.opendaylight.controller.sal.core.Property} and its extended classes
     * @return success or failed reason
     */
    public Status removeNodeProp(Node node, String propName);

    /**
     * Remove all the properties of a node
     *
     * @param node {@link org.opendaylight.controller.sal.core.Node}
     * @return success or failed reason
     */
    public Status removeNodeAllProps(Node node);

    /**
     * Return all the node connectors in up state for a given node
     *
     * @param node {@link org.opendaylight.controller.sal.core.Node}
     * @return set of {@link org.opendaylight.controller.sal.core.NodeConnector}
     */
    public Set<NodeConnector> getUpNodeConnectors(Node node);

    /**
     * Return all the node connectors including those special ones. Status of each node connector varies.
     *
     * @param node {@link org.opendaylight.controller.sal.core.Node}
     * @return all listed {@link org.opendaylight.controller.sal.core.NodeConnector}
     */
    public Set<NodeConnector> getNodeConnectors(Node node);

    /**
     * Return all the physical node connectors of a node. Status of each node connector varies.
     *
     * @param node {@link org.opendaylight.controller.sal.core.Node}
     * @return all physical {@link org.opendaylight.controller.sal.core.NodeConnector}
     */
    public Set<NodeConnector> getPhysicalNodeConnectors(Node node);

    /**
     * Return all the properties of a node connector
     *
     * @param nodeConnector {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @return map of {@link org.opendaylight.controller.sal.core.Property} such as
     *             {@link org.opendaylight.controller.sal.core.Description} and/or
     *             {@link org.opendaylight.controller.sal.core.State} etc.
     */
    public Map<String, Property> getNodeConnectorProps(
            NodeConnector nodeConnector);

    /**
     * Return a specific property of a node connector given the property name
     *
     * @param nodeConnector {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @param propName property name specified by {@link org.opendaylight.controller.sal.core.Property} and its extended classes
     * @return {@link org.opendaylight.controller.sal.core.Property}
     */
    public Property getNodeConnectorProp(NodeConnector nodeConnector,
            String propName);

    /**
     * Add a node connector and its property
     *
     * @param nodeConnector {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @param prop {@link org.opendaylight.controller.sal.core.Property}
     * @return success or failed reason
     */
    public Status addNodeConnectorProp(NodeConnector nodeConnector,
            Property prop);

    /**
     * Remove a property of a node connector
     *
     * @param nc {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @param propName property name specified by {@link org.opendaylight.controller.sal.core.Property} and its extended classes
     * @return success or failed reason
     */
    public Status removeNodeConnectorProp(NodeConnector nc, String propName);

    /**
     * Remove all the properties of a node connector
     *
     * @param nodeConnector {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @return success or failed reason
     */
    public Status removeNodeConnectorAllProps(NodeConnector nodeConnector);

    /**
     * Return the node connector given its name
     *
     * @param node                              {@link org.opendaylight.controller.sal.core.Node}
     * @param nodeConnectorName node connector identifier specified by {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @return {@link org.opendaylight.controller.sal.core.NodeConnector}
     */
    public NodeConnector getNodeConnector(Node node, String nodeConnectorName);

    /**
     * Return whether the specified node connector is a special node port
     * Example of node's special node connector are software stack, hardware path, controller...
     *
     * @param p {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @return true or false
     */
    public boolean isSpecial(NodeConnector p);

    /**
     * Check if the node connector is up running
     *
     * @param nodeConnector {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @return true or false
     */
    public Boolean isNodeConnectorEnabled(NodeConnector nodeConnector);

    /**
     * Test whether the given node connector exists.
     *
     * @param nc  {@link org.opendaylight.controller.sal.core.NodeConnector}
     * @return    True if exists, false otherwise.
     */
    public boolean doesNodeConnectorExist(NodeConnector nc);

    /**
     * Return controller MAC address
         *
     * @return MAC address in byte array
     */
    public byte[] getControllerMAC();

    /**
     * Return MAC address for a given node
     *
     * @param node  {@link org.opendaylight.controller.sal.core.Node}
     * @return MAC address in byte array
     */
    public byte[] getNodeMAC(Node node);

    /**
     * Create a Name/Tier/Bandwidth Property object based on given property name
     * and value. Other property types are not supported yet.
     *
     * @param propName
     *            Name of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @param propValue
     *            Value of the Property specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return {@link org.opendaylight.controller.sal.core.Property}
     */
    public Property createProperty(String propName, String propValue);

    /**
     * Returns the description for the specified node. It is either the one
     * configured by user or the description advertised by the node.
     *
     * @param node the network node identifier
     * @return the description of the specified node. If no description is
     * configured and the network node does not provide its description,
     * an empty string is returned.
     */
    @Deprecated
    public String getNodeDescription(Node node);

    /**
     * Return all the properties of the controller
     *
     * @return map of {@link org.opendaylight.controller.sal.core.Property} such
     *         as {@link org.opendaylight.controller.sal.core.Description}
     *         and/or {@link org.opendaylight.controller.sal.core.Tier} etc.
     */
    public Map<String, Property> getControllerProperties();

    /**
     * Return a specific property of the controller given the property name
     *
     * @param propName
     *            the property name specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return {@link org.opendaylight.controller.sal.core.Property}
     */
    public Property getControllerProperty(String propertyName);

    /**
     * Set a specific property of the controller
     *
     * @param property
     *            {@link org.opendaylight.controller.sal.core.Property}
     * @return
     */
    public Status setControllerProperty(Property property);

    /**
     * Remove a property of a node
     *
     * @param propertyName
     *            the property name specified by
     *            {@link org.opendaylight.controller.sal.core.Property} and its
     *            extended classes
     * @return success or failed reason
     */
    public Status removeControllerProperty(String propertyName);
}
