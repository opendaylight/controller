
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.arphandler.internal;


import org.junit.Assert;
import org.junit.Test;
import junit.framework.TestCase;

import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.HostTracker;

import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.internal.SwitchManagerImpl;


public class ArphandlerTest extends TestCase {
	 
	@Test
	public void testArphandlerCreation() {
			
		ArpHandler ah = null;
		ah = new ArpHandler();
		Assert.assertTrue(ah != null);
			
		HostTracker hostTracker = null;
		hostTracker = new HostTracker();
		ah.setHostTracker(hostTracker);
		IfIptoHost ht= ah.getHostTracker();
		Assert.assertTrue(ht.equals(hostTracker));
		ah.unsetHostTracker(hostTracker);
		ht= ah.getHostTracker();
		Assert.assertTrue(ht == null);
		
		ah.setHostListener(hostTracker);
		ah.unsetHostListener(hostTracker);
		
		ISwitchManager swManager = new SwitchManagerImpl();
		ah.setSwitchManager(swManager);
		ah.unsetSwitchManager(swManager);
		
	}


}
