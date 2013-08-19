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
 */
public interface IForwardingRulesManager {

    /**
     * It requests FRM to install the passed Flow Entry. FRM will request the
     * SDN protocol plugin to install the flow on the network node. Based on the
     * result of this operation FRM will update its database accordingly and
     * will return the proper {@code Status} code.
     *
     * @param flow
     *            the flow entry to install
     * @return the {@code Status} object indicating the result of this action.
     */
    public Status installFlowEntry(FlowEntry flow);

    /**
     * It requests FRM to remove the passed Flow Entry. FRM will request the SDN
     * protocol plugin to uninstall the flow from the network node. Based on the
     * result of this operation FRM will update its database accordingly and
     * will return the proper {@code Status} code.
     *
     * @param flow
     *            the flow entry to uninstall
     * @return the {@code Status} object indicating the result of this action
     */
    public Status uninstallFlowEntry(FlowEntry flow);

    /**
     * It requests FRM to remove all the Flow Entry that are part of the
     * specified group. FRM will request the SDN protocol plugin to uninstall
     * the flows from the network node one by one. Based on the result of this
     * operation FRM will update its database accordingly and will return the
     * proper {@code Status} code.
     *
     * @param groupName
     *            the group name
     * @return the {@code Status} object indicating the result of this action
     */
    public Status uninstallFlowEntryGroup(String groupName);

    /**
     * It requests FRM to replace the currently installed Flow Entry with the
     * new one. It is up to the SDN protocol plugin to decide how to convey this
     * message to the network node. It could be a delete + add or a single
     * modify message depending on the SDN protocol specifications If the
     * current flow is equal to the new one it will be a no op and success code
     * is returned.
     *
     * @param current
     *            the current flow entry to modify
     * @param newone
     *            the new flow entry which will replace the current one
     * @return the {@code Status} object indicating the result of this action
     */
    public Status modifyFlowEntry(FlowEntry current, FlowEntry newone);

    /**
     * It requests the FRM to replace the currently installed Flow Entry with
     * the new one. The currently installed entry is derived by the Match
     * portion of the passed Flow. FRM looks in its database for a previously
     * installed FlowEntry which Match equals the Match of the passed Flow. If
     * it finds it, it will request the SDN protocol plugin to replace the
     * existing flow with the new one on the network node. If it does not find
     * it, it will request plugin to add the new flow. If the passed entry is
     * not valid an error code is returned. If the existing flow is equal to the
     * passed one it will be a no op and success code is returned.
     *
     * @param newone
     *            the new flow entry to install
     * @return the {@code Status} object indicating the result of this action
     */
    public Status modifyOrAddFlowEntry(FlowEntry newone);

    /**
     * It requests FRM to install the passed Flow Entry through an asynchronous
     * call. A unique request id is returned to the caller. FRM will request the
     * SDN protocol plugin to install the flow on the network node. As immediate
     * result of this asynchronous call, FRM will update its flow database as if
     * the flow was successfully installed.
     *
     * @param flow
     *            the flow entry to install
     * @return the status of this request containing the request id associated
     *         to this asynchronous request
     */
    public Status installFlowEntryAsync(FlowEntry flow);

    /**
     * It requests FRM to remove the passed Flow Entry through an asynchronous
     * call. A unique request id is returned to the caller. FRM will request the
     * SDN protocol plugin to uninstall the flow from the network node. As
     * immediate result of this asynchronous call, FRM will update its flow
     * database as if the flow was successfully removed.
     *
     * @param flow
     *            the flow entry to uninstall
     * @return the status of this request containing the unique id associated to
     *         this asynchronous request
     */
    public Status uninstallFlowEntryAsync(FlowEntry flow);

    /**
     * It requests FRM to remove all the Flow Entry that are part of the
     * specified group through an asynchronous call. FRM will request the SDN
     * protocol plugin to uninstall the flows from the network node one by one.
     * As immediate result of this asynchronous call, FRM will update its flow
     * database as if the flow was successfully removed.
     *
     * @param groupName
     *            the group name
     * @return the {@code Status} object indicating the result of this action
     */
    public Status uninstallFlowEntryGroupAsync(String groupName);

