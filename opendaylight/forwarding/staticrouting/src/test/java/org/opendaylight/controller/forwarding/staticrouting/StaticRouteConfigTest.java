
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwarding.staticrouting;


import java.net.InetAddress;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.forwarding.staticrouting.StaticRoute.NextHopType;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

public class StaticRouteConfigTest {
	
	@Test
	public void testStaticRouteSetGet() {
		StaticRouteConfig staticRouteConfig1 = new StaticRouteConfig();
		staticRouteConfig1.setName("route");
		staticRouteConfig1.setStaticRoute("10.1.1.2/32");
		staticRouteConfig1.setNextHop("200.2.2.2");
		staticRouteConfig1.setNextHopType(NextHopType.IPADDRESS.toString());
		StaticRouteConfig staticRouteConfig2 = new StaticRouteConfig("route", "10.1.1.2/32", "200.2.2.2");
		
		Assert.assertEquals(staticRouteConfig2.getName(), staticRouteConfig1.getName());
		Assert.assertEquals(staticRouteConfig2.getStaticRoute(), staticRouteConfig1.getStaticRoute());
		Assert.assertEquals(staticRouteConfig2.getNextHop(), staticRouteConfig1.getNextHop());
		Assert.assertEquals("nexthop-ip", staticRouteConfig1.getNextHopType());
	}
		
	@Test
	public void testStaticRouteisValid() {	
        StaticRouteConfig staticRouteConfig1 = new StaticRouteConfig("route1", "10.1.1.254/24", "100.1.1.1");
		Status receivedResponse1 = staticRouteConfig1.isValid();
		Status expectedResponse1 = new Status(StatusCode.SUCCESS, null);
		Assert.assertEquals(expectedResponse1.toString(), receivedResponse1.toString());
		
        StaticRouteConfig staticRouteConfig2 = new StaticRouteConfig("", "", "100.1.1.1");
		Status receivedResponse2 = staticRouteConfig2.isValid();
		Status expectedResponse2 = new Status(StatusCode.BADREQUEST,
        		"Invalid Static Route name");
		Assert.assertEquals(expectedResponse2.toString(), receivedResponse2.toString());

        StaticRouteConfig staticRouteConfig3 = new StaticRouteConfig("route1", "10.1.1.254", "100.1.1.1");
		Status receivedResponse3 = staticRouteConfig3.isValid();
		Status expectedResponse3 = new Status(StatusCode.BADREQUEST,
        		"Invalid Static Route entry. Please use the " +
        		"IPAddress/mask format. Default gateway " +
        		"(0.0.0.0/0) is NOT supported.");
		Assert.assertEquals(expectedResponse3.toString(), receivedResponse3.toString());

        StaticRouteConfig staticRouteConfig4 = new StaticRouteConfig("route1", "289.1.1.254/24", "100.1.1.1");
		Status receivedResponse4 = staticRouteConfig4.isValid();
		Status expectedResponse4 = new Status(StatusCode.BADREQUEST,
        		"Invalid Static Route entry. Please use the " +
        		"IPAddress/mask format. Default gateway " +
        		"(0.0.0.0/0) is NOT supported.");
		Assert.assertEquals(expectedResponse4.toString(), receivedResponse4.toString());
		
        StaticRouteConfig staticRouteConfig5 = new StaticRouteConfig("route1", "10.1.1.254/24", "100.1.1");
		Status receivedResponse5 = staticRouteConfig5.isValid();
		Status expectedResponse5 = new Status(StatusCode.BADREQUEST,
        		"Invalid NextHop IP Address configuration. " +
				"Please use the X.X.X.X format.");
		Assert.assertEquals(expectedResponse5.toString(), receivedResponse5.toString());
	}
	
	@Test
	public void testGetStaticRouteIP() {
        StaticRouteConfig staticRouteConfig1 = new StaticRouteConfig("route1", "10.1.1.0/24", "100.1.1.1");
        InetAddress ip1 = staticRouteConfig1.getStaticRouteIP();
		Assert.assertEquals("10.1.1.0", ip1.getHostAddress());        
		
        StaticRouteConfig staticRouteConfig2 = new StaticRouteConfig("route1", "10.1.1.0/80", "100.1.1.1");
        InetAddress ip2 = staticRouteConfig2.getStaticRouteIP();
		Assert.assertEquals(null, ip2);        

	}
	
	@Test
	public void testGetStaticRouteMask() {
        StaticRouteConfig staticRouteConfig1 = new StaticRouteConfig("route1", "10.1.1.0/24", "100.1.1.1");
		Short receivedMaskLen1 = staticRouteConfig1.getStaticRouteMask();
		Short expectedMaskLen1 = 24;
		Assert.assertEquals(expectedMaskLen1, receivedMaskLen1);

		StaticRouteConfig staticRouteConfig2 = new StaticRouteConfig("route1", "10.1.1.0/40", "100.1.1.1");
		Short receivedMaskLen2 = staticRouteConfig2.getStaticRouteMask();
		Short expectedMaskLen2 = 0;
		Assert.assertEquals(expectedMaskLen2, receivedMaskLen2);
	}
	
	@Test 
	public void testGetNextHopIP() {
        StaticRouteConfig staticRouteConfig1 = new StaticRouteConfig("route1", "10.1.1.254/24", "100.1.1.1");
        InetAddress ip1 = staticRouteConfig1.getNextHopIP();
		Assert.assertEquals("100.1.1.1", ip1.getHostAddress());                

		StaticRouteConfig staticRouteConfig2 = new StaticRouteConfig("route1", "10.1.1.254/24", "100.1.1");
        InetAddress ip2 = staticRouteConfig2.getNextHopIP();
		Assert.assertEquals(null, ip2);                
	}
	
}

