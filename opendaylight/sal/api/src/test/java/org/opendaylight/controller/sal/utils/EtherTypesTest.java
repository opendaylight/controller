
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;
	
public class EtherTypesTest {
		
	@Test
	public void testEthertypesCreation() {
		
		EtherTypes arp = EtherTypes.ARP;
		
		Assert.assertTrue(arp.toString().equals("ARP"));
		Assert.assertTrue(arp.intValue() == 2054);
		Assert.assertTrue(arp.shortValue() == (short)2054);
	}
	
	@Test
	public void testGetEtherTypesString() {
		
		Assert.assertTrue(EtherTypes.getEtherTypeName(34984).equals("QINQ"));
		Assert.assertTrue(EtherTypes.getEtherTypeName((short)2048).equals("IPv4"));
		Assert.assertTrue(EtherTypes.getEtherTypeName(0x010B).equals("PVSTP"));
		
		Assert.assertFalse(EtherTypes.getEtherTypeName(0x800).equals("ARP"));
	}
	
	@Test
	public void testGetEtherTypesNumber() {
		Assert.assertTrue(EtherTypes.getEtherTypeNumberInt("VLAN Tagged") == 33024);
		Assert.assertTrue(EtherTypes.getEtherTypeNumberShort("ARP") == 2054);
		
		Assert.assertFalse(EtherTypes.getEtherTypeNumberInt("CDP") == 1000);
	}
	
	@Test
	public void testGetEtherTypesList() {
		ArrayList<String> etherTypeNames = (ArrayList<String>) EtherTypes.getEtherTypesNameList();
		Assert.assertTrue(etherTypeNames.get(0).equals("PVSTP"));
		Assert.assertTrue(etherTypeNames.get(1).equals("CDP"));
		Assert.assertTrue(etherTypeNames.get(2).equals("VTP"));
		Assert.assertTrue(etherTypeNames.get(3).equals("IPv4"));
		Assert.assertTrue(etherTypeNames.get(4).equals("ARP"));
		Assert.assertTrue(etherTypeNames.get(5).equals("Reverse ARP"));
		Assert.assertTrue(etherTypeNames.get(6).equals("VLAN Tagged"));
		Assert.assertTrue(etherTypeNames.get(7).equals("IPv6"));
		Assert.assertTrue(etherTypeNames.get(8).equals("MPLS Unicast"));
		Assert.assertTrue(etherTypeNames.get(9).equals("MPLS Multicast"));
		Assert.assertTrue(etherTypeNames.get(10).equals("QINQ"));
		Assert.assertTrue(etherTypeNames.get(11).equals("LLDP"));
		Assert.assertTrue(etherTypeNames.get(12).equals("Old QINQ"));
		Assert.assertTrue(etherTypeNames.get(13).equals("Cisco QINQ"));		
	}
	
	@Test
	public void testGetEtherTypesloadFromString() {
		Assert.assertTrue(EtherTypes.loadFromString("37376").equals(EtherTypes.CISCOQINQ));
		Assert.assertTrue(EtherTypes.loadFromString("100") == null);
	}

}



