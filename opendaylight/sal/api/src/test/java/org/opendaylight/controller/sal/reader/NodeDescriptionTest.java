
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.reader;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.reader.NodeDescription;

public class NodeDescriptionTest {
	
	@Test
	public void testNodeDescriptionMethods() {
		NodeDescription ncDesc = new NodeDescription();
		ncDesc.setHardware("Hardware1");
		ncDesc.setManufacturer("Manufacturer1");
		ncDesc.setDescription("SDNProtocol1");
		ncDesc.setSerialNumber("serialNumber1");
		ncDesc.setSoftware("Software1");
		
		Assert.assertTrue(ncDesc.getHardware().equals("Hardware1"));
		Assert.assertTrue(ncDesc.getManufacturer().equals("Manufacturer1"));
		Assert.assertTrue(ncDesc.getDescription().equals("SDNProtocol1"));
		Assert.assertTrue(ncDesc.getSerialNumber().equals("serialNumber1"));
		Assert.assertTrue(ncDesc.getSoftware().equals("Software1"));
		
		Assert.assertFalse(ncDesc.getHardware().equals("Hardware2"));
		Assert.assertFalse(ncDesc.getManufacturer().equals("Manufacturer2"));
		Assert.assertFalse(ncDesc.getDescription().equals("SDNProtocol2"));
		Assert.assertFalse(ncDesc.getSerialNumber().equals("serialNumber2"));
		Assert.assertFalse(ncDesc.getSoftware().equals("Software2"));

	}
}



