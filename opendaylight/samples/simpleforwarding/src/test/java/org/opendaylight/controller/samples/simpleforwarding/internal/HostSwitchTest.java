
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.samples.simpleforwarding.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import org.junit.Test;
import org.junit.Assert;

import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.samples.simpleforwarding.internal.HostNodePair;

public class HostSwitchTest {
    @Test
    public void TestEquality() {
        HostNodeConnector h1 = null;
        HostNodeConnector h2 = null;

        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            return;
        }
        try {
            h2 = new HostNodeConnector(ip2);
        } catch (ConstructionException e) {
            return;
        }
        HostNodePair hsw1 = new HostNodePair(h1, NodeCreator.createOFNode(1l));
        HostNodePair hsw2 = new HostNodePair(h2, NodeCreator.createOFNode(1l));
        Assert.assertTrue(hsw1.equals(hsw2));
    }

    @Test
    public void TestDiversityHost() {
        HostNodeConnector h1 = null;
        HostNodeConnector h2 = null;
        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByName("10.0.0.2");
        } catch (UnknownHostException e) {
            return;
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            return;
        }
        try {
            h2 = new HostNodeConnector(ip2);
        } catch (ConstructionException e) {
            return;
        }

        HostNodePair hsw1 = new HostNodePair(h1, NodeCreator.createOFNode(1l));
        HostNodePair hsw2 = new HostNodePair(h2, NodeCreator.createOFNode(1l));
        Assert.assertTrue(!hsw1.equals(hsw2));
    }

    @Test
    public void TestDiversitySwitch() {
        HostNodeConnector h1 = null;
        HostNodeConnector h2 = null;
        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            return;
        }
        try {
            h2 = new HostNodeConnector(ip2);
        } catch (ConstructionException e) {
            return;
        }

        HostNodePair hsw1 = new HostNodePair(h1, NodeCreator.createOFNode(1l));
        HostNodePair hsw2 = new HostNodePair(h2, NodeCreator.createOFNode(2l));
        Assert.assertTrue(!hsw1.equals(hsw2));
    }

    @Test
    public void TestDiversityAll() {
        HostNodeConnector h1 = null;
        HostNodeConnector h2 = null;
        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByName("10.0.0.2");
        } catch (UnknownHostException e) {
            return;
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            return;
        }
        try {
            h2 = new HostNodeConnector(ip2);
        } catch (ConstructionException e) {
            return;
        }

        HostNodePair hsw1 = new HostNodePair(h1, NodeCreator.createOFNode(1l));
        HostNodePair hsw2 = new HostNodePair(h2, NodeCreator.createOFNode(2l));
        Assert.assertTrue(!hsw1.equals(hsw2));
    }

    @Test
    public void TestEqualHashCode1() {
        HostNodeConnector h1 = null;
        HostNodeConnector h2 = null;
        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            return;
        }
        try {
            h2 = new HostNodeConnector(ip2);
        } catch (ConstructionException e) {
            return;
        }
        HostNodePair hsw1 = new HostNodePair(h1, NodeCreator.createOFNode(1l));
        HostNodePair hsw2 = new HostNodePair(h2, NodeCreator.createOFNode(1l));
        Assert.assertTrue(hsw1.hashCode() == hsw2.hashCode());
    }

    @Test
    public void TestEqualHashCode2() {
        HostNodeConnector h1 = null;
        HostNodeConnector h2 = null;
        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName("10.0.0.2");
        } catch (UnknownHostException e) {
            return;
        }
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByName("10.0.0.2");
        } catch (UnknownHostException e) {
            return;
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            return;
        }
        try {
            h2 = new HostNodeConnector(ip2);
        } catch (ConstructionException e) {
            return;
        }

        HostNodePair hsw1 = new HostNodePair(h1, NodeCreator.createOFNode(1l));
        HostNodePair hsw2 = new HostNodePair(h2, NodeCreator.createOFNode(1l));
        Assert.assertTrue(hsw1.hashCode() == hsw2.hashCode());
    }

    @Test
    public void TestDiverseHashCodeHost() {
        HostNodeConnector h1 = null;
        HostNodeConnector h2 = null;
        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByName("10.0.0.2");
        } catch (UnknownHostException e) {
            return;
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            return;
        }
        try {
            h2 = new HostNodeConnector(ip2);
        } catch (ConstructionException e) {
            return;
        }
        HostNodePair hsw1 = new HostNodePair(h1, NodeCreator.createOFNode(1l));
        HostNodePair hsw2 = new HostNodePair(h2, NodeCreator.createOFNode(1l));
        Assert.assertTrue(hsw1.hashCode() != hsw2.hashCode());
    }

    @Test
    public void TestDiverseHashCodeSwitch() {
        HostNodeConnector h1 = null;
        HostNodeConnector h2 = null;
        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            return;
        }
        try {
            h2 = new HostNodeConnector(ip2);
        } catch (ConstructionException e) {
            return;
        }

        HostNodePair hsw1 = new HostNodePair(h1, NodeCreator.createOFNode(1l));
        HostNodePair hsw2 = new HostNodePair(h2, NodeCreator.createOFNode(2l));
        Assert.assertTrue(hsw1.hashCode() != hsw2.hashCode());
    }

    @Test
    public void TestDiverseHashCodeAll() {
        HostNodeConnector h1 = null;
        HostNodeConnector h2 = null;
        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByName("10.0.0.3");
        } catch (UnknownHostException e) {
            return;
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            return;
        }
        try {
            h2 = new HostNodeConnector(ip2);
        } catch (ConstructionException e) {
            return;
        }

        HostNodePair hsw1 = new HostNodePair(h1, NodeCreator.createOFNode(1l));
        HostNodePair hsw2 = new HostNodePair(h2, NodeCreator.createOFNode(2l));
        Assert.assertTrue(hsw1.hashCode() != hsw2.hashCode());
    }

    @Test
    public void TestUsageAsKey() {
        HostNodeConnector h1 = null;
        HostNodeConnector h2 = null;
        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            return;
        }
        try {
            h2 = new HostNodeConnector(ip2);
        } catch (ConstructionException e) {
            return;
        }

        HostNodePair hsw1 = new HostNodePair(h1, NodeCreator.createOFNode(1l));
        HostNodePair hsw2 = new HostNodePair(h2, NodeCreator.createOFNode(1l));
        HashMap<HostNodePair, Long> hm = new HashMap<HostNodePair, Long>();
        hm.put(hsw1, new Long(10));
        Assert.assertTrue(hm.get(hsw2) != null);
        Assert.assertTrue(hm.get(hsw2).equals(new Long(10)));
    }

    @Test
    public void TestUsageAsKeyChangingField() {
        HostNodeConnector h1 = null;
        HostNodeConnector h2 = null;
        InetAddress ip1 = null;
        try {
            ip1 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        InetAddress ip2 = null;
        try {
            ip2 = InetAddress.getByName("10.0.0.1");
        } catch (UnknownHostException e) {
            return;
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            return;
        }
        try {
            h2 = new HostNodeConnector(ip2);
        } catch (ConstructionException e) {
            return;
        }
        HostNodePair hsw1 = new HostNodePair(h1, null);
        HostNodePair hsw2 = new HostNodePair(h2, NodeCreator.createOFNode(1l));
        hsw1.setNode(NodeCreator.createOFNode(1l));
        HashMap<HostNodePair, Long> hm = new HashMap<HostNodePair, Long>();
        hm.put(hsw1, new Long(10));
        Assert.assertTrue(hm.get(hsw2) != null);
        Assert.assertTrue(hm.get(hsw2).equals(new Long(10)));
    }
}