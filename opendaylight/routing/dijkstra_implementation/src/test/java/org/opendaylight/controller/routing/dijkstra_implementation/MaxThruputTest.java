
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.routing.dijkstra_implementation;

import org.junit.Test;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.opendaylight.controller.routing.dijkstra_implementation.internal.DijkstraImplementation;

import org.opendaylight.controller.sal.core.UpdateType;

public class MaxThruputTest {

    Map<Edge, Number> LinkCostMap = new HashMap<Edge, Number>();

    @Test
    public void testMaxThruPut() {
        DijkstraImplementation imp = new DijkstraImplementation();
        imp.init();
        Node node1 = NodeCreator.createOFNode((long) 1);
        Node node2 = NodeCreator.createOFNode((long) 2);
        Node node3 = NodeCreator.createOFNode((long) 3);
        Node node4 = NodeCreator.createOFNode((long) 4);
        Node node5 = NodeCreator.createOFNode((long) 5);
        Node node6 = NodeCreator.createOFNode((long) 6);
        NodeConnector nc11 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node1);
        NodeConnector nc21 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node2);
        NodeConnector nc31 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node3);
        NodeConnector nc41 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node4);
        NodeConnector nc51 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node5);
        NodeConnector nc61 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node6);
        NodeConnector nc12 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node1);
        NodeConnector nc22 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node2);
        NodeConnector nc32 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node3);
        NodeConnector nc42 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node4);
        NodeConnector nc52 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node5);
        NodeConnector nc62 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node6);

        Edge edge1 = null;
        try {
            edge1 = new Edge(nc11, nc21);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge1, 10);
        Edge edge2 = null;
        try {
            edge2 = new Edge(nc21, nc11);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge2, 10);

        Edge edge3 = null;
        try {
            edge3 = new Edge(nc22, nc31);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge3, 30);
        Edge edge4 = null;
        try {
            edge4 = new Edge(nc31, nc22);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge4, 30);

        Edge edge5 = null;
        try {
            edge5 = new Edge(nc32, nc41);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge5, 10);
        Edge edge6 = null;
        try {
            edge6 = new Edge(nc41, nc32);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge6, 10);

        Edge edge7 = null;
        try {
            edge7 = new Edge(nc12, nc51);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge7, 20);
        Edge edge8 = null;
        try {
            edge8 = new Edge(nc51, nc12);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge8, 20);

        Edge edge9 = null;
        try {
            edge9 = new Edge(nc52, nc61);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge9, 20);
        Edge edge10 = null;
        try {
            edge10 = new Edge(nc61, nc52);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge10, 20);

        Edge edge11 = null;
        try {
            edge11 = new Edge(nc62, nc42);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge11, 20);
        Edge edge12 = null;
        try {
            edge12 = new Edge(nc42, nc62);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        LinkCostMap.put(edge12, 20);

        imp.edgeUpdate(edge1, UpdateType.ADDED, null);
        imp.edgeUpdate(edge2, UpdateType.ADDED, null);
        imp.edgeUpdate(edge3, UpdateType.ADDED, null);
        imp.edgeUpdate(edge4, UpdateType.ADDED, null);
        imp.edgeUpdate(edge5, UpdateType.ADDED, null);
        imp.edgeUpdate(edge6, UpdateType.ADDED, null);
        imp.edgeUpdate(edge7, UpdateType.ADDED, null);
        imp.edgeUpdate(edge8, UpdateType.ADDED, null);
        imp.edgeUpdate(edge9, UpdateType.ADDED, null);
        imp.edgeUpdate(edge10, UpdateType.ADDED, null);
        imp.edgeUpdate(edge11, UpdateType.ADDED, null);
        imp.edgeUpdate(edge12, UpdateType.ADDED, null);

        imp.initMaxThroughput(LinkCostMap);

        Path res = imp.getMaxThroughputRoute(node1, node3);
        System.out.println("Max Thruput Path between n1-n3: " + res);

        List<Edge> expectedPath = (List<Edge>) new LinkedList<Edge>();
        expectedPath.add(0, edge7);
        expectedPath.add(1, edge9);
        expectedPath.add(2, edge11);
        expectedPath.add(3, edge6);

        Path expectedRes = null;
        try {
            expectedRes = new Path(expectedPath);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        if (!res.equals(expectedRes)) {
            System.out.println("Actual Res is " + res);
            System.out.println("Expected Res is " + expectedRes);
        }
        Assert.assertTrue(res.equals(expectedRes));

        res = imp.getRoute(node1, node3);
        System.out.println("Shortest Path between n1-n3: " + res);
        expectedPath.clear();
        expectedPath.add(0, edge1);
        expectedPath.add(1, edge3);

        expectedRes = null;
        try {
            expectedRes = new Path(expectedPath);
        } catch (ConstructionException e) {
            e.printStackTrace();
        }
        if (!res.equals(expectedRes)) {
            System.out.println("Actual Res is " + res);
            System.out.println("Expected Res is " + expectedRes);
        }
        Assert.assertTrue(res.equals(expectedRes));
    }
}
