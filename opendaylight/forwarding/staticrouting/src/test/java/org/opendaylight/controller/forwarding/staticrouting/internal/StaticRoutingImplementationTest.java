
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.forwarding.staticrouting.internal;

import org.junit.Assert;
import org.junit.Test;

public class StaticRoutingImplementationTest {
	
	@Test
	public void isIPv4AddressValidTest() {
		StaticRoutingImplementation staticRouteImpl = new StaticRoutingImplementation();
        Assert.assertTrue(staticRouteImpl.isIPv4AddressValid("192.168.100.0/24"));		
        Assert.assertFalse(staticRouteImpl.isIPv4AddressValid("192.168.100.0/36"));		
        Assert.assertFalse(staticRouteImpl.isIPv4AddressValid("192.168.300.0/32"));		
	}
}




