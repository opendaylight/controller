
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.Latency;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.SubnetConfig;

public class SwitchManagerImplTest {

	@Test
	public void testSwitchManagerAddRemoveSubnet() {
		SwitchManagerImpl switchmgr = new SwitchManagerImpl();
		switchmgr.nonClusterObjectCreate();
		
		ArrayList<String>portList = new ArrayList<String>();
		portList.add("1/1");
		portList.add("1/2");
		portList.add("1/3");

		
		SubnetConfig subnet = new SubnetConfig("subnet", "10.0.0.254/16", portList);
		//System.out.println("*" + switchmgr.addSubnet(subnet) + "*");
		Status addResult = (switchmgr.addSubnet(subnet));
		Assert.assertTrue(addResult.isSuccess());
		
		Status removeResult = (switchmgr.removeSubnet(subnet.getName()));
		Assert.assertTrue(removeResult.isSuccess());

		SubnetConfig subnetConfigResult = switchmgr.getSubnetConfig(subnet.getName());
		Assert.assertTrue(subnetConfigResult == null);
		
	}
	
	@Test
	public void testSwitchManagerNodeConnectors() {
		SwitchManagerImpl switchmgr = new SwitchManagerImpl();
		switchmgr.nonClusterObjectCreate();
		
		State state;
		Bandwidth bw;
		Latency l;
		
		NodeConnector[] headnc = new NodeConnector[5];
		NodeConnector[] tailnc = new NodeConnector[5];

		Set<Property> props = new HashSet<Property>();
		state = new State(State.EDGE_UP);
		bw = new Bandwidth(Bandwidth.BW100Gbps);
		l = new Latency(Latency.LATENCY100ns);
		props.add(state);
		props.add(bw);
		props.add(l);
		
		for (short i = 1; i < 6; i = (short)(i + 1)) {

				headnc[i - 1] = NodeConnectorCreator.createOFNodeConnector(i, NodeCreator.createOFNode((long)i));
				tailnc[i - 1] = NodeConnectorCreator.createOFNodeConnector((short)(i+10), NodeCreator.createOFNode((long)(i+10)));
				switchmgr.updateNode(headnc[i - 1].getNode(), UpdateType.ADDED, props);
				switchmgr.updateNode(tailnc[i - 1].getNode(), UpdateType.ADDED, props);

				switchmgr.updateNodeConnector(headnc[i - 1], UpdateType.ADDED, props);
				switchmgr.updateNodeConnector(tailnc[i - 1], UpdateType.ADDED, props);
		}
		
		for (int i = 0; i < 5; i++) {
			Property bwProp = switchmgr.getNodeConnectorProp(headnc[i], Bandwidth.BandwidthPropName);
			Assert.assertTrue(bwProp.equals(bw));
			Property latencyProp = switchmgr.getNodeConnectorProp(tailnc[i], Latency.LatencyPropName);
			Assert.assertEquals(latencyProp, l);
			
			byte[] headNodeMac = switchmgr.getNodeMAC(headnc[i].getNode());
			Assert.assertTrue(headNodeMac[headNodeMac.length - 1] == (byte)(i + 1));
		}
		
		Set<Node> nodes = switchmgr.getNodes();
		for (int i = 0; i < 5; i++) {
			if (nodes.contains(headnc[i].getNode()) == true) 
					nodes.remove(headnc[i].getNode());
			
			if (nodes.contains(tailnc[i].getNode()) == true) 
					nodes.remove(tailnc[i].getNode());
			
		}
		Assert.assertTrue(nodes.isEmpty());
	}
	
}
