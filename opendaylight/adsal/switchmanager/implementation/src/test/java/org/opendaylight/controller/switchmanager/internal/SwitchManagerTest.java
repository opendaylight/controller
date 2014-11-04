/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Latency;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.SubnetConfig;

public class SwitchManagerTest {

    @Test
    public void testSwitchManagerAddRemoveSubnet() throws ConstructionException {
        SwitchManager switchmgr = new SwitchManager();
        switchmgr.startUp();

        // Create the node connector string list
        Node node1 = new Node(Node.NodeIDType.OPENFLOW, 1L);
        Node node2 = new Node(Node.NodeIDType.OPENFLOW, 2L);
        NodeConnector nc1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)1, node1);
        NodeConnector nc2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)2, node2);
        NodeConnector nc3 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)3, node1);
        List<String> portList = new ArrayList<String>();
        portList.add(nc1.toString());
        portList.add(nc2.toString());
        portList.add(nc3.toString());


        SubnetConfig subnet = new SubnetConfig("subnet", "10.0.0.254/16", portList);
        Status addResult = (switchmgr.addSubnet(subnet));
        Assert.assertTrue(addResult.isSuccess());

        Status removeResult = (switchmgr.removeSubnet(subnet.getName()));
        Assert.assertTrue(removeResult.isSuccess());

        SubnetConfig subnetConfigResult = switchmgr.getSubnetConfig(subnet.getName());
        Assert.assertNull(subnetConfigResult);

        subnet = new SubnetConfig("hr", "0.0.0.0", portList);
        Status status = switchmgr.addSubnet(subnet);
        Assert.assertFalse(status.isSuccess());

        subnet = new SubnetConfig("hr", "12.12.12.254/16", null);
        status = switchmgr.addSubnet(subnet);
        Assert.assertTrue(status.isSuccess());

    }

    @Test
    public void testSwitchManagerAddRemovePortsToSubnet() {
        SwitchManager switchmgr = new SwitchManager();
        switchmgr.startUp();

        List<String> portList = new ArrayList<String>();
        portList.add("OF|1@OF|1");
        portList.add("OF|2@OF|00:00:00:00:00:00:00:02");
        portList.add("OF|3@OF|00:00:00:00:00:00:00:01");

        SubnetConfig subnet = new SubnetConfig("eng", "11.1.1.254/16", portList);
        Status status = (switchmgr.addSubnet(subnet));
        Assert.assertTrue(status.isSuccess());


        // Empty port set
        List<String> badPortSet = new ArrayList<String>();
        status = switchmgr.addPortsToSubnet("eng", badPortSet);
        Assert.assertFalse(status.isSuccess());

        // Non existant subnet
        status = switchmgr.removePortsFromSubnet("hr", badPortSet);
        Assert.assertFalse(status.isSuccess());

        // Port set containing non conventional but parsable port
        badPortSet.add("1/1");
        status = switchmgr.addPortsToSubnet("eng", badPortSet);
        Assert.assertTrue(status.isSuccess());

        // Port set containing non parsable port
        badPortSet.add("OF1/1");
        status = switchmgr.addPortsToSubnet("eng", badPortSet);
        Assert.assertTrue(status.isSuccess());
    }

    @Test
    public void testSwitchManagerNodeConnectors() {
        SwitchManager switchmgr = new SwitchManager();
        switchmgr.startUp();

        State state;
        Bandwidth bw;
        Latency l;

        NodeConnector[] headnc = new NodeConnector[5];
        NodeConnector[] tailnc = new NodeConnector[5];

        Set<Property> props = new HashSet<Property>();
        state = new State(State.EDGE_UP);
        bw = new Bandwidth(Bandwidth.BW100Gbps);
        l = new Latency(Latency.LATENCY100ns);
        props.add(state);
        props.add(bw);
        props.add(l);

        for (short i = 1; i < 6; i = (short) (i + 1)) {

            headnc[i - 1] = NodeConnectorCreator.createOFNodeConnector(i,
                    NodeCreator.createOFNode((long) i));
            tailnc[i - 1] = NodeConnectorCreator
                    .createOFNodeConnector((short) (i + 10),
                            NodeCreator.createOFNode((long) (i + 10)));
            switchmgr.updateNode(headnc[i - 1].getNode(), UpdateType.ADDED,
                    props);
            switchmgr.updateNode(tailnc[i - 1].getNode(), UpdateType.ADDED,
                    props);

            Assert.assertFalse(switchmgr.doesNodeConnectorExist(headnc[i - 1]));
            switchmgr.updateNodeConnector(headnc[i - 1], UpdateType.ADDED,
                    props);
            Assert.assertTrue(switchmgr.doesNodeConnectorExist(headnc[i - 1]));

            Assert.assertFalse(switchmgr.doesNodeConnectorExist(tailnc[i - 1]));
            switchmgr.updateNodeConnector(tailnc[i - 1], UpdateType.ADDED,
                    props);
            Assert.assertTrue(switchmgr.doesNodeConnectorExist(tailnc[i - 1]));
        }

        for (int i = 0; i < 5; i++) {
            Property bwProp = switchmgr.getNodeConnectorProp(headnc[i],
                    Bandwidth.BandwidthPropName);
            Assert.assertTrue(bwProp.equals(bw));
            Property latencyProp = switchmgr.getNodeConnectorProp(tailnc[i],
                    Latency.LatencyPropName);
            Assert.assertEquals(latencyProp, l);
        }

        Set<Node> nodes = switchmgr.getNodes();
        for (int i = 0; i < 5; i++) {
            if (nodes.contains(headnc[i].getNode()) == true) {
                nodes.remove(headnc[i].getNode());
            }

            if (nodes.contains(tailnc[i].getNode()) == true) {
                nodes.remove(tailnc[i].getNode());
            }

        }
        Assert.assertTrue(nodes.isEmpty());
    }

}
