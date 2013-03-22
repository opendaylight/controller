
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   EdgeTest.java
 *
 * @brief  Unit Tests for Edge element
 *
 * Unit Tests for Edge element
 */
package org.opendaylight.controller.sal.core;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;

public class EdgeTest {
    @Test
    public void testEdgeEquals() {
        try {
            Node n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            NodeConnector c0 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x4), n0);

            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector c1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x1), n1);

            Node n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            NodeConnector c2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x4), n2);

            Node n3 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector c3 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x1), n3);

            Edge e0 = new Edge(c0, c1);
            Edge e1 = new Edge(c2, c3);
            // e0 must be equal to e1 to pass the test
            Assert.assertTrue(e0.equals(e1));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testEdgeDifferents() {
        Node n0, n1, n2, n3;
        NodeConnector c0, c1, c2, c3;
        Edge e0, e1;
        try {
            // Difference in the tail node
            n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            c0 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x4), n0);

            n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            c1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x1), n1);

            e0 = new Edge(c0, c1);

            n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            c2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x4), n2);

            n3 = new Node(Node.NodeIDType.OPENFLOW, new Long(111L));
            c3 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x1), n3);

            e0 = new Edge(c0, c1);
            e1 = new Edge(c2, c3);
            // e0 must be different from e1 to pass the test
            Assert.assertTrue(!e0.equals(e1));

            // Difference in the head node
            n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            c0 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x4), n0);

            n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            c1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x1), n1);

            e0 = new Edge(c0, c1);

            n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(41L));
            c2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x4), n2);

            n3 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            c3 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x1), n3);

            e0 = new Edge(c0, c1);
            e1 = new Edge(c2, c3);
            // e0 must be different from e1 to pass the test
            Assert.assertTrue(!e0.equals(e1));

            // Difference in the head nodeconnetor
            n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            c0 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x4), n0);

            n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            c1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x1), n1);

            e0 = new Edge(c0, c1);

            n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            c2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x5), n2);

            n3 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            c3 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x1), n3);

            e0 = new Edge(c0, c1);
            e1 = new Edge(c2, c3);
            // e0 must be different from e1 to pass the test
            Assert.assertTrue(!e0.equals(e1));

            // Difference in the tail nodeconnetor
            n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            c0 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x4), n0);

            n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            c1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x1), n1);

            e0 = new Edge(c0, c1);

            n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            c2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x4), n2);

            n3 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            c3 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x2), n3);

            e0 = new Edge(c0, c1);
            e1 = new Edge(c2, c3);
            // e0 must be different from e1 to pass the test
            Assert.assertTrue(!e0.equals(e1));

            // Difference in the both nodeconnetor/node
            n0 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            c0 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x4), n0);

            n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            c1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x1), n1);

            e0 = new Edge(c0, c1);

            n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(40L));
            c2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x4), n2);

            n3 = new Node(Node.NodeIDType.OPENFLOW, new Long(111L));
            c3 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW,
                    new Short((short) 0x2), n3);

            e0 = new Edge(c0, c1);
            e1 = new Edge(c2, c3);
            // e0 must be different from e1 to pass the test
            Assert.assertTrue(!e0.equals(e1));
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }
    }
}
