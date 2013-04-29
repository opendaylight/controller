/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;

public class SwitchTest {

    @Test
    public void testSwitchCreation() {

        Node node = NodeCreator.createOFNode(((long) 10));
        Node node2 = NodeCreator.createOFNode(((long) 11));
        NodeConnector nc0 = NodeConnectorCreator.createOFNodeConnector(
                (short) 20, node);
        NodeConnector nc1 = NodeConnectorCreator.createOFNodeConnector(
                (short) 30, node);
        NodeConnector nc2 = NodeConnectorCreator.createOFNodeConnector(
                (short) 40, node);
        NodeConnector nc3 = NodeConnectorCreator.createOFNodeConnector(
                (short) 50, node);
        NodeConnector nc4 = NodeConnectorCreator.createOFNodeConnector(
                (short) 60, node);
        NodeConnector nc5 = NodeConnectorCreator.createOFNodeConnector(
                (short) 70, node);

        Set<NodeConnector> ncSet = new HashSet<NodeConnector>();
        ArrayList<NodeConnector> portList = new ArrayList<NodeConnector>();

        Switch sw = new Switch(node);
        Switch sw2 = new Switch(node);
        Assert.assertTrue(sw.equals(sw2));
        sw2.setNode(node2);
        Assert.assertTrue(sw2.getNode().equals(node2));
        Assert.assertFalse(sw.equals(sw2));

        ncSet.add(nc0);
        ncSet.add(nc1);
        ncSet.add(nc2);
        sw.addNodeConnector(nc3);
        try {
            sw.addNodeConnector(nc3);
        } catch (Exception e) {
            fail("Attempted to add duplicate NodeConnector to set");
        }

        portList.add(nc4);
        portList.add(nc5);

        sw.setNodeConnectors(ncSet);
        sw.addSpanPorts(portList);

        sw.setDataLayerAddress(null);
        Assert.assertNull(sw.getDataLayerAddress());
        byte[] dlAddress = { (byte) 0x01, (byte) 0x02, (byte) 0x03,
                (byte) 0x04, (byte) 0x05, (byte) 0x06 };
        sw.setDataLayerAddress(dlAddress);

        Node resultNode = sw.getNode();
        Set<NodeConnector> resultncSet = sw.getNodeConnectors();
        byte[] resultdlAddress = sw.getDataLayerAddress();
        ArrayList<NodeConnector> resultSpanPort = (ArrayList<NodeConnector>) sw
                .getSpanPorts();

        Assert.assertEquals(node, resultNode);
        for (int i = 0; i < dlAddress.length; i++)
            Assert.assertEquals(dlAddress[i], resultdlAddress[i]);

        Assert.assertTrue(ncSet.equals(resultncSet));

        for (int i = 0; i < portList.size(); i++)
            Assert.assertEquals(portList.get(i), resultSpanPort.get(i));
    }

    @Test
    public void testSwitchAddRemovePort() {
        Node node = NodeCreator.createOFNode(((long) 10));

        NodeConnector nc0 = NodeConnectorCreator.createOFNodeConnector(
                (short) 20, node);
        NodeConnector nc1 = NodeConnectorCreator.createOFNodeConnector(
                (short) 30, node);
        NodeConnector nc4 = NodeConnectorCreator.createOFNodeConnector(
                (short) 60, node);
        NodeConnector nc5 = NodeConnectorCreator.createOFNodeConnector(
                (short) 70, node);
        ArrayList<NodeConnector> portList = new ArrayList<NodeConnector>();

        portList.add(nc4);
        portList.add(nc5);

        Set<NodeConnector> ncSet = new HashSet<NodeConnector>();
        ncSet.add(nc0);
        ncSet.add(nc1);

        Switch sw = new Switch(node);
        sw.setNodeConnectors(ncSet);
        sw.removeNodeConnector(nc0);
        Assert.assertFalse(ncSet.contains(nc0));

        sw.removeSpanPorts(portList);
        Assert.assertTrue(sw.getSpanPorts().isEmpty());

    }
    
    @Test
    public void testSwitchConfig(){
        SwitchConfig sc = new SwitchConfig(null, null, null, null);
        SwitchConfig sc2 = new SwitchConfig(null, null, null, null);
        Assert.assertTrue(sc.equals(sc2));
        
        Assert.assertNull(sc.getMode());
        Assert.assertNull(sc.getNodeId());
        Assert.assertNull(sc.getTier());
        Assert.assertNull(sc.getNodeDescription());
        
        SwitchConfig sc3 = new SwitchConfig("123", "name", "tier", "mode");
        SwitchConfig sc4 = new SwitchConfig("123", "name", "tier", "mode");
        Assert.assertFalse(sc.equals(sc3));
        Assert.assertTrue(sc3.equals(sc4));
        
        Assert.assertTrue(sc3.getNodeId().equals("123"));
        Assert.assertTrue(sc3.getNodeDescription().equals("name"));
        Assert.assertTrue(sc3.getTier().equals("tier"));
        Assert.assertTrue(sc3.getMode().equals("mode"));
    }

}
