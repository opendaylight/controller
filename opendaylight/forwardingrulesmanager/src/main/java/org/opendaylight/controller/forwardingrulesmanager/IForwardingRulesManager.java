
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwardingrulesmanager;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.Status;

/**
 * Interface that describes methods for installing or removing forwarding rules
 * and to access to the flows database.
 *
 */
public interface IForwardingRulesManager {

    /**
     * It requests FRM to install the passed Flow Entry. FRM will request
     * the SDN protocol plugin to install the flow on the network node.
     * Based on the result of this operation FRM will update its database
     * accordingly and will return the proper {@code Status} code.
     * 
	 * @param flow the flow entry to install
	 * @return the {@code Status} object indicating the result of this action.
	 */
    public Status installFlowEntry(FlowEntry flow);

    /**
     * It requests FRM to remove the passed Flow Entry. FRM will request
     * the SDN protocol plugin to uninstall the flow from the network node.
     * Based on the result of this operation FRM will update its database
     * accordingly and will return the proper {@code Status} code.
     * 
     * @param flow the flow entry to uninstall
     * @return the {@code Status} object indicating the result of this action
     */
    public Status uninstallFlowEntry(FlowEntry flow);

    /**
     * It requests FRM to replace the currently installed Flow Entry with the
     * new one. It is up to the SDN protocol plugin to decide how to convey
     * this message to the network node. It could be a delete + add or a single
     * modify message depending on the SDN protocol specifications
     * If the current flow is equal to the new one  it will be a no op and
     * success code is returned.
     * 
     * @param current the current flow entry to modify
     * @param newone the new flow entry which will replace the current one
     * @return the {@code Status} object indicating the result of this action
     */
    public Status modifyFlowEntry(FlowEntry current, FlowEntry newone);

    /**
     * It requests the FRM to replace the currently installed Flow Entry with
     * the new one. The currently installed entry is derived by the Match
     * portion of the passed Flow. FRM looks in its database for a previously
     * installed FlowEntry which Match equals the Match of the passed Flow.
     * If it finds it, it will request the SDN protocol plugin to replace the
     * existing flow with the new one on the network node. If it does not
     * find it, it will request plugin to add the new flow. If the passed entry
     * is not valid an error code is returned.
     * If the existing flow is equal to the passed one it will be a no op and
     * success code is returned.
     * 
     * @param newone the new flow entry to install
     * @return the {@code Status} object indicating the result of this action
     */
    public Status modifyOrAddFlowEntry(FlowEntry newone);

    /**
     * Check whether the passed flow entry conflicts with the Container flows
     * 
     * @param flow the flow entry to test
     * @return true if conflicts, false otherwise
     */
    public boolean checkFlowEntryConflict(FlowEntry flow);

    /**
     * Returns the list of Flow entries across network nodes which are part of the
     * same flow group, policy
     *
     * @param group the group name
     * @return the list of flow entries belonging to the specified group
     */
    public List<FlowEntry> getFlowEntriesForGroup(String group);

    /**
     * Add a list of output port to the flow with the specified name on the specified network node
     *
     * @param node	the network node
     * @param flowName	the flow name
     * @param dstPort the list of ports to be added to the flow output actions
     */
    public void addOutputPort(Node node, String flowName,
            List<NodeConnector> dstPort);

    /**
     * Remove a list of output port from the flow with the specified name on the specified network node
     *
     * @param node the network node
     * @param flowName	the flow name
     * @param dstPortthe list of ports to be removed from the flow output actions
     */
    public void removeOutputPort(Node node, String flowName,
            List<NodeConnector> dstPort);

    /**
     * Replace the current output port in the specified flow with the specified one
     *
     * @param node	the network node
     * @param groupName the group name
     * @param flowName	the flow name
     * @param dstPort	the new output action port
     */
    public void replaceOutputPort(Node node, String flowName,
            NodeConnector outPort);

    /**
     * Returns the output port configured on the specified flow
     *
     * @param node	the network node
     * @param flowName	the flow name
     * @return the output action port for the specified flow
     */
    public NodeConnector getOutputPort(Node node, String flowName);

