
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

import org.opendaylight.controller.switchmanager.SpanConfig;
import org.opendaylight.controller.switchmanager.Subnet;
import org.opendaylight.controller.switchmanager.SubnetConfig;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.switchmanager.SwitchConfig;

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
     * @return "Success" or failure reason
     */
    public Status addSubnet(SubnetConfig configObject);

    /**
     * Remove a subnet configuration
     *
     * @param  configObject	refer to {@link Open Declaration org.opendaylight.controller.switchmanager.SubnetConfig}
     * @return "Success" or failure reason
     */
    public Status removeSubnet(SubnetConfig configObject);

    /**
     * Remove a subnet configuration given the name
     *
     * @param   name      subnet name
     * @return	"Success" or failure reason
     */
    public Status removeSubnet(String name);

    /**
     * Return a list of all known devices in the system
     *
     * @return	returns a list of {@link org.opendaylight.controller.switchmanager.Switch}
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
     * @param networkAddress 	the ip address in long format
     * @return 					the {@link org.opendaylight.controller.switchmanager.Subnet}
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
     * @return				status code
     */
    public Status addSpanConfig(SpanConfig configObject);

    /**
     * Remove a span port configuration
     *
     * @param SpanConfig refer to {@link Open Declaration org.opendaylight.controller.switchmanager.SpanConfig}
     * @return				status code
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
     */
    public void updateSwitchConfig(SwitchConfig cfgObject);

    /**
     * Return the previously configured Switch Configuration given the node id
     *
     * @param nodeId Node Identifier as specified by {@link org.opendaylight.controller.sal.core.Node}
     * @return {@link org.opendaylight.controller.switchmanager.SwitchConfig} resources
     */
    public SwitchConfig getSwitchConfig(String nodeId);

    /**
     * Add node connectors to a subnet
     *
     * @param name The subnet config name
     * @param nodeConnectors nodePorts string specified by {@link Open Declaration org.opendaylight.controller.switchmanager.SubnetConfig}
     * @return "Success" or failure reason
     */
    public Status addPortsToSubnet(String name, String nodeConnectors);

    /**
     * Remove node connectors from a subnet
     *
     * @param name				the subnet config name
     * @param nodeConnectors 	nodePorts string specified by {@link Open Declaration org.opendaylight.controller.switchmanager.SubnetConfig}
     * @return "Success" or failure reason
     */
    public Status removePortsFromSubnet(String name, String nodeConnectors);

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
     *   	   {@link org.opendaylight.controller.sal.core.Description} and/or
     * 		   {@link org.opendaylight.controller.sal.core.Tier} etc.
     */
    public Map<String, Property> getNodeProps(Node node);

    /**
     * Return a specific property of a node given the property name
     *
     * @param node 		{@link org.opendaylight.controller.sal.core.Node}
     * @param propName 	the property name specified by {@link org.opendaylight.controller.sal.core.Property} and its extended classes
     * @return {@link org.opendaylight.controller.sal.core.Property}
     */
    public Property getNodeProp(Node node, String propName);

    /**
     * Set a specific property of a node
     *
     * @param node 		{@link org.opendaylight.controller.sal.core.Node}
     * @param prop 		{@link org.opendaylight.controller.sal.core.Property}
     */
    public void setNodeProp(Node node, Property prop);

    /**
     * Remove a property of a node
     * 
     * @param nc 		{@link org.opendaylight.controller.sal.core.Node}
     * @param propName 	the property name specified by {@link org.opendaylight.controller.sal.core.Property} and its extended classes
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
     * 		   {@link org.opendaylight.controller.sal.core.Description} and/or
     * 		   {@link org.opendaylight.controller.sal.core.State} etc.
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
     * @param node 				{@link org.opendaylight.controller.sal.core.Node}
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
     * Return controller MAC address
	 *
     * @return MAC address in byte array
     */
    public byte[] getControllerMAC();

    /**
     * Return MAC address for a given node
     *
     * @param node	{@link org.opendaylight.controller.sal.core.Node}
     * @return MAC address in byte array
     */
    public byte[] getNodeMAC(Node node);

    /**
     * Return true if the host Refresh procedure (by sending ARP request probes
     * to known hosts) is enabled. By default, the procedure is enabled. This can
     * be overwritten by OSFI CLI "hostRefresh off".
     *
     * @return true if it is enabled; false if it's disabled.
     */
    public boolean isHostRefreshEnabled();

    /**
     * Return host refresh retry count
     *
     * @return host refresh retry count
     */
    public int getHostRetryCount();

	/**
	 * Create a Name/Tier/Bandwidth Property object based on given property
	 * name and value. Other property types are not supported yet.
	 * 
     * @param propName Name of the Property specified by {@link org.opendaylight.controller.sal.core.Property} and its extended classes
     * @param propValue Value of the Property specified by {@link org.opendaylight.controller.sal.core.Property} and its extended classes
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
    public String getNodeDescription(Node node);
}
