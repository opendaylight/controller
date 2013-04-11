
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.topologymanager.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Latency;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Node.NodeIDType;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.packet.address.EthernetAddress;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.topologymanager.TopologyUserLinkConfig;

public class TopologyManagerImplTest { 
	
	/*
	 * Sets the node, edges and properties for edges here:
	 * Edge <SwitchId : NodeConnectorId> :
	 * <1:1>--><11:11>; <1:2>--><11:12>; 
	 * <3:3>--><13:13>; <3:4>--><13:14>;
	 * <5:5>--><15:15>; <5:6>--><15:16>;
	 * Method used by two tests: testGetNodeEdges and testGetEdges
	 * @param topoManagerImpl
	 * @throws ConstructionException
	 */
	public void setNodeEdges(TopologyManagerImpl topoManagerImpl) throws ConstructionException {
		topoManagerImpl.nonClusterObjectCreate();
		
		State state;
		Bandwidth bw;
		Latency l;
		
		Set<Property> props = new HashSet<Property>();
		state = new State(State.EDGE_UP);
		bw = new Bandwidth(Bandwidth.BW100Gbps);
		l = new Latency(Latency.LATENCY100ns);
		props.add(state);
		props.add(bw);
		props.add(l);
		
		for (short i = 1; i < 6; i=(short) (i+2)) {
				NodeConnector headnc1 = NodeConnectorCreator.createOFNodeConnector(i, NodeCreator.createOFNode((long)i));
				NodeConnector tailnc1 = NodeConnectorCreator.createOFNodeConnector((short)(i+10), NodeCreator.createOFNode((long)(i+10)));
				Edge e1 = new Edge(headnc1, tailnc1);
				topoManagerImpl.edgeUpdate(e1, UpdateType.ADDED, props);

				NodeConnector headnc2 = NodeConnectorCreator.createOFNodeConnector((short) (i+1), headnc1.getNode());
				NodeConnector tailnc2 = NodeConnectorCreator.createOFNodeConnector((short)(i+11), tailnc1.getNode());
				Edge e2 = new Edge(headnc2, tailnc2);
				topoManagerImpl.edgeUpdate(e2, UpdateType.ADDED, props);				
		}
	}
	
	@Test
	public void testGetNodeEdges() throws ConstructionException  {
		TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
		setNodeEdges(topoManagerImpl);	
		
		Map<Node, Set<Edge>> nodeEdgeMap = topoManagerImpl.getNodeEdges();
		for (Iterator<Map.Entry<Node,Set<Edge>>> i = nodeEdgeMap.entrySet().iterator();  i.hasNext();) {
			Map.Entry<Node, Set<Edge>> entry = i.next();
			Node node = entry.getKey();
			Long nodeId = ((Long) node.getID()).longValue();
			Assert.assertTrue((node.getType().equals(NodeIDType.OPENFLOW)));
						
			Set<Edge> edges = entry.getValue();
			for (Edge edge : edges) {
				Long headNcId =  ((Short)edge.getHeadNodeConnector().getID()).longValue();
				Long tailNcId = ((Short) edge.getTailNodeConnector().getID()).longValue();
				if (nodeId == 1 || nodeId == 3 || nodeId == 5) {
					Assert.assertTrue((headNcId.equals(nodeId) && tailNcId.equals(nodeId + 10)) ||
									  (headNcId.equals(nodeId + 10) && tailNcId.equals(nodeId)) ||
									  (headNcId.equals(nodeId + 1) && tailNcId.equals(nodeId + 11)) ||
									  (headNcId.equals(nodeId + 11) && tailNcId.equals(nodeId + 1)));
				} else if (nodeId == 11 || nodeId == 13 || nodeId == 15) {
					Assert.assertTrue((headNcId.equals(nodeId) && tailNcId.equals(nodeId - 10)) ||
									 (headNcId.equals(nodeId) && tailNcId.equals(nodeId - 10)) ||
									 (headNcId.equals(nodeId - 9) && tailNcId.equals(nodeId + 1)) ||
									 (headNcId.equals(nodeId + 1) && tailNcId.equals(nodeId - 9)));
				}
			}
			i.remove();
		}
		Assert.assertTrue(nodeEdgeMap.isEmpty());
	}
	
