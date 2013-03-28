package org.opendaylight.controller.forwarding.staticrouting.northbound;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

public class StaticRoutingNorthboundTest extends TestCase {

    @Test
    public void testStaticRoute() {
        StaticRoute sr = new StaticRoute();
        Assert.assertTrue(sr.getName() == null);
        Assert.assertTrue(sr.getPrefix() == null);
        Assert.assertTrue(sr.getNextHop() == null);

        sr = new StaticRoute("Static Route 1", "192.168.100.0/24", "170.0.0.1");
        Assert.assertTrue(sr.getName().equals("Static Route 1"));
        Assert.assertTrue(sr.getPrefix().equals("192.168.100.0/24"));
        Assert.assertTrue(sr.getNextHop().equals("170.0.0.1"));

        sr.setName("Static Route 2");
        Assert.assertTrue(sr.getName().equals("Static Route 2"));
        sr.setPrefix("192.168.100.0/26");
        Assert.assertTrue(sr.getPrefix().equals("192.168.100.0/26"));
        sr.setNextHop("170.0.2.1");
        Assert.assertTrue(sr.getNextHop().equals("170.0.2.1"));
    }

    @Test
    public void testStaticRoutes() {
        StaticRoutes srs = new StaticRoutes(null);
        Assert.assertTrue(srs.getFlowConfig() == null);

        List<StaticRoute> srl = new ArrayList<StaticRoute>();
        srs.setFlowConfig(srl);
        Assert.assertTrue(srs.getFlowConfig().equals(srl));
    }

}
