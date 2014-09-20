/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.test;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.MacAddress;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * test of {@link NodeMapping} utility class
 */
public class NodeMappingTest {

    /**
     * Test method for
     * {@link org.opendaylight.controller.sal.compatibility.NodeMapping#toADMacAddress(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId)}
     * .
     */
    @Test
    public void testToADMacAddress() {
        NodeId[] nodeIds = new NodeId[] {
                // 0x0000|0000 0000002a (answer to the ultimate question of life, universe and everything)
                new NodeId("42"),
                // 0x7fff|ffff ffffffff (max long -> 2**63 - 1)
                new NodeId("9223372036854775807"),
                // 0x7fff|7fff ffffffff
                new NodeId("9223231299366420479"),
                // 0x8fff|7fff ffffffff (more than biggest positive long)
                new NodeId("10376152803973267455"),
                // 0xca13|764a e9ace65a (BUG-770)
                new NodeId("14561112084339025498")
        };

        byte[][] expectedMacs = new byte[][] {
                {0, 0, 0, 0, 0, 0x2a},
                {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff},
                {(byte) 0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff},
                {(byte) 0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff},
                {(byte) 0x76, (byte) 0x4a, (byte) 0xe9, (byte) 0xac, (byte) 0xe6, (byte) 0x5a}
        };

        Assert.assertEquals(expectedMacs.length, nodeIds.length);

        for (int i = 0; i < expectedMacs.length; i++) {
            NodeId nodeId = nodeIds[i];
            MacAddress mac = NodeMapping.toADMacAddress(nodeId);
            Assert.assertArrayEquals(expectedMacs[i], mac.getMacAddress());
        }
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.sal.compatibility.NodeMapping#toAdNodeId(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId)}
     * .
     */
    @Test
    public void testToAdNodeId() {
        NodeId observed;
        observed = NodeMapping.toAdNodeId(null);
        Assert.assertNull(observed);

        observed = NodeMapping.toAdNodeId(new NodeConnectorId("openflow:5:2"));
        Assert.assertEquals("openflow:5", observed.getValue());
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.sal.compatibility.NodeMapping#toADNode(NodeId)}
     * .
     */
    @Test
    public void testToAdNode1() {
        org.opendaylight.controller.sal.core.Node observed;
        try {
            observed = NodeMapping.toADNode((NodeId) null);
        } catch (NullPointerException | ConstructionException e) {
            //expected
        }

        NodeId nodeId = new NodeId("openflow:1");
        try {
            observed = NodeMapping.toADNode(nodeId);
            Assert.assertEquals("OF|00:00:00:00:00:00:00:01", observed.toString());
        } catch (ConstructionException e) {
            Assert.fail("should succeed to construct Node: "+e.getMessage());
        }

        final String nodeUriPrefix = "opendaylight-inventory:nodes/node/";
        nodeId = new NodeId(nodeUriPrefix + "iosv-2");
        try {
            observed = NodeMapping.toADNode(nodeId);
            Assert.assertEquals("PR|opendaylight-inventory:nodes/node/iosv-2", observed.toString());
        } catch (ConstructionException e) {
            Assert.fail("should succeed to construct Node: "+e.getMessage());
        }

    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.sal.compatibility.NodeMapping#toNodeConnectorType(NodeConnectorId, NodeId)}
     * .
     */
    @Test
    public void testToNodeConnectorType() {
        NodeConnectorId ncId;
        NodeId nodeId = buildNodeId("1");

        ncId = buildNodeConnectorId("1", "42");
        Assert.assertEquals(NodeConnectorIDType.OPENFLOW, NodeMapping.toNodeConnectorType(ncId, nodeId ));

        ncId = buildNodeConnectorId("1", OutputPortValues.CONTROLLER.toString());
        Assert.assertEquals(NodeConnectorIDType.CONTROLLER, NodeMapping.toNodeConnectorType(ncId, nodeId ));

        ncId = buildNodeConnectorId("1", OutputPortValues.NORMAL.toString());
        Assert.assertEquals(NodeConnectorIDType.HWPATH, NodeMapping.toNodeConnectorType(ncId, nodeId ));

        ncId = buildNodeConnectorId("1", OutputPortValues.LOCAL.toString());
        Assert.assertEquals(NodeConnectorIDType.SWSTACK, NodeMapping.toNodeConnectorType(ncId, nodeId ));
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.sal.compatibility.NodeMapping#toADNodeConnectorId(NodeConnectorId, NodeId)}
     * .
     */
    @Test
    public void testToAdNodeConnectorId() {
        NodeConnectorId nodeConnectorId = buildNodeConnectorId("1", "2");
        NodeId nodeId = buildNodeId("1");
        Assert.assertEquals(Short.valueOf((short) 2), NodeMapping.toADNodeConnectorId(nodeConnectorId , nodeId));
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.sal.compatibility.NodeMapping#toNodeRef(Node)}
     * .
     * @throws ConstructionException
     */
    @Test
    public void testToNodeRef() throws ConstructionException {
        org.opendaylight.controller.sal.core.Node node = new org.opendaylight.controller.sal.core.Node(NodeIDType.OPENFLOW, 42L);
        InstanceIdentifier<?> nodePath = NodeMapping.toNodeRef(node).getValue();

        String observedId = nodePath.firstKeyOf(Node.class, NodeKey.class).getId().getValue();
        Assert.assertEquals("openflow:42", observedId);
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.sal.compatibility.NodeMapping#toNodeConnectorRef(org.opendaylight.controller.sal.core.NodeConnector)}
     * .
     * @throws ConstructionException
     */
    @Test
    public void testToNodeConnectorRef() throws ConstructionException {
        org.opendaylight.controller.sal.core.Node node = new org.opendaylight.controller.sal.core.Node(NodeIDType.OPENFLOW, 42L);
        org.opendaylight.controller.sal.core.NodeConnector nodeConnector =
                new org.opendaylight.controller.sal.core.NodeConnector(
                        NodeConnectorIDType.OPENFLOW, (short) 1, node);

        InstanceIdentifier<?> nodeConnectorPath = NodeMapping.toNodeConnectorRef(nodeConnector ).getValue();
        String observedNodeId = nodeConnectorPath.firstKeyOf(Node.class, NodeKey.class).getId().getValue();
        Assert.assertEquals("openflow:42", observedNodeId);

        String observedNodeConnectorId = nodeConnectorPath.firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId().getValue();
        Assert.assertEquals("openflow:1", observedNodeConnectorId);
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.sal.compatibility.NodeMapping#openflowFullNodeIdToLong(String)}
     * .
     * @throws ConstructionException
     */
    @Test
    public void testOpenflowFullNodeIdToLong() throws ConstructionException {
        Assert.assertEquals(42L, NodeMapping.openflowFullNodeIdToLong("42").longValue());
        Assert.assertEquals(0xCC4E241C4A000000L, NodeMapping.openflowFullNodeIdToLong("14721743935839928320").longValue());
    }

    /**
     * Test method for
     * {@link org.opendaylight.controller.sal.compatibility.NodeMapping#toNodeKey(org.opendaylight.controller.sal.core.Node)}
     * .
     * @throws ConstructionException
     */
    @Test
    public void testToNodeKey() throws ConstructionException {
        org.opendaylight.controller.sal.core.Node aDNode = new org.opendaylight.controller.sal.core.Node(NodeIDType.OPENFLOW, 42L);
        NodeKey nodeKey = NodeMapping.toNodeKey(aDNode);
        Assert.assertEquals("openflow:42", nodeKey.getId().getValue());
    }

    /**
     * @param nodeId
     * @param portId
     * @return nodeConnectorId
     */
    public static NodeConnectorId buildNodeConnectorId(String nodeId, String portId) {
        return new NodeConnectorId(NodeConnectorIDType.OPENFLOW+"|" + nodeId + ":" + portId);
    }

    /**
     * @param id
     * @return nodeId
     */
    public static NodeId buildNodeId(String id) {
        return new NodeId(NodeConnectorIDType.OPENFLOW+"|" + id);
    }
}