	@Test
	public void testGetEdges() throws ConstructionException  {
		TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
		setNodeEdges(topoManagerImpl);

		Map<Edge, Set<Property>> edgeProperty = topoManagerImpl.getEdges();
		
		for (Iterator <Map.Entry<Edge, Set<Property>>> i = edgeProperty.entrySet().iterator() ;  i.hasNext();) {
			Map.Entry<Edge, Set<Property>> entry = i.next();
			Edge e = entry.getKey();
			NodeConnector headnc = e.getHeadNodeConnector();
			NodeConnector tailnc = e.getTailNodeConnector();
			
			Long headNodeId = (Long) headnc.getNode().getID();
			
			Long headNcId =  ((Short)headnc.getID()).longValue();
			Long tailNcId = ((Short)tailnc.getID()).longValue();
			
			if (headNodeId == 1 || headNodeId == 3 || headNodeId == 5) {
				Assert.assertTrue((headNcId.equals(headNodeId) && tailNcId.equals(headNodeId + 10)) ||
								  (headNcId.equals(headNodeId + 10) && tailNcId.equals(headNodeId)) ||
								  (headNcId.equals(headNodeId + 1) && tailNcId.equals(headNodeId + 11)) ||
								  (headNcId.equals(headNodeId + 11) && tailNcId.equals(headNodeId + 1)));
			} else if (headNodeId == 11 || headNodeId == 13 || headNodeId == 15) {
				Assert.assertTrue((headNcId.equals(headNodeId) && tailNcId.equals(headNodeId - 10)) ||
								 (headNcId.equals(headNodeId) && tailNcId.equals(headNodeId - 10)) ||
								 (headNcId.equals(headNodeId - 9) && tailNcId.equals(headNodeId + 1)) ||
								 (headNcId.equals(headNodeId + 1) && tailNcId.equals(headNodeId - 9)));
			}

			Set<Property> prop = entry.getValue();
			for (Property p : prop) {
				String pName;
				long pValue;
				if (p instanceof Bandwidth) {
					Bandwidth b = (Bandwidth)p;
					pName = Bandwidth.BandwidthPropName;
					pValue  = b.getValue();
					Assert.assertTrue(pName.equals(p.getName()) && pValue == Bandwidth.BW100Gbps );
					continue;
				}
				if (p instanceof Latency) {
					Latency l = (Latency)p;
					pName = Latency.LatencyPropName;
					pValue  = l.getValue();
					Assert.assertTrue(pName.equals(p.getName()) && pValue == Latency.LATENCY100ns);
					continue;
				}
				if (p instanceof State) {
					State state = (State)p;
					pName = State.StatePropName;
					pValue  = state.getValue();
					Assert.assertTrue(pName.equals(p.getName()) && pValue  == State.EDGE_UP);
					continue;
				}
			}
			i.remove();
		}
		Assert.assertTrue(edgeProperty.isEmpty());
	}
	
	
	@Test
	public void testAddDeleteUserLink () {
		TopologyUserLinkConfig link1 = new TopologyUserLinkConfig("default1", "OF", "1", "OF", "2", "OF", "1", "OF", "2"); 
		TopologyUserLinkConfig link2 = new TopologyUserLinkConfig("default1", "OF", "10", "OF", "20", "OF", "10", "OF", "20"); 
		TopologyUserLinkConfig link3 = new TopologyUserLinkConfig("default2", "OF", "1", "OF", "2", "OF", "1", "OF", "2"); 
		TopologyUserLinkConfig link4 = new TopologyUserLinkConfig("default20", "OF", "10", "OF", "20", "OF", "10", "OF", "20"); 
		
		TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
		topoManagerImpl.nonClusterObjectCreate();
		
		Assert.assertTrue (topoManagerImpl.addUserLink(link1).isSuccess());		
		Assert.assertTrue (topoManagerImpl.addUserLink(link2).getCode() == StatusCode.CONFLICT);		
		Assert.assertTrue (topoManagerImpl.addUserLink(link3).getCode() == StatusCode.CONFLICT);		
		Assert.assertTrue (topoManagerImpl.addUserLink(link4).isSuccess());		
		
		Assert.assertTrue (topoManagerImpl.deleteUserLink(null).getCode() == StatusCode.BADREQUEST);	
		Assert.assertTrue (topoManagerImpl.deleteUserLink(link1.getName()).isSuccess());	
		Assert.assertTrue (topoManagerImpl.deleteUserLink(link4.getName()).isSuccess());	
		Assert.assertTrue (topoManagerImpl.getUserLinks().isEmpty());

	}
	
