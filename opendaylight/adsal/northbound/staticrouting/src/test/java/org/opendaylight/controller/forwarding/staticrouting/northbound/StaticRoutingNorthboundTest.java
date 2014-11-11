/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.forwarding.staticrouting.northbound;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class StaticRoutingNorthboundTest {

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