    /**
     * Returns all the troubleshooting information that applications
     * have set along with the policy they have configured through
     * forwarding rules manger.
     * 
     * @return the collection of troubleshooting objects
     */
    public Map<String, Object> getTSPolicyData();

    /**
     * Set the troubleshooting information for the policy
     *
     * @param policyname the flow group name
     * @param o	the object containing the troubleshooting information
     * @param add	true for adding, false for removing
     */
    public void setTSPolicyData(String policyName, Object o, boolean add);

    /**
     * Returns the troubleshooting information that was set for the specified policy
     * 
     * @param groupName the flows group name
     * @return the troubleshooting info object
     */
    public Object getTSPolicyData(String policyName);

    /**
     * Returns the specifications of all the flows configured for all the 
     * switches on the current container
     *
     * @return the list of flow configurations present in the database
     */
    public List<FlowConfig> getStaticFlows();

    /**
     * Returns the specifications of all the flows configured for 
     * the given switch on the current container
     *
     * @param node	the network node identifier
     * @return	the list of {@code FlowConfig} objects
     */
    public List<FlowConfig> getStaticFlows(Node node);

    /**
     * Returns the specification of the flow configured for the given
     * network node on the current container
     *
     * @param name the flow name
     * @param n the netwrok node identifier
     * @return the {@code FlowConfig} object
     */
    public FlowConfig getStaticFlow(String name, Node n);

    /**
     * Returns the list of names of flows configured for the given 
     * Network node on the current container
     *
     * @param node the network node identifier
     * @return the list of flow names
     */
    public List<String> getStaticFlowNamesForNode(Node node);

    /**
     * Returns the list of Node(s) for which a static flow has been configured
     *
     * @return the list of network nodes
     */
    public List<Node> getListNodeWithConfiguredFlows();

    /**
     * Save the flow configured so far to file
     * 
     * @return the {@code Status} object indicating the result of this action.
     */
    public Status saveConfig();

    /**
     * Add a flow specified by the {@code FlowConfig} object on the current container
     * 
     * @param config the {@code FlowConfig} object representing the static flow
     * @param restore if set to true, the config object validation will be skipped.
     * 				 Used only internally, always set it to false.
     * @return the {@code Status} object indicating the result of this action.
     */
    public Status addStaticFlow(FlowConfig config, boolean restore);

    /**
     * Remove a flow specified by the {@code FlowConfig} object on the current container
     * 
     * @param config the {@code FlowConfig} object representing the static flow
     * @return the {@code Status} object indicating the result of this action
     */
    public Status removeStaticFlow(FlowConfig config);

    /**
     * Replace the flow identified by the {@code FlowConfig.name} name for
     * the {@code FlowConfig.node} network node with the new flow specified
     * by {@code FlowConfig} object
     *
     * @param config the {@code FlowConfig} object
     * @returnthe {@code Status} object indicating the result of this action
     */
    public Status modifyStaticFlow(FlowConfig config);

    /**
     * Remove the flow specified by name on the passed network node
     *
     * @param name for the static flow
     * @param node on which the flow is attached
     * @return the {@code Status} object indicating the result of this action
     */
    public Status removeStaticFlow(String name, Node node);

    /**
     * Toggle the installation status of the specified configured flow
     * If the flow configuration status is active, this call will
     * change the flow status to inactive and vice-versa
     *
     * @param configObject the {@code FlowConfig} object
     * @return the {@code Status} object indicating the result of this action
     */
    public Status toggleStaticFlowStatus(FlowConfig configObject);
    
    /**
     * Toggle the installation status of the specified configured flow
     * If the flow configuration status is active, this call will
     * change the flow status to inactive and vice-versa
     *
     * @param name for the static flow
     * @param node on which the flow is attached
     * @return the {@code Status} object indicating the result of this action
     */
    public Status toggleStaticFlowStatus(String name, Node node);

    public Map<String, PortGroupConfig> getPortGroupConfigs();

    public boolean addPortGroupConfig(String name, String regex, boolean load);

    public boolean delPortGroupConfig(String name);

    public PortGroupProvider getPortGroupProvider();

}
