
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   NodeTest.java
 *
 * @brief  Unit Tests for Node element
 *
 * Unit Tests for Node element
 */
package org.opendaylight.controller.sal.core;

import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;

public class NodeTest {
    @Test
    public void testNodeOpenFlowOfWrongType() {
        try {
            Node of1 = new Node(Node.NodeIDType.OPENFLOW, new String(
                    "0xDEADBEEFCAFE0001L"));

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
    public void testNodeONEPKOfWrongType() {
        try {
            Node onepk1 = new Node(Node.NodeIDType.ONEPK, new Long(
                    0xDEADBEEFCAFE0001L));

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
    public void testNodePCEPOfWrongType() {
        try {
            Node pcep1 = new Node(Node.NodeIDType.PCEP, new Long(
                    0xDEADBEEFCAFE0001L));

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
    public void testNodeOpenFlowOfCorrectType() {
        try {
            Node of1 = new Node(Node.NodeIDType.OPENFLOW, new Long(
                    0xDEADBEEFCAFE0001L));

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
    public void testNodeONEPKOfCorrectType() {
        try {
            Node onepk1 = new Node(Node.NodeIDType.ONEPK, new String(
                    "0xDEADBEEFCAFE0001L"));

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
    public void testNodePCEPOfCorrectType() {
        try {
            Node pcep1 = new Node(Node.NodeIDType.PCEP, new UUID(0L, 0L));

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
    public void testTwoOpenFlowNodeEquals() {
        try {
            Node of1 = new Node(Node.NodeIDType.OPENFLOW, new Long(
                    0xDEADBEEFCAFE0001L));
            Node of2 = new Node(Node.NodeIDType.OPENFLOW, new Long(
                    0xDEADBEEFCAFE0001L));

            Assert.assertTrue(of1.equals(of2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoONEPKNodeEquals() {
        try {
            Node onepk1 = new Node(Node.NodeIDType.ONEPK, new String(
                    "0xDEADBEEFCAFE0001L"));
            Node onepk2 = new Node(Node.NodeIDType.ONEPK, new String(
                    "0xDEADBEEFCAFE0001L"));

            Assert.assertTrue(onepk1.equals(onepk2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoPCEPNodeEquals() {
        try {
            Node pcep1 = new Node(Node.NodeIDType.PCEP, new UUID(
                    0xDEADBEEFCAFE0001L, 0L));
            Node pcep2 = new Node(Node.NodeIDType.PCEP, new UUID(
                    0xDEADBEEFCAFE0001L, 0L));

            Assert.assertTrue(pcep1.equals(pcep2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoOpenFlowNodeDifferents() {
        try {
            Node of1 = new Node(Node.NodeIDType.OPENFLOW, new Long(
                    0xDEADBEEFCAFE0001L));
            Node of2 = new Node(Node.NodeIDType.OPENFLOW, new Long(
                    0xDEADBEEFCAFE0002L));

            Assert.assertTrue(!of1.equals(of2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoONEPKNodeDifferents() {
        try {
            Node onepk1 = new Node(Node.NodeIDType.ONEPK, new String(
                    "0xDEADBEEFCAFE0001L"));
            Node onepk2 = new Node(Node.NodeIDType.ONEPK, new String(
                    "0xDEADBEEFCAFE0002L"));

            Assert.assertTrue(!onepk1.equals(onepk2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoPCEPNodeDifferents() {
        try {
            Node pcep1 = new Node(Node.NodeIDType.PCEP, new UUID(
                    0xDEADBEEFCAFE0001L, 0L));
            Node pcep2 = new Node(Node.NodeIDType.PCEP, new UUID(
                    0xDEADBEEFCAFE0001L, 1L));

            Assert.assertTrue(!pcep1.equals(pcep2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testToStringConversionAndBack() {
        // Test PCEP
        try {
            Node pcep = new Node(Node.NodeIDType.PCEP, new UUID(
                    0xDEADBEEFCAFE0001L, 0L));

            String pcepToStr = pcep.toString();
            System.out.println("Got String from PCEP=(" + pcepToStr + ")");

            Node pcepFromStr = Node.fromString(pcepToStr);

            // Make sure we got a node
            Assert.assertTrue(pcepFromStr != null);

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(pcep.equals(pcepFromStr));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }

        // Test ONEPK
        try {
            Node onepk = new Node(Node.NodeIDType.ONEPK, new String(
                    "0xDEADBEEFCAFE0001L"));

            String onepkToStr = onepk.toString();
            System.out.println("Got String from ONEPK=(" + onepkToStr + ")");

            Node onepkFromStr = Node.fromString(onepkToStr);

            // Make sure we got a node
            Assert.assertTrue(onepkFromStr != null);

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(onepk.equals(onepkFromStr));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }

        // Test OPENFLOW short string
        try {
            Node of = new Node(Node.NodeIDType.OPENFLOW, new Long(0x10L));

            String ofToStr = of.toString();
            System.out.println("Got String from OPENFLOW=(" + ofToStr + ")");

            Node ofFromStr = Node.fromString(ofToStr);

            // Make sure we got a node
            Assert.assertTrue(ofFromStr != null);

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(of.equals(ofFromStr));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }

        // Test OPENFLOW longer string
        try {
            Node of = new Node(Node.NodeIDType.OPENFLOW, new Long(
                    0xDEADBEEFCAFE0001L));

            String ofToStr = of.toString();
            System.out.println("Got String from OPENFLOW=(" + ofToStr + ")");

            Node ofFromStr = Node.fromString(ofToStr);

            // Make sure we got a node
            Assert.assertTrue(ofFromStr != null);

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(of.equals(ofFromStr));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testToStringConversionForOpenFlow() {
        // Test OPENFLOW longer string
        try {
            Node of = new Node(Node.NodeIDType.OPENFLOW, new Long(
                    0xDEADBEEFCAFE0001L));
            Node ofFromStr = null;

            // Decimal value for deadbeefcafe0001
            ofFromStr = Node.fromString("-2401053089206501375");

            // Make sure we got a node
            Assert.assertTrue(ofFromStr != null);

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(of.equals(ofFromStr));

            ofFromStr = Node.fromString("deadbeefcafe0001");

            // Make sure we got a node
            Assert.assertTrue(ofFromStr != null);

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(of.equals(ofFromStr));

            ofFromStr = Node.fromString("DEADBEEFCAFE0001");

            // Make sure we got a node
            Assert.assertTrue(ofFromStr != null);

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(of.equals(ofFromStr));

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(of.equals(ofFromStr));

            ofFromStr = Node.fromString("0xdeadbeefcafe0001");

            // Make sure we got a node
            Assert.assertTrue(ofFromStr != null);

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(of.equals(ofFromStr));

            ofFromStr = Node.fromString("0xDEADBEEFCAFE0001");

            // Make sure we got a node
            Assert.assertTrue(ofFromStr != null);

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(of.equals(ofFromStr));

            ofFromStr = Node.fromString("DE:AD:BE:EF:CA:FE:00:01");

            // Make sure we got a node
            Assert.assertTrue(ofFromStr != null);

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(of.equals(ofFromStr));

            ofFromStr = Node.fromString("de:ad:be:ef:ca:fe:00:01");

            // Make sure we got a node
            Assert.assertTrue(ofFromStr != null);

            // Now the converted node need to be the same of the
            // original one
            Assert.assertTrue(of.equals(ofFromStr));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testExtensibleNode() {
        // Add a new ID type
        Assert.assertTrue(Node.NodeIDType.registerIDType("FOO", Integer.class));
        
        // Trying to re-register the node must fail
        Assert.assertFalse(Node.NodeIDType.registerIDType("FOO",
                                                          Integer.class));
        try {
            Node n = new Node("FOO", new Integer(0xCAFE));

            System.out.println("Got Extended node:" + n);
        } catch (ConstructionException e) {
            // Got an unexpected exception
            Assert.assertTrue(false);
        }
        
        // Now unregister the type and make sure the node doesn't get
        // created
        Node.NodeIDType.unRegisterIDType("FOO");
        try {
            Node n = new Node("FOO", new Integer(0xCAFE));

            // If we reach here, something didn't go fine, an
            // exception should have been raised
            Assert.assertTrue(false);
        } catch (ConstructionException e) {
            // Got an expected exception, do nothing!
        }

        Assert.assertTrue(true);
    }
}
