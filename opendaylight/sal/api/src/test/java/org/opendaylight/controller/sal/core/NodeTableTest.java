/*
 * Copyright (c) 2013 Big Switch Networks, Inc.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.utils.NodeCreator;

public class NodeTableTest {
    @Test
    public void testNodeTableOpenFlowOfWrongType() {
        try {
            Node node = NodeCreator.createOFNode((long) 20);
            NodeTable of1 = new NodeTable(NodeTable.NodeTableIDType.OPENFLOW, "name", node);

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
    public void testNodeTableOpenFlowOfCorrectType() {
        try {
            Node node = NodeCreator.createOFNode((long) 20);
            NodeTable of1 = new NodeTable(NodeTable.NodeTableIDType.OPENFLOW, Byte.valueOf("10"), node);

            // If we reach this point the exception has not been
            // raised so we passed the test
            System.out.println("Got node table:" + of1);
            Assert.assertTrue(true);
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoOpenFlowNodeTableEquals() {
        try {
            Node node1 = NodeCreator.createOFNode((long) 20);
            NodeTable of1 = new NodeTable(NodeTable.NodeTableIDType.OPENFLOW, Byte.valueOf("10"), node1);
            NodeTable of2 = new NodeTable(NodeTable.NodeTableIDType.OPENFLOW, Byte.valueOf("10"), node1);

            Assert.assertTrue(of1.equals(of2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testTwoOpenFlowNodeTableDifferents() {
        try {
            Node node1 = NodeCreator.createOFNode((long) 20);
            NodeTable of1 = new NodeTable(NodeTable.NodeTableIDType.OPENFLOW, Byte.valueOf("10"), node1);
            Node node2 = NodeCreator.createOFNode((long) 40);
            NodeTable of2 = new NodeTable(NodeTable.NodeTableIDType.OPENFLOW, Byte.valueOf("20"), node2);

            Assert.assertTrue(!of1.equals(of2));
        } catch (ConstructionException e) {
            // If we reach this point the exception was raised
            // which is not expected
            Assert.assertTrue(false);
        }
    }
}