	@Test	
	public void testGetUserLink () {
		TopologyUserLinkConfig[] link = new TopologyUserLinkConfig[5];
		TopologyUserLinkConfig[] reverseLink = new TopologyUserLinkConfig[5];
		TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
		topoManagerImpl.nonClusterObjectCreate();
		
		String name = null;
		String srcNodeIDType = null;
		String srcSwitchId = null;
		String srcNodeConnectorIDType = null;
		String srcPort = null;
		String dstNodeIDType = null;
		String dstSwitchId = null;
		String dstNodeConnectorIDType = null;
		String dstPort = null;
		
		/*Creating userlinks and checking for their validity*/
		link[0] = new TopologyUserLinkConfig(name, srcNodeIDType, srcSwitchId,
				srcNodeConnectorIDType, srcPort, dstNodeIDType, dstSwitchId,
				dstNodeConnectorIDType, dstPort);
		Assert.assertTrue(link[0].isValid() == false);
		
		srcSwitchId = "1";
		link[0] = new TopologyUserLinkConfig(name, srcNodeIDType, srcSwitchId,
				srcNodeConnectorIDType, srcPort, dstNodeIDType, dstSwitchId,
				dstNodeConnectorIDType, dstPort);
		Assert.assertTrue(link[0].isValid() == false);
		
		dstSwitchId = "2";
		link[0] = new TopologyUserLinkConfig(name, srcNodeIDType, srcSwitchId,
				srcNodeConnectorIDType, srcPort, dstNodeIDType, dstSwitchId,
				dstNodeConnectorIDType, dstPort);
		Assert.assertTrue(link[0].isValid() == false);

		
		Integer i;
		
		for (i = 0; i < 5; i++) {
			link[i] = new TopologyUserLinkConfig(name, srcNodeIDType,
					srcSwitchId, srcNodeConnectorIDType, srcPort,
					dstNodeIDType, dstSwitchId, dstNodeConnectorIDType, dstPort);

			name = Integer.toString(i + 1);
			srcSwitchId = Integer.toString(i + 1);
			srcPort = Integer.toString(i + 1);
			dstSwitchId = Integer.toString((i + 1)*10);
			dstPort = Integer.toString((i + 1)*10);
			
			link[i].setName(name);
			link[i].setSrcSwitchId(srcSwitchId);
			link[i].setSrcPort(srcPort);
			link[i].setDstSwitchId(dstSwitchId);
			link[i].setDstPort(dstPort);
			
			Assert.assertTrue(link[i].isValid() == false);
			
			link[i].setSrcNodeIDType("OF");
			link[i].setSrcNodeConnectorIDType("OF");

			Assert.assertTrue(link[i].isValid() == false);

			link[i].setDstNodeIDType("OF");
			link[i].setDstNodeConnectorIDType("OF");
			
			Assert.assertTrue(link[i].isValid() == true);

			reverseLink[i] = new TopologyUserLinkConfig(name, dstNodeIDType,
					dstSwitchId, dstNodeConnectorIDType, dstPort,
					srcNodeIDType, srcSwitchId, srcNodeConnectorIDType, srcPort);

			topoManagerImpl.addUserLink(link[i]);
		}
		ConcurrentMap<String, TopologyUserLinkConfig> userLinks = topoManagerImpl.getUserLinks();
		TopologyUserLinkConfig resultLink;

		for (i = 0; i < 5; i++) {
			resultLink = userLinks.get(((Integer)(i + 1)).toString());
							
			Assert.assertTrue(resultLink.getName().equals(reverseLink[i].getName()));
			Assert.assertTrue(resultLink.getDstSwitchId().equals(reverseLink[i].getSrcSwitchId()));
			Assert.assertTrue(resultLink.getDstPort().equals(reverseLink[i].getSrcPort()));
			Assert.assertTrue(resultLink.getSrcSwitchId().equals(reverseLink[i].getDstSwitchId()));
			Assert.assertTrue(resultLink.getSrcPort().equals(reverseLink[i].getDstPort()));
		}
	}
	
