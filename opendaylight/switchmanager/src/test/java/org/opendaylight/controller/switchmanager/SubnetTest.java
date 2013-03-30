/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;

public class SubnetTest {

    @Test
    public void testSubnet() throws Exception {
        InetAddress ipaddr = InetAddress.getByName("100.0.0.1");
        Subnet subnet = new Subnet(ipaddr, (short) 24, (short) 5);
        Assert.assertTrue(subnet.equals(subnet));
        Assert.assertFalse(subnet.equals(null));
        Assert.assertFalse(subnet.equals(ipaddr));
        Subnet subnet2 = new Subnet(ipaddr, (short) 24, (short) 5);
        Inet6Address ipv6 = (Inet6Address) Inet6Address
                .getByName("1111::2222:3333:4444:5555:6666");
        Subnet subnet3 = new Subnet(ipv6, (short) 24, (short) 5);
        Assert.assertTrue(subnet.equals(subnet2));
        Assert.assertFalse(subnet.isMutualExclusive(subnet2));
        Assert.assertTrue(subnet.isMutualExclusive(subnet3));
        InetAddress subnetAddr = InetAddress.getByName("200.0.0.100");

        Assert.assertTrue(subnet.isFlatLayer2() == true);

        Set<NodeConnector> ncSet = new HashSet<NodeConnector>();
        Node node = NodeCreator.createOFNode(((long) 10));
        NodeConnector nc0 = NodeConnectorCreator.createOFNodeConnector(
                (short) 20, node);
        NodeConnector nc1 = NodeConnectorCreator.createOFNodeConnector(
                (short) 30, node);
        NodeConnector nc2 = NodeConnectorCreator.createOFNodeConnector(
                (short) 40, node);

        ncSet.add(nc0);
        ncSet.add(nc1);
        ncSet.add(nc2);

        Assert.assertTrue(subnet.hasNodeConnector(nc0));
        Assert.assertFalse(subnet.hasNodeConnector(null));
        subnet.addNodeConnectors(ncSet);
        Assert.assertTrue(subnet.hasNodeConnector(nc0));

        Set<NodeConnector> resultncSet = subnet.getNodeConnectors();
        Assert.assertEquals(resultncSet, ncSet);
        subnet.addNodeConnectors(null);
        Assert.assertEquals(subnet.getNodeConnectors(), ncSet);

        subnet.deleteNodeConnectors(null);
        Assert.assertEquals(subnet.getNodeConnectors(), ncSet);

        Set<NodeConnector> ncSet2 = new HashSet<NodeConnector>();
        ncSet2.add(nc0);
        subnet.deleteNodeConnectors(ncSet2);
        Assert.assertFalse(subnet.getNodeConnectors().contains(nc0));
        Assert.assertFalse(subnet.hasNodeConnector(nc0));
        Assert.assertTrue(subnet.getNodeConnectors().contains(nc1));
        Assert.assertTrue(subnet.getNodeConnectors().contains(nc2));

        subnet.deleteNodeConnectors(ncSet2);

        subnet.setNetworkAddress(subnetAddr);
        Assert.assertTrue(subnet.isMutualExclusive(subnet2));
        Assert.assertTrue(subnet.getNetworkAddress().equals(subnetAddr));

        subnet.setSubnetMaskLength((short) 16);
        Assert.assertTrue(subnet.getSubnetMaskLength() == 16);

        subnet.setVlan((short) 100);
        Assert.assertTrue(subnet.getVlan() == 100);

        Assert.assertTrue(subnet.isFlatLayer2() == false);

    }

}
