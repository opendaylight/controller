/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.routing.dijkstra_implementation;

import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.routing.dijkstra_implementation.internal.DijkstraImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DijkstraTest {
    protected static final Logger logger = LoggerFactory
            .getLogger(DijkstraTest.class);

    @Test
    public void testSinglePathRouteNoBw() {
        DijkstraImplementation imp = new DijkstraImplementation();
        imp.init();
        Node node1 = NodeCreator.createOFNode((long) 1);
        Node node2 = NodeCreator.createOFNode((long) 2);
        Node node3 = NodeCreator.createOFNode((long) 3);
        List<TopoEdgeUpdate> topoedgeupdateList = new ArrayList<TopoEdgeUpdate>();
        NodeConnector nc11 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node1);
        NodeConnector nc21 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node2);
        Edge edge1 = null;
        try {
            edge1 = new Edge(nc11, nc21);
        } catch (ConstructionException e) {
            logger.error("", e);
        }
        Set<Property> props = new HashSet<Property>();
        props.add(new Bandwidth(0));
        TopoEdgeUpdate teu1 = new TopoEdgeUpdate(edge1, props, UpdateType.ADDED);
        topoedgeupdateList.add(teu1);

        NodeConnector nc22 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node2);
        NodeConnector nc31 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node3);
        Edge edge2 = null;
        try {
            edge2 = new Edge(nc22, nc31);
        } catch (ConstructionException e) {
            logger.error("", e);
        }
        Set<Property> props2 = new HashSet<Property>();
        props2.add(new Bandwidth(0));
        TopoEdgeUpdate teu2 = new TopoEdgeUpdate(edge2, props2,
                UpdateType.ADDED);
        topoedgeupdateList.add(teu2);
        imp.edgeUpdate(topoedgeupdateList);
        Path res = imp.getRoute(node1, node3);

        List<Edge> expectedPath = (List<Edge>) new LinkedList<Edge>();
        expectedPath.add(0, edge1);
        expectedPath.add(1, edge2);
        Path expectedRes = null;
        try {
            expectedRes = new Path(expectedPath);
        } catch (ConstructionException e) {
            logger.error("", e);
        }
        if (!res.equals(expectedRes)) {
            System.out.println("Actual Res is " + res);
            System.out.println("Expected Res is " + expectedRes);
        }
        Assert.assertTrue(res.equals(expectedRes));
    }

    @Test
    public void testShortestPathRouteNoBw() {
        DijkstraImplementation imp = new DijkstraImplementation();
        List<TopoEdgeUpdate> topoedgeupdateList = new ArrayList<TopoEdgeUpdate>();
        imp.init();
        Node node1 = NodeCreator.createOFNode((long) 1);
        Node node2 = NodeCreator.createOFNode((long) 2);
        Node node3 = NodeCreator.createOFNode((long) 3);
        NodeConnector nc11 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node1);
        NodeConnector nc21 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node2);
        Edge edge1 = null;
        try {
            edge1 = new Edge(nc11, nc21);
        } catch (ConstructionException e) {
            logger.error("", e);
        }
        Set<Property> props = new HashSet<Property>();
        props.add(new Bandwidth(0));
        TopoEdgeUpdate teu1 = new TopoEdgeUpdate(edge1, props, UpdateType.ADDED);
        topoedgeupdateList.add(teu1);

        NodeConnector nc22 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node2);
        NodeConnector nc31 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node3);
        Edge edge2 = null;
        try {
            edge2 = new Edge(nc22, nc31);
        } catch (ConstructionException e) {
            logger.error("", e);
        }
        Set<Property> props2 = new HashSet<Property>();
        props2.add(new Bandwidth(0));
        TopoEdgeUpdate teu2 = new TopoEdgeUpdate(edge2, props2,
                UpdateType.ADDED);
        topoedgeupdateList.add(teu2);

        NodeConnector nc12 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node1);
        NodeConnector nc32 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node3);
        Edge edge3 = null;
        try {
            edge3 = new Edge(nc12, nc32);
        } catch (ConstructionException e) {
            logger.error("", e);
        }
        Set<Property> props3 = new HashSet<Property>();
        props3.add(new Bandwidth(0));
        TopoEdgeUpdate teu3 = new TopoEdgeUpdate(edge3, props3,
                UpdateType.ADDED);
        topoedgeupdateList.add(teu3);
        imp.edgeUpdate(topoedgeupdateList);

        Path res = imp.getRoute(node1, node3);

        List<Edge> expectedPath = (List<Edge>) new LinkedList<Edge>();
        expectedPath.add(0, edge3);
        Path expectedRes = null;
        try {
            expectedRes = new Path(expectedPath);
        } catch (ConstructionException e) {
            logger.error("", e);
        }
        if (!res.equals(expectedRes)) {
            System.out.println("Actual Res is " + res);
            System.out.println("Expected Res is " + expectedRes);
        }
        Assert.assertTrue(res.equals(expectedRes));
    }

    @Test
    public void testShortestPathRouteNoBwAfterLinkDelete() {
        DijkstraImplementation imp = new DijkstraImplementation();
        imp.init();
        List<TopoEdgeUpdate> topoedgeupdateList = new ArrayList<TopoEdgeUpdate>();
        Node node1 = NodeCreator.createOFNode((long) 1);
        Node node2 = NodeCreator.createOFNode((long) 2);
        Node node3 = NodeCreator.createOFNode((long) 3);
        NodeConnector nc11 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node1);
        NodeConnector nc21 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node2);
        Edge edge1 = null;
        try {
            edge1 = new Edge(nc11, nc21);
        } catch (ConstructionException e) {
            logger.error("", e);
        }
        Set<Property> props = new HashSet<Property>();
        props.add(new Bandwidth(0));
        TopoEdgeUpdate teu1 = new TopoEdgeUpdate(edge1, props, UpdateType.ADDED);
        topoedgeupdateList.add(teu1);

        NodeConnector nc22 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node2);
        NodeConnector nc31 = NodeConnectorCreator.createOFNodeConnector(
                (short) 1, node3);
        Edge edge2 = null;
        try {
            edge2 = new Edge(nc22, nc31);
        } catch (ConstructionException e) {
            logger.error("", e);
        }
        Set<Property> props2 = new HashSet<Property>();
        props2.add(new Bandwidth(0));
        TopoEdgeUpdate teu2 = new TopoEdgeUpdate(edge2, props2,
                UpdateType.ADDED);
        topoedgeupdateList.add(teu2);

        NodeConnector nc12 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node1);
        NodeConnector nc32 = NodeConnectorCreator.createOFNodeConnector(
                (short) 2, node3);
        Edge edge3 = null;
        try {
            edge3 = new Edge(nc12, nc32);
        } catch (ConstructionException e) {
            logger.error("", e);
        }
        Set<Property> props3 = new HashSet<Property>();
        props3.add(new Bandwidth(0));
        TopoEdgeUpdate teu3 = new TopoEdgeUpdate(edge3, props3,
                UpdateType.ADDED);
        topoedgeupdateList.add(teu3);
        TopoEdgeUpdate teu4 = new TopoEdgeUpdate(edge3, props3,
                UpdateType.REMOVED);
        topoedgeupdateList.add(teu4);

        imp.edgeUpdate(topoedgeupdateList);

        Path res = imp.getRoute(node1, node3);
        List<Edge> expectedPath = (List<Edge>) new LinkedList<Edge>();
        expectedPath.add(0, edge1);
        expectedPath.add(1, edge2);
        Path expectedRes = null;
        try {
            expectedRes = new Path(expectedPath);
        } catch (ConstructionException e) {
            logger.error("", e);
        }
        if (!res.equals(expectedRes)) {
            System.out.println("Actual Res is " + res);
            System.out.println("Expected Res is " + expectedRes);
        }
        Assert.assertTrue(res.equals(expectedRes));
    }
}
