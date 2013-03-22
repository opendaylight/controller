
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   PathTest.java
 *
 * @brief  Unit Tests for Path element
 *
 * Unit Tests for Path element
 */
package org.opendaylight.controller.sal.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Path;

public class PathTest {
    @Test
    public void testPathValid() {
        List<Edge> edges = null;
        try {
            Node n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            NodeConnector c0 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x4), n0);

            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector c1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x1), n1);
            NodeConnector c2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2ONEPK,
                    new Short((short) 0xCAFE), n1);

            Node n2 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector c3 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2OPENFLOW,
                    new String("towardOF1"), n2);
            NodeConnector c4 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n2);

            Node n3 = new Node(Node.NodeIDType.ONEPK, new String("Router2"));
            NodeConnector c5 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n3);

            Edge e0 = new Edge(c0, c1);
            Edge e1 = new Edge(c1, c2);
            Edge e2 = new Edge(c2, c3);
            Edge e3 = new Edge(c3, c4);
            Edge e4 = new Edge(c4, c5);
            edges = new LinkedList(Arrays.asList(e0, e1, e2, e3, e4));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            Path res = new Path(edges);
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        // Now lets disconnect on edge so to create a disconnected
        // path, the constructor should catch that and should not
        // create the path
        edges.remove(2);

        try {
            Path res = new Path(edges);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }
    }

    @Test
    public void testPathComparison() {
        List<Edge> edges1 = null;
        Path path1 = null;
        List<Edge> edges2 = null;
        Path path2 = null;
        List<Edge> edges3 = null;
        Path path3 = null;

        try {
            Node n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            NodeConnector c0 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x4), n0);

            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector c1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x1), n1);
            NodeConnector c2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2ONEPK,
                    new Short((short) 0xCAFE), n1);

            Node n2 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector c3 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2OPENFLOW,
                    new String("towardOF1"), n2);
            NodeConnector c4 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n2);

            Node n3 = new Node(Node.NodeIDType.ONEPK, new String("Router2"));
            NodeConnector c5 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n3);

            Edge e0 = new Edge(c0, c1);
            Edge e1 = new Edge(c1, c2);
            Edge e2 = new Edge(c2, c3);
            Edge e3 = new Edge(c3, c4);
            Edge e4 = new Edge(c4, c5);
            edges1 = new LinkedList(Arrays.asList(e0, e1, e2, e3, e4));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            Node n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            NodeConnector c0 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x4), n0);

            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector c1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x1), n1);
            NodeConnector c2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2ONEPK,
                    new Short((short) 0xCAFE), n1);

            Node n2 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector c3 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2OPENFLOW,
                    new String("towardOF1"), n2);
            NodeConnector c4 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n2);

            Node n3 = new Node(Node.NodeIDType.ONEPK, new String("Router2"));
            NodeConnector c5 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n3);

            Edge e0 = new Edge(c0, c1);
            Edge e1 = new Edge(c1, c2);
            Edge e2 = new Edge(c2, c3);
            Edge e3 = new Edge(c3, c4);
            Edge e4 = new Edge(c4, c5);
            edges2 = new LinkedList(Arrays.asList(e0, e1, e2, e3, e4));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            Node n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            NodeConnector c0 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x5), n0);

            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector c1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x1), n1);
            NodeConnector c2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2ONEPK,
                    new Short((short) 0xCAFE), n1);

            Node n2 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector c3 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2OPENFLOW,
                    new String("towardOF1"), n2);
            NodeConnector c4 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n2);

            Node n3 = new Node(Node.NodeIDType.ONEPK, new String("Router2"));
            NodeConnector c5 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n3);

            Edge e0 = new Edge(c0, c1);
            Edge e1 = new Edge(c1, c2);
            Edge e2 = new Edge(c2, c3);
            Edge e3 = new Edge(c3, c4);
            Edge e4 = new Edge(c4, c5);
            edges3 = new LinkedList(Arrays.asList(e0, e1, e2, e3, e4));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            path1 = new Path(edges1);
            path2 = new Path(edges2);
            path3 = new Path(edges3);

            System.out.println("Path1: " + path1);
            System.out.println("Path2: " + path2);
            System.out.println("Path3: " + path3);

            // Make sure the path are equals
            Assert.assertTrue(path1.equals(path2));

            // Make sure the path are marked as different
            Assert.assertTrue(!path1.equals(path3));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testPathEmpty() {
        try {
            Path path = new Path(new LinkedList());
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }
    }

    @Test
    public void testPathOneElement() {
        try {
            Node n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            NodeConnector c0 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x5), n0);

            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector c1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x1), n1);

            Edge e0 = new Edge(c0, c1);

            Path path = new Path(new LinkedList(Arrays.asList(e0)));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testPathGetNodes() {
        // Test on >2 edges paths
        try {
            Node n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            NodeConnector c0 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x4), n0);

            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector c1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x1), n1);
            NodeConnector c2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2ONEPK,
                    new Short((short) 0xCAFE), n1);

            Node n2 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector c3 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2OPENFLOW,
                    new String("towardOF1"), n2);
            NodeConnector c4 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n2);

            Node n3 = new Node(Node.NodeIDType.ONEPK, new String("Router2"));
            NodeConnector c5 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n3);

            Edge e0 = new Edge(c0, c1);
            Edge e1 = new Edge(c1, c2);
            Edge e2 = new Edge(c2, c3);
            Edge e3 = new Edge(c3, c4);
            Edge e4 = new Edge(c4, c5);
            List<Edge> edges = new LinkedList(Arrays.asList(e0, e1, e2, e3, e4));
            Path path = new Path(edges);

            // Test start node
            Assert.assertTrue(path.getStartNode().equals(n0));

            // Test end node
            Assert.assertTrue(path.getEndNode().equals(n3));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        // Test on 1 edge path
        try {
            Node n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            NodeConnector c0 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x4), n0);

            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector c1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x1), n1);

            Edge e0 = new Edge(c0, c1);
            List<Edge> edges = new LinkedList(Arrays.asList(e0));
            Path path = new Path(edges);

            // Test start node
            Assert.assertTrue(path.getStartNode().equals(n0));

            // Test end node
            Assert.assertTrue(path.getEndNode().equals(n1));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }
    }
}
