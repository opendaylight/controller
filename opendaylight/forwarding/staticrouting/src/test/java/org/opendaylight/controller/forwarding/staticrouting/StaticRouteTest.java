
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwarding.staticrouting;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.forwarding.staticrouting.StaticRoute.NextHopType;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;

public class StaticRouteTest {
	
	@Test
	public void testStaticRouteGetSet() {
		StaticRoute staticRoute = new StaticRoute();
		InetAddress networkAddress = null;
		InetAddress mask = null;
		InetAddress nextHopAddress = null;
		try {
			networkAddress = InetAddress.getByName("10.1.1.0");
			mask = InetAddress.getByName("255.255.255.0");
			nextHopAddress = InetAddress.getByName("200.0.0.1");
			
		} catch (UnknownHostException e) {
			Assert.assertTrue(false);
		}
		staticRoute.setNetworkAddress(networkAddress);
		Assert.assertEquals(networkAddress.getHostAddress(), staticRoute.getNetworkAddress().getHostAddress());
		staticRoute.setMask(mask);
		Assert.assertEquals(mask.getHostAddress(), staticRoute.getMask().getHostAddress());
		staticRoute.setType(NextHopType.IPADDRESS);
		Assert.assertEquals("nexthop-ip", staticRoute.getType().toString());
		staticRoute.setNextHopAddress(nextHopAddress);
		Assert.assertEquals(nextHopAddress.getHostAddress(), staticRoute.getNextHopAddress().getHostAddress());
		Node node = NodeCreator.createOFNode(((long)10));
		staticRoute.setNode(node);
		Assert.assertEquals(node, staticRoute.getNode());
		NodeConnector nc0 = NodeConnectorCreator.createOFNodeConnector((short)20, node);
		staticRoute.setPort(nc0);
		Assert.assertEquals(nc0, staticRoute.getPort());
        InetAddress ip1 = null;
        HostNodeConnector h1 = null;
        try {
            ip1 = InetAddress.getByName("192.1.1.1");
        } catch (UnknownHostException e) {
            Assert.assertTrue(false);
        }
        try {
            h1 = new HostNodeConnector(ip1);
        } catch (ConstructionException e) {
            Assert.assertTrue(false);
        }
        staticRoute.setHost(h1);
        Assert.assertEquals(h1, staticRoute.getHost());
	}
	
	@Test
	public void testStaticRouteComparison() {
        StaticRouteConfig staticRouteConfig1 = new StaticRouteConfig("route1", "10.1.1.0/24", "100.1.1.1");
        StaticRouteConfig staticRouteConfig2 = new StaticRouteConfig("route2", "10.1.1.0/24", "100.2.1.1");
        StaticRouteConfig staticRouteConfig3 = new StaticRouteConfig("route3", "10.2.1.0/24", "100.3.1.1");
        StaticRouteConfig staticRouteConfig4 = new StaticRouteConfig("route4", "10.1.1.0/31", "");
        StaticRoute staticRoute1 = new StaticRoute(staticRouteConfig1);
        StaticRoute staticRoute2 = new StaticRoute(staticRouteConfig2);
        StaticRoute staticRoute3 = new StaticRoute(staticRouteConfig3);
        StaticRoute staticRoute4 = new StaticRoute(staticRouteConfig4);

        Assert.assertTrue(staticRoute1.equals(staticRoute2));
        Assert.assertFalse(staticRoute1.equals(staticRoute3));
        Assert.assertFalse(staticRoute1.equals(staticRoute4));
        
        Assert.assertTrue(staticRoute1.compareTo(staticRoute2) == 0 ? true : false);
        Assert.assertFalse(staticRoute1.compareTo(staticRoute3) == 0 ? true : false);
        Assert.assertTrue(staticRoute1.compareTo(staticRoute4) == 0 ? true : false);
        		
	}
	
	@Test
	public void testLongestPrefixMatch() {
        StaticRouteConfig staticRouteConfig1 = new StaticRouteConfig("route1", "10.1.1.254/24", "100.1.1.1");
        StaticRoute staticRoute1 = new StaticRoute(staticRouteConfig1);
		InetAddress ip1 = null;
		InetAddress ip2 = null;
		try {
			ip1 = InetAddress.getByName("10.1.0.2");
			ip2 = InetAddress.getByName("10.1.1.2");
		} catch (UnknownHostException e) {
			Assert.assertTrue(false);
		}
        InetAddress rxdIp1 = staticRoute1.longestPrefixMatch(ip1);
        InetAddress rxdIp2 = staticRoute1.longestPrefixMatch(ip2);
		Assert.assertEquals(null, rxdIp1);
		Assert.assertEquals("10.1.1.0", rxdIp2.getHostAddress());
	}
}
