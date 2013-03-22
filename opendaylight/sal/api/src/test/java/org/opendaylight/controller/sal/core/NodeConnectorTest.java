
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   NodeConnectorTest.java
 *
 * @brief  Unit Tests for NodeConnector element
 *
 * Unit Tests for NodeConnector element
 */
package org.opendaylight.controller.sal.core;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;

public class NodeConnectorTest {
    @Test
    public void testNodeConnectorOpenFlowOfWrongType() {
        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector of1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new String(
                            "0xDEADBEEFCAFE0001L"), n1);

            // If we reach this point the exception was not raised
            // which should have been the case
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
            // If we reach this point the exception has been raised
            // and so test passed
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNodeConnectorONEPKOfWrongType() {
        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector onepk1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new Long(
                            0xDEADBEEFCAFE0001L), n1);

            // If we reach this point the exception was not raised
            // which should have been the case
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
            // If we reach this point the exception has been raised
            // and so test passed
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNodeConnectorPCEPOfWrongType() {
        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector pcep1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Long(
                            0xDEADBEEFCAFE0001L), n1);

            // If we reach this point the exception was not raised
            // which should have been the case
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
            // If we reach this point the exception has been raised
            // and so test passed
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNodeConnectorOpenFlowOfCorrectType() {
        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector of1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0xCAFE), n1);

            // If we reach this point the exception has not been
            // raised so we passed the test
            System.out.println("Got node:" + of1);
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testNodeConnectorONEPKOfCorrectType() {
        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector onepk1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n1);

            // If we reach this point the exception has not been
            // raised so we passed the test
            System.out.println("Got node:" + onepk1);
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testNodeConnectorPCEPOfCorrectType() {
        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector pcep1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Integer(
                            0xDEADBEEF), n1);

            // If we reach this point the exception has not been
            // raised so we passed the test
            System.out.println("Got node:" + pcep1);
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoOpenFlowNodeConnectorEquals() {
        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector of1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0xCAFE), n1);
            NodeConnector of2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0xCAFE), n1);

            Assert.assertTrue(of1.equals(of2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoONEPKNodeConnectorEquals() {
        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector onepk1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n1);
            NodeConnector onepk2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n1);

            Assert.assertTrue(onepk1.equals(onepk2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoPCEPNodeConnectorEquals() {
        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector pcep1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Integer(
                            0xDEADBEEF), n1);
            NodeConnector pcep2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Integer(
                            0xDEADBEEF), n1);

            Assert.assertTrue(pcep1.equals(pcep2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoOpenFlowNodeConnectorDifferents() {
        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector of1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0xCAFE), n1);
            NodeConnector of2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0xBEEF), n1);

            Assert.assertTrue(!of1.equals(of2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoONEPKNodeConnectorDifferents() {
        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector onepk1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n1);
            NodeConnector onepk2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/2"), n1);

            Assert.assertTrue(!onepk1.equals(onepk2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoPCEPNodeConnectorDifferents() {
        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector pcep1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Integer(
                            0xDEADBEEF), n1);
            NodeConnector pcep2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Integer(
                            0xCAFECAFE), n1);

            Assert.assertTrue(!pcep1.equals(pcep2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoOpenFlowNodeConnectorDifferentsNodes() {
        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            Node n2 = new Node(Node.NodeIDType.OPENFLOW, new Long(111L));
            NodeConnector of1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0xCAFE), n1);
            NodeConnector of2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0xCAFE), n2);

            Assert.assertTrue(!of1.equals(of2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoONEPKNodeConnectorDifferentsNodes() {
        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            Node n2 = new Node(Node.NodeIDType.ONEPK, new String("Router2"));
            NodeConnector onepk1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n1);
            NodeConnector onepk2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n2);

            Assert.assertTrue(!onepk1.equals(onepk2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoPCEPNodeConnectorDifferentsNodes() {
        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            Node n2 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 1L));
            NodeConnector pcep1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Integer(
                            0xDEADBEEF), n1);
            NodeConnector pcep2 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Integer(
                            0xDEADBEEF), n2);

            Assert.assertTrue(!pcep1.equals(pcep2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testIncompatibleNodes() {
        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2PCEP, new Short(
                            (short) 0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2ONEPK,
                    new Short((short) 0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2PCEP, new String(
                            "towardPCEP1"), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2OPENFLOW,
                    new String("towardPCEP1"), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Integer(0), n1);
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }
        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP2ONEPK,
                    new Integer(0), n1);
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP2OPENFLOW,
                    new Integer(0), n1);
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2PCEP, new Short(
                            (short) 0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2ONEPK,
                    new Short((short) 0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2PCEP, new String(
                            "towardPCEP1"), n1);
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2OPENFLOW,
                    new String("towardPCEP1"), n1);
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n1);
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Integer(0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP2ONEPK,
                    new Integer(0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP2OPENFLOW,
                    new Integer(0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0), n1);
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2PCEP, new Short(
                            (short) 0), n1);
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2ONEPK,
                    new Short((short) 0), n1);
        } catch (ConstructionException e) {
            // Exception is NOT expected if raised test will fail
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2PCEP, new String(
                            "towardPCEP1"), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2OPENFLOW,
                    new String("towardPCEP1"), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Integer(0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP2ONEPK,
                    new Integer(0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP2OPENFLOW,
                    new Integer(0), n1);
            // Exception is expected if not raised test will fail
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
        }
    }

    @Test
    public void testConversionToStringAndBack() {
        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(
                    0xDEADBEEFCAFE0001L, 0xDEADBEEFCAFE0002L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP2ONEPK, new Integer(
                            0xDEADBEEF), n1);
            String nc1Str = nc1.toString();
            System.out.println("NodeConnector String = " + nc1Str);
            NodeConnector nc1FromStr = NodeConnector.fromString(nc1Str);

            // Make sure we got a nodeconnector
            Assert.assertTrue(nc1FromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc1.equals(nc1FromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(
                    0xDEADBEEFCAFE0001L, 0xDEADBEEFCAFE0002L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP2OPENFLOW,
                    new Integer(0x10), n1);
            String nc1Str = nc1.toString();
            System.out.println("NodeConnector String = " + nc1Str);
            NodeConnector nc1FromStr = NodeConnector.fromString(nc1Str);

            // Make sure we got a nodeconnector
            Assert.assertTrue(nc1FromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc1.equals(nc1FromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.PCEP, new UUID(
                    0xDEADBEEFCAFE0001L, 0xDEADBEEFCAFE0002L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.PCEP, new Integer(0x10),
                    n1);
            String nc1Str = nc1.toString();
            System.out.println("NodeConnector String = " + nc1Str);
            NodeConnector nc1FromStr = NodeConnector.fromString(nc1Str);

            // Make sure we got a nodeconnector
            Assert.assertTrue(nc1FromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc1.equals(nc1FromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2PCEP, new String(
                            "towardPCEP1"), n1);

            String nc1Str = nc1.toString();
            System.out.println("NodeConnector String = " + nc1Str);
            NodeConnector nc1FromStr = NodeConnector.fromString(nc1Str);

            // Make sure we got a nodeconnector
            Assert.assertTrue(nc1FromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc1.equals(nc1FromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK2OPENFLOW,
                    new String("towardOPENFLOW1"), n1);

            String nc1Str = nc1.toString();
            System.out.println("NodeConnector String = " + nc1Str);
            NodeConnector nc1FromStr = NodeConnector.fromString(nc1Str);

            // Make sure we got a nodeconnector
            Assert.assertTrue(nc1FromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc1.equals(nc1FromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ONEPK, new String(
                            "Gi1/0/1"), n1);

            String nc1Str = nc1.toString();
            System.out.println("NodeConnector String = " + nc1Str);
            NodeConnector nc1FromStr = NodeConnector.fromString(nc1Str);

            // Make sure we got a nodeconnector
            Assert.assertTrue(nc1FromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc1.equals(nc1FromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(
                    0xDEADBEEFCAFE0001L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0xCAFE), n1);

            String nc1Str = nc1.toString();
            System.out.println("NodeConnector String = " + nc1Str);
            NodeConnector nc1FromStr = NodeConnector.fromString(nc1Str);

            // Make sure we got a nodeconnector
            Assert.assertTrue(nc1FromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc1.equals(nc1FromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(0x100L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2PCEP, new Short(
                            (short) 0x10), n1);

            String nc1Str = nc1.toString();
            System.out.println("NodeConnector String = " + nc1Str);
            NodeConnector nc1FromStr = NodeConnector.fromString(nc1Str);

            // Make sure we got a nodeconnector
            Assert.assertTrue(nc1FromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc1.equals(nc1FromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n1 = new Node(Node.NodeIDType.OPENFLOW, new Long(0x100L));
            NodeConnector nc1 = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW2ONEPK,
                    new Short((short) 0x11), n1);

            String nc1Str = nc1.toString();
            System.out.println("NodeConnector String = " + nc1Str);
            NodeConnector nc1FromStr = NodeConnector.fromString(nc1Str);

            // Make sure we got a nodeconnector
            Assert.assertTrue(nc1FromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc1.equals(nc1FromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testNodeConnectorSpecialType() {
        try {
            Node n = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.CONTROLLER,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ALL,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.SWSTACK,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.HWPATH,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.CONTROLLER,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ALL,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.SWSTACK,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.HWPATH,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.CONTROLLER,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ALL,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.SWSTACK,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.HWPATH,
                    NodeConnector.SPECIALNODECONNECTORID, n);
            System.out.println("Special NC = " + specialNc);
            // We expect to reach this point succesfully
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testToStringConversionForOpenFlow() {
        try {
            Node n = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector nc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0x2AB), n);

            NodeConnector ofFromStr = null;

            ofFromStr = NodeConnector.fromStringNoNode("0x2ab", n);

            // Make sure we got a nodeconnector
            Assert.assertTrue(ofFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc.equals(ofFromStr));

            ofFromStr = NodeConnector.fromStringNoNode("0x2AB", n);

            // Make sure we got a nodeconnector
            Assert.assertTrue(ofFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc.equals(ofFromStr));

            ofFromStr = NodeConnector.fromStringNoNode("683", n);

            // Make sure we got a nodeconnector
            Assert.assertTrue(ofFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc.equals(ofFromStr));
        } catch (Exception e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector nc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.OPENFLOW, new Short(
                            (short) 0xcafe), n);

            NodeConnector ofFromStr = null;

            ofFromStr = NodeConnector.fromStringNoNode("-13570", n);

            // Make sure we got a nodeconnector
            Assert.assertTrue(ofFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(nc.equals(ofFromStr));
        } catch (Exception e) {
            // If this expection is raised the test is failing
            System.out.println("Got exception as expected!:" + e);
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testConversionToStringAndBackSpecialPorts() {
        try {
            Node n = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.CONTROLLER,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ALL,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.HWPATH,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.OPENFLOW, new Long(110L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.SWSTACK,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.CONTROLLER,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ALL,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.HWPATH,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.SWSTACK,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.CONTROLLER,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.ALL,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.HWPATH,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }

        try {
            Node n = new Node(Node.NodeIDType.ONEPK, new String("Router1"));
            NodeConnector specialNc = new NodeConnector(
                    NodeConnector.NodeConnectorIDType.SWSTACK,
                    NodeConnector.SPECIALNODECONNECTORID, n);

            String specialNcStr = specialNc.toString();
            System.out.println("NodeConnector String obtained= " + specialNc);
            NodeConnector specialNcFromStr = NodeConnector
                    .fromString(specialNcStr);

            // Make sure we got a nodeconnector
            Assert.assertTrue(specialNcFromStr != null);

            // Now the converted nodeconnector need to be the same of the
            // original one
            Assert.assertTrue(specialNc.equals(specialNcFromStr));
        } catch (ConstructionException e) {
            // Fail if exception raised
            Assert.assertTrue(false);
        }
    }
}
