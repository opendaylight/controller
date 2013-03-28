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
        Assert.assertNull(h1.getHostNodeConnector());

        Hosts h2 = new Hosts(null);
        Assert.assertNull(h2.getHostNodeConnector());

        Set<HostNodeConnector> conn = new HashSet<HostNodeConnector>();
        InetAddress addr = InetAddress.getByName("10.1.1.1");
        HostNodeConnector c1 = new HostNodeConnector(addr);
        conn.add(c1);
        h1.setHostNodeConnector(conn);
        Assert.assertTrue(h1.getHostNodeConnector().equals(conn));

        Hosts h3 = new Hosts(conn);
        Assert.assertTrue(h3.getHostNodeConnector().equals(conn));
        h3.setHostNodeConnector(null);
        Assert.assertNull(h3.getHostNodeConnector());

    }
}
