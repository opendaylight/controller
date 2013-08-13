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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.service.command.Descriptor;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.Config;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.MacAddress;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.Tier;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.HexEncode;
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

        System.out.println("           Node               Type           MAC            Name      Tier");

        Set<Node> nodes = sm.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        List<String> nodeArray = new ArrayList<String>();
        for (Node node : nodes) {
            nodeArray.add(node.toString());
        }
        Collections.sort(nodeArray);
        for (String str : nodeArray) {
            Node node = Node.fromString(str);
            Description desc = ((Description) sm.getNodeProp(node, Description.propertyName));
            Tier tier = ((Tier) sm.getNodeProp(node, Tier.TierPropName));
            String nodeName = (desc == null) ? "" : desc.getValue();
            MacAddress mac = (MacAddress) sm.getNodeProp(node, MacAddress.name);
            String macAddr = (mac == null) ? "" : HexEncode.bytesToHexStringFormat(mac.getMacAddress());
            int tierNum = (tier == null) ? 0 : tier.getValue();
            System.out.println(node + "     " + node.getType() + "     " + macAddr + "     " + nodeName + "     "
                    + tierNum);
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

        System.out.println("          NodeConnector               BandWidth(Gbps)     Admin     State");
        Set<NodeConnector> nodeConnectorSet = sm.getNodeConnectors(target);
        if (nodeConnectorSet == null) {
            return;
        }
        for (NodeConnector nodeConnector : nodeConnectorSet) {
            if (nodeConnector == null) {
                continue;
            }
            Map<String, Property> propMap = sm.getNodeConnectorProps(nodeConnector);
            Bandwidth bw = (Bandwidth) propMap.get(Bandwidth.BandwidthPropName);
            Config config = (Config) propMap.get(Config.ConfigPropName);
            State state = (State) propMap.get(State.StatePropName);
            String out = nodeConnector + "           ";
            out += (bw != null) ? bw.getValue() / Math.pow(10, 9) : "    ";
            out += "             ";
            out += (config != null) ? config.getValue() : " ";
            out += "          ";
            out += (state != null) ? state.getValue() : " ";
            System.out.println(out);
        }
        System.out.println("Total number of NodeConnectors: " + nodeConnectorSet.size());
    }
}
