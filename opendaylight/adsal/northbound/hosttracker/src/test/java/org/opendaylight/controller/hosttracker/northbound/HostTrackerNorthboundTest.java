/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.hosttracker.northbound;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.ConstructionException;

public class HostTrackerNorthboundTest {

    @Test
    public void testHosts() throws UnknownHostException, ConstructionException {
        Hosts h1 = new Hosts();
        Assert.assertNull(h1.getHostConfig());

        Hosts h2 = new Hosts(null);
        Assert.assertNull(h2.getHostConfig());

        Set<HostConfig> conn = new HashSet<HostConfig>();
        InetAddress addr = InetAddress.getByName("10.1.1.1");
        HostNodeConnector c1 = new HostNodeConnector(addr);
        conn.add(HostConfig.convert(c1));
        h1.setHostConfig(conn);
        Assert.assertTrue(h1.getHostConfig().equals(conn));

        Hosts h3 = new Hosts(conn);
        Assert.assertTrue(h3.getHostConfig().equals(conn));
        h3.setHostConfig(null);
        Assert.assertNull(h3.getHostConfig());

    }
}
