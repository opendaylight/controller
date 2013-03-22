
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;

public class HostTrackerTest extends TestCase {

	@Test
	public void testHostTrackerCallable() throws UnknownHostException {
		
		HostTracker hostTracker = null;
		hostTracker = new HostTracker();
		Assert.assertFalse(hostTracker== null);
		
		InetAddress hostIP = InetAddress.getByName("192.168.0.8");
		
		HostTrackerCallable htCallable = new HostTrackerCallable (hostTracker, hostIP);
		Assert.assertTrue(htCallable.trackedHost.equals(hostIP));
		Assert.assertTrue(htCallable.hostTracker.equals(hostTracker));

		long count = htCallable.latch.getCount();
		htCallable.wakeup();
		Assert.assertTrue(htCallable.latch.getCount() == --count );
	}		
		
	
	
	@Test
	public void testHostTracker() throws UnknownHostException {
		HostTracker hostTracker = null;
		hostTracker = new HostTracker();
		Assert.assertFalse(hostTracker== null);
		
		InetAddress hostIP_1 = InetAddress.getByName("192.168.0.8");
		InetAddress hostIP_2 = InetAddress.getByName("192.168.0.18");
		Future<HostNodeConnector> dschost = hostTracker.discoverHost(hostIP_1);
		dschost = hostTracker.discoverHost(hostIP_2);
		hostTracker.nonClusterObjectCreate();
	}
	

}
