/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.topology.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opendaylight.controller.sal.compatibility.topology.TopologyMapping;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;

/**
 * test for {@link TopologyMapping}
 */
public class TopologyMappingTest {

    /**
     * Test method for {@link org.opendaylight.controller.sal.compatibility.topology.TopologyMapping#toADNodeId(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId)}.
     */
    @Test
    public void testToADNodeId() {
        NodeId nodeId = new NodeId("openflow:1");
        String observedNodeId = TopologyMapping.toADNodeId(nodeId);

        assertEquals("1", observedNodeId);
    }

    /**
     * Test method for {@link org.opendaylight.controller.sal.compatibility.topology.TopologyMapping#toADNodeConnector(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId)}.
     * @throws ConstructionException
     */
    @Test
    public void testToADNodeConnector() throws ConstructionException {
        NodeId nodeId = new NodeId("openflow:1");
        TpId source = new TpId("foo:2");
        NodeConnector observedNodeConnector = TopologyMapping.toADNodeConnector(source, nodeId);

        assertEquals("OF|2@OF|00:00:00:00:00:00:00:01", observedNodeConnector.toString());
    }

    /**
     * Test method for {@link org.opendaylight.controller.sal.compatibility.topology.TopologyMapping#toADNodeConnectorId(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId)}.
     */
    @Test
    public void testToADNodeConnectorId() {
        TpId source = new TpId("foo:2");
        String observedNodeConnectorId = TopologyMapping.toADNodeConnectorId(source);

        assertEquals("2", observedNodeConnectorId);
    }

    /**
     * Test method for {@link org.opendaylight.controller.sal.compatibility.topology.TopologyMapping#toADNode(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId)}.
     * @throws ConstructionException
     */
    @Test
    public void testToADNode() throws ConstructionException {
        NodeId nodeId = new NodeId("openflow:1");
        Node observedNode = TopologyMapping.toADNode(nodeId);

        assertEquals("OF|00:00:00:00:00:00:00:01", observedNode.toString());
    }

    /**
     * Test method for {@link org.opendaylight.controller.sal.compatibility.topology.TopologyMapping#toADNodeConnector(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId)}.
     * @throws ConstructionException
     */
    @Test
    public void bug1309ToADNodeConnector() throws ConstructionException {
        NodeId nodeId = new NodeId("some_unknown_node");
        TpId source = new TpId("192.168.0.1");
        NodeConnector observedNodeConnector = TopologyMapping.toADNodeConnector(source, nodeId);

        assertEquals("MD_SAL_DEPRECATED|192.168.0.1@MD_SAL_DEPRECATED|some_unknown_node", observedNodeConnector.toString());
    }

}