    /**
     * It requests FRM to replace the currently installed Flow Entry with the
     * new one through an asynchronous call. A unique request id is returned to
     * the caller. It is up to the SDN protocol plugin to decide how to convey
     * this message to the network node. It could be a delete + add or a single
     * modify message depending on the SDN protocol specifications. If the
     * current flow is equal to the new one it will be a no op.
     *
     * @param current
     *            the current flow entry to modify
     * @param newone
     *            the new flow entry which will replace the current one
     * @return the status of this request containing the request id associated
     *         to this asynchronous request
     */
    public Status modifyFlowEntryAsync(FlowEntry current, FlowEntry newone);

    /**
     * It requests the FRM to replace the currently installed Flow Entry with
     * the new one through an asynchronous call. A unique request id is returned
     * to the caller. The currently installed entry is derived by the Match
     * portion of the passed Flow. FRM looks in its database for a previously
     * installed FlowEntry which Match equals the Match of the passed Flow. If
     * it finds it, it will request the SDN protocol plugin to replace the
     * existing flow with the new one on the network node. If it does not find
     * it, it will request plugin to add the new flow. If the passed entry is
     * not valid a zero request id is returned. If the existing flow is equal to
     * the passed one it will be a no op.
     *
     * @param newone
     *            the new flow entry to install
     * @return the unique id associated to this request. In case of not
     *         acceptable request -1 will be returned.
     */
    public Status modifyOrAddFlowEntryAsync(FlowEntry newone);

    /**
     * Requests ForwardingRulesManager to solicit the network node to inform us
     * about the status of its execution on the asynchronous requests that were
     * sent to it so far. It is a way for an application to poke the network
     * node in order to get a feedback asap on the asynchronous requests
     * generated by the application. The caller may decide if this is a blocking
     * or non-blocking operation. If blocking is set to true, the caller will be
     * blocked until the solicitation response is received from the network node
     * or receive timeout. Otherwise, it is a non-blocking call and does not
     * guarantee the node will respond in any given time.
     *
     * @param node
     *            The network node to solicit a response
     * @param blocking
     *            The blocking mode
     * @return the status of this request containing the request id associated
     *         to this asynchronous request
     */
    public Status solicitStatusResponse(Node node, boolean blocking);

    /**
     * Check whether the passed flow entry conflicts with the Container flows
     *
     * @param flow
     *            the flow entry to test
     * @return true if conflicts, false otherwise
     */
    public boolean checkFlowEntryConflict(FlowEntry flow);

    /**
     * Returns the list of Flow entries across network nodes which are part of
     * the same flow group, policy. This list contains the flows as they were
     * requested to be installed by the applications, before any merging with
     * container flow is done.
     *
     * @param group
     *            the group name
     * @return the original list of flow entries belonging to the specified group
     */
    public List<FlowEntry> getFlowEntriesForGroup(String group);

    /**
     * Returns the list of Flow entries installed in network nodes which are part of
     * the same flow group, policy. This list contains the effective flows installed
     * on the nodes after the merging with any possible container flow was performed.
     * If no container flow are specified, this method returns the same list returned
     * by getFlowEntriesForGroup(String group).
     *
     * @param group
     *            the group name
     * @return the list of container flow merged flow entries belonging to the specified group
     */
    public List<FlowEntry> getInstalledFlowEntriesForGroup(String policyName);

    /**
     * Add a list of output port to the flow with the specified name on the
     * specified network node
     *
     * @param node
     *            the network node
     * @param flowName
     *            the flow name
     * @param dstPort
     *            the list of ports to be added to the flow output actions
     */
    public void addOutputPort(Node node, String flowName, List<NodeConnector> dstPort);

    /**
     * Remove a list of output port from the flow with the specified name on the
     * specified network node
     *
     * @param node
     *            the network node
     * @param flowName
     *            the flow name
     * @param dstPortthe
     *            list of ports to be removed from the flow output actions
     */
    public void removeOutputPort(Node node, String flowName, List<NodeConnector> dstPort);

