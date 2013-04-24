
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.hosttracker.hostAware;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;

import junit.framework.TestCase;

import org.opendaylight.controller.sal.packet.address.EthernetAddress;

import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.NodeCreator;


public class HostNodeConnectorTest extends TestCase {

	@Test
	public void testHostNodeConnector() throws UnknownHostException {
		HostNodeConnector hostnodeconnector_1, hostnodeconnector_2, hostnodeconnector_3;
		InetAddress hostIP_1 = InetAddress.getByName("192.168.0.8");
		InetAddress hostIP_2 = InetAddress.getByName("2001:420:281:1004:e123:e688:d655:a1b0");
		InetAddress hostIP_3 = InetAddress.getByName("192.168.0.28");
		byte[] hostMAC_2 = new byte[]{(byte)0x11,(byte)0x22,(byte)0x33,(byte)0x22,(byte)0x22,(byte)0x22};
		byte[] hostMAC_3 = new byte[]{(byte)0x11,(byte)0x22,(byte)0x33,(byte)0x33,(byte)0x33,(byte)0x33};
		
		Node node  = NodeCreator.createOFNode(1L);
		NodeConnector nc1 = NodeConnectorCreator.createOFNodeConnector((short) 2, node);
		NodeConnector nc2 = NodeConnectorCreator.createOFNodeConnector((short) 1, node);
		
		try {
			hostnodeconnector_1 = new HostNodeConnector(hostIP_1);
			Assert.assertTrue(hostnodeconnector_1.equalsByIP(hostIP_1));
			Assert.assertTrue(hostnodeconnector_1.isV4Host());
			Assert.assertTrue(hostnodeconnector_1.equalsByIP(hostIP_1));
		} catch (ConstructionException e) {
			Assert.assertTrue(false);
		}
		
		try {
			hostnodeconnector_2 = new HostNodeConnector(
				hostMAC_2, hostIP_2, nc1, (short)2);
			Assert.assertTrue(hostnodeconnector_2.isV6Host());
			Assert.assertTrue(hostnodeconnector_2.getnodeConnector().equals(nc1));
			Assert.assertTrue(hostnodeconnector_2.getnodeconnectorNode().equals(node));
			Assert.assertTrue(node.getID().equals(hostnodeconnector_2.getnodeconnectornodeId()));
			Assert.assertTrue(hostnodeconnector_2.getnodeconnectorportId().equals((short)2));
		} catch (ConstructionException e) {
			Assert.assertTrue(false);
		}
		
		try {
			hostnodeconnector_3 = new HostNodeConnector(
					new EthernetAddress(hostMAC_3), hostIP_3, nc2, (short)3);
			byte[] hostMAC_3_rb = hostnodeconnector_3.getDataLayerAddressBytes();
			HostNodeConnector  hostnodeconnector_3rb = new HostNodeConnector(
					new EthernetAddress(hostMAC_3_rb), hostIP_3, nc2, (short)3);
			Assert.assertTrue(hostnodeconnector_3.equals(hostnodeconnector_3rb));
			
			Assert.assertTrue(hostnodeconnector_3.getVlan() == (short)3);
			
			hostnodeconnector_3.setStaticHost(true);
			Assert.assertTrue(hostnodeconnector_3.isStaticHost());
			
			Assert.assertTrue(hostnodeconnector_3.isRewriteEnabled());
			
			hostnodeconnector_3.initArpSendCountDown().setArpSendCountDown((short) 10);
			Assert.assertTrue(hostnodeconnector_3.getArpSendCountDown() == (short)10);
			
		} catch (ConstructionException e) {
			Assert.assertTrue(false);
		}
		
	}

}