	@Test
	 public void testHostLinkMethods() throws ConstructionException, UnknownHostException  {
		TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
		topoManagerImpl.nonClusterObjectCreate();
		int hostCounter = 0;
		
		State state;
		Bandwidth bw;
		Latency l;
		Set<Property> props = new HashSet<Property>();
		state = new State(State.EDGE_UP);
		bw = new Bandwidth(Bandwidth.BW100Gbps);
		l = new Latency(Latency.LATENCY100ns);
		props.add(state);
		props.add(bw);
		props.add(l);

		EthernetAddress ea;
		InetAddress ip;
		Host[] h = new Host[5];
		NodeConnector[] nc = new NodeConnector[5];
		
		/* Adding host, nodeConnector to hostsDB for the i = 0,1,2,3.  No host
		 * added for i = 4
		 */
		for (int i = 0; i < 5; i++) {
			if (hostCounter < 4) {
				ea = new EthernetAddress(new byte[]{(byte)0x0, (byte)0x0,
						(byte)0x0, (byte)0x0,
						(byte)0x0, (byte)i});
				String stringIP = new StringBuilder().append(i + 1).append(".").append(i+10).append(".").append(i+20).append(".").append(i+30).toString();
				ip = InetAddress.getByName(stringIP);
				h[hostCounter] = new Host(ea, ip);
			} else {
				h[hostCounter] = null;
			}
			hostCounter++;
			nc[i] = NodeConnectorCreator.createOFNodeConnector((short)(i + 1), NodeCreator.createOFNode((long)(i + 1)));
			topoManagerImpl.updateHostLink(nc[i], h[i], UpdateType.ADDED, props);
		}
		
		for (int i = 0; i < 5; i++) {
			Host host = topoManagerImpl.getHostAttachedToNodeConnector(nc[i]);
			if (i == 4)
				Assert.assertTrue(host == null);
			else
				Assert.assertTrue(host.equals(h[i]));			
		}
		
		Set<NodeConnector> ncSet = 	topoManagerImpl.getNodeConnectorWithHost();
		for (int i = 0; i < 5; i++) {
			Assert.assertTrue(ncSet.remove(nc[i]));
		}
		Assert.assertTrue(ncSet.isEmpty());
	}
	
	@Test
	public void testGetNodesWithNodeConnectorHost() throws ConstructionException, UnknownHostException {
		TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
		topoManagerImpl.nonClusterObjectCreate();
		int hostCounter = 0;
		
		State state;
		Bandwidth bw;
		Latency l;
		Set<Property> props = new HashSet<Property>();
		state = new State(State.EDGE_UP);
		bw = new Bandwidth(Bandwidth.BW100Gbps);
		l = new Latency(Latency.LATENCY100ns);
		props.add(state);
		props.add(bw);
		props.add(l);

		EthernetAddress ea;
		InetAddress ip;
		Host[] h = new Host[5];
		NodeConnector[] nc = new NodeConnector[5];
		
		/*Adding host, nodeconnector, properties of edge to hostsDB for the first three nodes only*/
		for (int i = 1; i < 6; i++) {
			if (i < 4) {
				ea = new EthernetAddress(new byte[]{(byte)0x0, (byte)0x0,
						(byte)0x0, (byte)0x0,
						(byte)0x0, (byte)i});
				String stringIP = new StringBuilder().append(i).append(".").append(i+10).append(".").append(i+20).append(".").append(i+30).toString();
				ip = InetAddress.getByName(stringIP);
				h[hostCounter] = new Host(ea, ip);
			}
			else {
				h[hostCounter] = null;
			}
			hostCounter++;
			nc[i - 1] = NodeConnectorCreator.createOFNodeConnector((short)i, NodeCreator.createOFNode((long)i));
			topoManagerImpl.updateHostLink(nc[i - 1], h[i - 1], UpdateType.ADDED, props);
		}
		
		/*Get the nodes which have host connected to its nodeConnector*/
		Map<Node, Set<NodeConnector>> nodeNCmap = topoManagerImpl.getNodesWithNodeConnectorHost();
		for (int i = 1; i < 6; i++) {
			Node node = nc[i - 1].getNode();
			Set<NodeConnector> ncSet = nodeNCmap.get(nc[i - 1].getNode());

			Assert.assertTrue(ncSet == nodeNCmap.remove(node));
		}
	
		Assert.assertTrue(nodeNCmap.isEmpty());
	}
}

