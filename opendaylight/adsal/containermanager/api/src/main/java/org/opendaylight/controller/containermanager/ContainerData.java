
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.containermanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.ContainerFlow;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;


/**
 * This Class contains all the configurations pertained to this container
 *
 */
public class ContainerData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String containerAdminRole;
    private String containerOperatorRole;
    private String name;
    private ConcurrentMap<Node, Set<NodeConnector>> swPorts;
    private short staticVlan;
    private List<ContainerFlow> cFlowList;

    /**
     * Build a ContainerData from container configuration
     *
     * @param conf configuration from where the ContainerData need to be extracted
     *
     * @return the constructed containerData
     */
    public ContainerData(ContainerConfig conf) {
        /*
         * Back-end Non-Configuration Classes store names in lower case
         */
        name = conf.getContainerName().toLowerCase(Locale.ENGLISH);
        swPorts = new ConcurrentHashMap<Node, Set<NodeConnector>>();
        cFlowList = new ArrayList<ContainerFlow>();
        staticVlan = conf.getStaticVlanValue();
        containerAdminRole = conf.getContainerAdminRole();
        containerOperatorRole = conf.getContainerOperatorRole();
    }

    /**
     * getter for containerName
     *
     *
     * @return the container name
     */
    public String getContainerName() {
        return name;
    }

    /**
     * getter for static vlan
     *
     *
     * @return attribute static vlan
     */
    public short getStaticVlan() {
        return staticVlan;
    }

    /**
     * Check if the static vlan has non-zero value in that case is
     * assumed to be assigned
     *
     *
     * @return true if static vlan non-zero
     */
    public boolean hasStaticVlanAssigned() {
        return (staticVlan != 0);
    }

    /**
     * retrieve all NodeConnectors on a given switch on the containerName
     * used in this object
     *
     * @param switchId the Node for switch we want to retrieve the list of ports
     *
     * @return get all the ports in form of a set of nodeconnectors
     */
    public Set<NodeConnector> getPorts(Node switchId) {
        Set<NodeConnector> swpinfo = swPorts.get(switchId);
        if (swpinfo != null) {
            return swpinfo;
        }
        return null;
    }

    /**
     * Returns the DB of all the association Node -> List of
     * NodeConnectors composing this container
     *
     * @return the ConcurrentMap of all the association Node -> List
     * of NodeConnectors
     */
    public ConcurrentMap<Node, Set<NodeConnector>> getSwPorts() {
        return swPorts;
    }

    /**
     * Add to the container a NodeConnector, that implicitely means also
     * the Node being added
     *
     * @param port NodeConnector corresponding to the port being added
     * in the container
     */
    public void addPortToSwitch(NodeConnector port) {
        Node switchId = port.getNode();
        if (switchId == null) {
            return;
        }
        Set<NodeConnector> portSet = swPorts.get(switchId);
        if (portSet == null) {
            portSet = new HashSet<NodeConnector>();
            swPorts.put(switchId, portSet);
        }
        portSet.add(port);
    }

    /**
     * Remove from the container a NodeConnector, that implicitely means also
     * the Node being removed
     *
     * @param port NodeConnector corresponding to the port being removed
     * in the container
     */
    public void removePortFromSwitch(NodeConnector port) {
        Node switchId = port.getNode();
        if (switchId == null) {
            return;
        }
        Set<NodeConnector> swpinfo = swPorts.get(switchId);
        if (swpinfo != null) {
            swpinfo.remove(port);
            if (swpinfo.isEmpty()) {
                // There are no more ports lets get ride of the switch
                swPorts.remove(switchId);
            }
        }
    }

    /**
     * Existence check of a Node in a container
     *
     * @param switchId See if a particular node is part of the container
     *
     * @return true if the node is in it else false
     */
    public boolean isSwitchInContainer(Node switchId) {
        if (swPorts.containsKey(switchId)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test for existance of NodeConnectors on a Node in a container
     *
     * @param switchId Node we are testing for port existance
     *
     * @return ture if the portset is non-empty
     */
    public boolean portListEmpty(Node switchId) {
        Set<NodeConnector> swpinfo = swPorts.get(switchId);
        if (swpinfo != null) {
            if (!swpinfo.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void deleteFlowSpec(ContainerFlow cFlow) {
        cFlowList.remove(cFlow);
    }

    public void addFlowSpec(ContainerFlow cFlow) {
        cFlowList.add(cFlow);
    }

    public boolean isFlowSpecEmpty() {
        return (cFlowList.isEmpty());

    }

    public boolean containsFlowSpec(ContainerFlow cFlow) {
        return cFlowList.contains(cFlow);
    }

    public int getFlowSpecCount() {
        return cFlowList.size();
    }

    public List<ContainerFlow> getContainerFlowSpecs() {
        return cFlowList;
    }

    public boolean matchName(String name) {
        return this.name.equalsIgnoreCase(name);
    }

    /**
     * Returns all the node connectors part of the container
     * @return The node connectors belonging to this container. The returning set is never null.
     */
    public Set<NodeConnector> getNodeConnectors() {
        Set<NodeConnector> set = new HashSet<NodeConnector>();
        for (Map.Entry<Node, Set<NodeConnector>> entry : swPorts.entrySet()) {
            set.addAll(entry.getValue());
        }
        return set;
    }

    public String getContainerAdminRole() {
        return containerAdminRole;
    }

    public String getContainerOperatorRole() {
        return containerOperatorRole;
    }
}