    /**
     * Replace the current output port in the specified flow with the specified
     * one
     *
     * @param node
     *            the network node
     * @param groupName
     *            the group name
     * @param flowName
     *            the flow name
     * @param dstPort
     *            the new output action port
     */
    public void replaceOutputPort(Node node, String flowName, NodeConnector outPort);

    /**
     * Returns the output port configured on the specified flow
     *
     * @param node
     *            the network node
     * @param flowName
     *            the flow name
     * @return the output action port for the specified flow
     */
    public NodeConnector getOutputPort(Node node, String flowName);

    /**
     * Returns all the troubleshooting information that applications have set
     * along with the policy they have configured through forwarding rules
     * manger.
     *
     * @return the collection of troubleshooting objects
     */
    public Map<String, Object> getTSPolicyData();

    /**
     * Set the troubleshooting information for the policy
     *
     * @param policyname
     *            the flow group name
     * @param o
     *            the object containing the troubleshooting information
     * @param add
     *            true for adding, false for removing
     */
    public void setTSPolicyData(String policyName, Object o, boolean add);

    /**
     * Returns the troubleshooting information that was set for the specified
     * policy
     *
     * @param groupName
     *            the flows group name
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
     * Returns the specifications of all the flows configured for the given
     * switch on the current container
     *
     * @param node
     *            the network node identifier
     * @return the list of {@code FlowConfig} objects
     */
    public List<FlowConfig> getStaticFlows(Node node);

    /**
     * Returns the specification of the flow configured for the given network
     * node on the current container
     *
     * @param name
     *            the flow name
     * @param n
     *            the network node identifier
     * @return the {@code FlowConfig} object
     */
    public FlowConfig getStaticFlow(String name, Node n);

    /**
     * Returns the list of names of flows configured for the given Network node
     * on the current container
     *
     * @param node
     *            the network node identifier
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
     * Add a flow specified by the {@code FlowConfig} object on the current
     * container
     *
     * @param config
     *            the {@code FlowConfig} object representing the static flow
     * @return the {@code Status} object indicating the result of this action.
     */
    public Status addStaticFlow(FlowConfig config);

    /**
     * Remove a flow specified by the {@code FlowConfig} object on the current
     * container
     *
     * @param config
     *            the {@code FlowConfig} object representing the static flow
     * @return the {@code Status} object indicating the result of this action
     */
    public Status removeStaticFlow(FlowConfig config);

    /**
     * Replace the flow identified by the {@code FlowConfig.name} name for the
     * {@code FlowConfig.node} network node with the new flow specified by
     * {@code FlowConfig} object
     *
     * @param config
     *            the {@code FlowConfig} object
     * @returnthe {@code Status} object indicating the result of this action
     */
    public Status modifyStaticFlow(FlowConfig config);

    /**
     * Remove the flow specified by name on the passed network node
     *
     * @param name
     *            for the static flow
     * @param node
     *            on which the flow is attached
     * @return the {@code Status} object indicating the result of this action
     */
    public Status removeStaticFlow(String name, Node node);

    /**
     * Toggle the installation status of the specified configured flow If the
     * flow configuration status is active, this call will change the flow
     * status to inactive and vice-versa
     *
     * @param configObject
     *            the {@code FlowConfig} object
     * @return the {@code Status} object indicating the result of this action
     */
    public Status toggleStaticFlowStatus(FlowConfig configObject);

    /**
     * Toggle the installation status of the specified configured flow If the
     * flow configuration status is active, this call will change the flow
     * status to inactive and vice-versa
     *
     * @param name
     *            for the static flow
     * @param node
     *            on which the flow is attached
     * @return the {@code Status} object indicating the result of this action
     */
    public Status toggleStaticFlowStatus(String name, Node node);

    public Map<String, PortGroupConfig> getPortGroupConfigs();

    public boolean addPortGroupConfig(String name, String regex, boolean load);

    public boolean delPortGroupConfig(String name);

    public PortGroupProvider getPortGroupProvider();

}
