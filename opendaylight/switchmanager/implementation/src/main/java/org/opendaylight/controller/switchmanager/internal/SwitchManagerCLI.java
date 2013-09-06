/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.service.command.Descriptor;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.osgi.framework.ServiceRegistration;

/**
 * This class provides osgi cli commands for developers to debug Switch Manager
 * functionality
 */
public class SwitchManagerCLI {
    @SuppressWarnings("rawtypes")
    private ServiceRegistration sr = null;

    public void init() {
    }

    public void destroy() {
    }

    public void start() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.command.scope", "odpcontroller");
        props.put("osgi.command.function", new String[] { "showNodes", "showNodeConnectors" });
        this.sr = ServiceHelper.registerGlobalServiceWReg(SwitchManagerCLI.class, this, props);
    }

    public void stop() {
        if (this.sr != null) {
            this.sr.unregister();
            this.sr = null;
        }
    }

    @Descriptor("Retrieves the nodes information present in Switch Manager DB")
    public void showNodes(
            @Descriptor("Container in which to query Switch Manager") String container) {
        final ISwitchManager sm = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, container, this);

        if (sm == null) {
            System.out.println("Cannot find the switch manager instance on container: " + container);
            return;
        }

        Set<Node> nodes = sm.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        Set<String> propertyList = new HashSet<String>();
        for (Node node : nodes) {
            Map<String, Property> propList = sm.getNodeProps(node);
            propertyList.addAll(propList.keySet());
        }
        List<String> sortedProps = new ArrayList<String>(propertyList);
        Collections.sort(sortedProps);
        String properties = String.format("%-26s  ", "Node");
        for (String s : sortedProps) {
            properties = properties.concat(String.format("%-18s ", s));
        }
        System.out.println(properties);
        for (Node node : nodes) {
            String nodeProp = String.format("%-26s  ", node);
            Map<String, Property> propList = sm.getNodeProps(node);
            for (String s : sortedProps) {
                if (propList.containsKey(s)) {
                    nodeProp = nodeProp.concat(String.format("%-18s ", propList.get(s).getStringValue()));
                } else {
                    nodeProp = nodeProp.concat(String.format("%-18s ", "null"));
                }
            }
            System.out.println(nodeProp);
        }
        System.out.println("Total number of Nodes: " + nodes.size());
    }

    @Descriptor("Retrieves the node connectors information present in Switch Manager DB for the specified node")
    public void showNodeConnectors(@Descriptor("Container in which to query Switch Manager") String container,
            @Descriptor("String representation of the Node, this need to be consumable from Node.fromString()") String node) {
        final String containerName = (container == null) ? GlobalConstants.DEFAULT.toString() : container;
        final ISwitchManager sm = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, containerName, this);

        if (sm == null) {
            System.out.println("Cannot find the switch manager instance on container: " + containerName);
            return;
        }

        Node target = Node.fromString(node);
        if (target == null) {
            System.out.println("Please enter a valid node id");
            return;
        }

        Set<NodeConnector> nodeConnectorSet = sm.getNodeConnectors(target);
        if (nodeConnectorSet == null || nodeConnectorSet.isEmpty()) {
            return;
        }

        Set<String> propertyList = new HashSet<String>();
        for (NodeConnector nodeConnector : nodeConnectorSet) {
            Map<String, Property> propList = sm.getNodeConnectorProps(nodeConnector);
            propertyList.addAll(propList.keySet());
        }
        List<String> sortedProps = new ArrayList<String>(propertyList);
        Collections.sort(sortedProps);
        String properties = String.format("%-33s  ", "NodeConnector");
        for (String s : sortedProps) {
            properties = properties.concat(String.format("%-18s ", s));
        }
        System.out.println(properties);
        for (NodeConnector nodeConnector : nodeConnectorSet) {
            String ncProp = String.format("%-33s  ", nodeConnector);
            Map<String, Property> ncProperties = sm.getNodeConnectorProps(nodeConnector);
            for (String s : sortedProps) {
                if (ncProperties.containsKey(s)) {
                    ncProp = ncProp.concat(String.format("%-18s ", ncProperties.get(s).getStringValue()));
                } else {
                    ncProp = ncProp.concat(String.format("%-18s ", "null"));
                }
            }
            System.out.println(ncProp);
        }
        System.out.println("Total number of NodeConnectors: " + nodeConnectorSet.size());
    }
}
