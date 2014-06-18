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
import org.opendaylight.controller.sal.core.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;

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

        observed = NodeMapping.toAdNodeId(new NodeConnectorId("MD_SAL|openflow:5:2"));
        Assert.assertEquals("MD_SAL|openflow:5", observed.getValue());
    }

}
