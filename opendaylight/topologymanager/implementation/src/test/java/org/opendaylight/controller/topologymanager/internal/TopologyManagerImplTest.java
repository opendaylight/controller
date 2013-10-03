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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.packet.address.EthernetAddress;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.SpanConfig;
import org.opendaylight.controller.switchmanager.Subnet;
import org.opendaylight.controller.switchmanager.SubnetConfig;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.switchmanager.SwitchConfig;
import org.opendaylight.controller.topologymanager.TopologyUserLinkConfig;

public class TopologyManagerImplTest {
    /**
     * Mockup of switch manager that only maintains existence of node
     * connector.
     */
    private final class TestSwitchManager implements ISwitchManager {
        private final Set<Node>  nodeSet = new HashSet<Node>();
        private final Set<NodeConnector> nodeConnectorSet =
            new HashSet<NodeConnector>();

        private void addNodeConnectors(NodeConnector ... connectors) {
            for (NodeConnector nc: connectors) {
                if (nc != null) {
                    nodeSet.add(nc.getNode());
                    nodeConnectorSet.add(nc);
                }
            }
        }

        private void addNodeConnectors(TopologyUserLinkConfig ... links) {
            for (TopologyUserLinkConfig link: links) {
                NodeConnector src =
                    NodeConnector.fromString(link.getSrcNodeConnector());
                NodeConnector dst =
                    NodeConnector.fromString(link.getDstNodeConnector());
                addNodeConnectors(src, dst);
            }
        }

        @Override
        public Status addSubnet(SubnetConfig configObject) {
            return null;
        }

        @Override
        public Status removeSubnet(SubnetConfig configObject) {
            return null;
        }

        @Override
        public Status modifySubnet(SubnetConfig configObject) {
            return null;
        }

        @Override
        public Status removeSubnet(String name) {
            return null;
        }

        @Override
        public List<Switch> getNetworkDevices() {
            return null;
        }

        @Override
        public List<SubnetConfig> getSubnetsConfigList() {
            return null;
        }

        @Override
        public SubnetConfig getSubnetConfig(String subnet) {
            return null;
        }

        @Override
        public Subnet getSubnetByNetworkAddress(InetAddress networkAddress) {
            return null;
        }

        @Override
        public Status saveSwitchConfig() {
            return null;
        }

        @Override
        public Status addSpanConfig(SpanConfig configObject) {
            return null;
        }

        @Override
        public Status removeSpanConfig(SpanConfig cfgObject) {
            return null;
        }

        @Override
        public List<SpanConfig> getSpanConfigList() {
            return null;
        }

        @Override
        public List<NodeConnector> getSpanPorts(Node node) {
            return null;
        }

        @Override
        public void updateSwitchConfig(SwitchConfig cfgObject) {
        }

        @Override
        public Status updateNodeConfig(SwitchConfig switchConfig) {
            return null;
        }

        @Override
        public Status removeNodeConfig(String nodeId) {
            return null;
        }

        @Override
        public SwitchConfig getSwitchConfig(String nodeId) {
            return null;
        }

        @Override
        public Status addPortsToSubnet(String name, List<String> nodeConnectors) {
            return null;
        }

        @Override
        public Status removePortsFromSubnet(String name, List<String> nodeConnectors) {
            return null;
        }

        @Override
        public Set<Node> getNodes() {
            return new HashSet<Node>(nodeSet);
        }

        @Override
        public Map<String, Property> getNodeProps(Node node) {
            return new HashMap<String, Property>();
        }

        @Override
        public Property getNodeProp(Node node, String propName) {
            return null;
        }

        @Override
        public void setNodeProp(Node node, Property prop) {
        }

        @Override
        public Status removeNodeProp(Node node, String propName) {
            return null;
        }

        @Override
        public Status removeNodeAllProps(Node node) {
            return null;
        }

        @Override
        public Set<NodeConnector> getUpNodeConnectors(Node node) {
            return getNodeConnectors(node);
        }

        @Override
        public Set<NodeConnector> getNodeConnectors(Node node) {
            Set<NodeConnector> set = new HashSet<NodeConnector>();
            for (NodeConnector nc: nodeConnectorSet) {
                if (nc.getNode().equals(node)) {
                    set.add(nc);
                }
            }

            return set;
        }

        @Override
        public Set<NodeConnector> getPhysicalNodeConnectors(Node node) {
            return getNodeConnectors(node);
        }

        @Override
        public Map<String, Property> getNodeConnectorProps(NodeConnector nodeConnector) {
            return new HashMap<String, Property>();
        }

        @Override
        public Property getNodeConnectorProp(NodeConnector nodeConnector, String propName) {
            return null;
        }

        @Override
        public Status addNodeConnectorProp(NodeConnector nodeConnector, Property prop) {
            return null;
        }

        @Override
        public Status removeNodeConnectorProp(NodeConnector nc, String propName) {
            return null;
        }

        @Override
        public Status removeNodeConnectorAllProps(NodeConnector nodeConnector) {
            return null;
        }

        @Override
        public NodeConnector getNodeConnector(Node node, String nodeConnectorName) {
            return null;
        }

        @Override
        public boolean isSpecial(NodeConnector p) {
            String type = p.getType();
            return (type.equals(NodeConnectorIDType.CONTROLLER)
                    || type.equals(NodeConnectorIDType.ALL)
                    || type.equals(NodeConnectorIDType.SWSTACK)
                    || type.equals(NodeConnectorIDType.HWPATH));
        }

        @Override
        public Boolean isNodeConnectorEnabled(NodeConnector nodeConnector) {
            if (doesNodeConnectorExist(nodeConnector)) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }

        @Override
        public boolean doesNodeConnectorExist(NodeConnector nc) {
            return (nc != null && nodeConnectorSet.contains(nc));
        }

        @Override
        public byte[] getControllerMAC() {
            return new byte[6];
        }

        @Override
        public byte[] getNodeMAC(Node node) {
            return new byte[6];
        }

        @Override
        public Property createProperty(String propName, String propValue) {
            return null;
        }

        @Override
        public String getNodeDescription(Node node) {
            return null;
        }
    }

    /*
     * Sets the node, edges and properties for edges here: Edge <SwitchId :
     * NodeConnectorId> : <1:1>--><11:11>; <1:2>--><11:12>; <3:3>--><13:13>;
     * <3:4>--><13:14>; <5:5>--><15:15>; <5:6>--><15:16>; Method used by two
     * tests: testGetNodeEdges and testGetEdges
     *
     * @param topoManagerImpl
     *
     * @throws ConstructionException
     */
    public void setNodeEdges(TopologyManagerImpl topoManagerImpl, TestSwitchManager swMgr)
            throws ConstructionException {
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

        for (short i = 1; i < 6; i = (short) (i + 2)) {
            List<TopoEdgeUpdate> topoedgeupdateList = new ArrayList<TopoEdgeUpdate>();
            NodeConnector headnc1 = NodeConnectorCreator.createOFNodeConnector(
                    i, NodeCreator.createOFNode((long) i));
            NodeConnector tailnc1 = NodeConnectorCreator
                    .createOFNodeConnector((short) (i + 10),
                            NodeCreator.createOFNode((long) (i + 10)));
            swMgr.addNodeConnectors(tailnc1, headnc1);
            Edge e1 = new Edge(tailnc1, headnc1);
            TopoEdgeUpdate teu1 = new TopoEdgeUpdate(e1, props,
                    UpdateType.ADDED);
            topoedgeupdateList.add(teu1);

            NodeConnector tailnc2 = NodeConnectorCreator.createOFNodeConnector(
                    (short) (i + 1), headnc1.getNode());
            NodeConnector headnc2 = NodeConnectorCreator.createOFNodeConnector(
                    (short) (i + 11), tailnc1.getNode());
            swMgr.addNodeConnectors(tailnc1, headnc2);
            Edge e2 = new Edge(tailnc2, headnc2);
            TopoEdgeUpdate teu2 = new TopoEdgeUpdate(e2, props,
                    UpdateType.ADDED);
            topoedgeupdateList.add(teu2);
            topoManagerImpl.edgeUpdate(topoedgeupdateList);
        }
    }

    @Test
    public void testGetNodeEdges() throws ConstructionException {
        TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
        TestSwitchManager swMgr = new TestSwitchManager();
        topoManagerImpl.setSwitchManager(swMgr);
        setNodeEdges(topoManagerImpl, swMgr);

        Map<Node, Set<Edge>> nodeEdgeMap = topoManagerImpl.getNodeEdges();
        for (Iterator<Map.Entry<Node, Set<Edge>>> i = nodeEdgeMap.entrySet()
                .iterator(); i.hasNext();) {
            Map.Entry<Node, Set<Edge>> entry = i.next();
            Node node = entry.getKey();
            Long nodeId = ((Long) node.getID()).longValue();
            Assert.assertTrue((node.getType().equals(NodeIDType.OPENFLOW)));

            Set<Edge> edges = entry.getValue();
            for (Edge edge : edges) {
                Long headNcId = ((Short) edge.getHeadNodeConnector().getID())
                        .longValue();
                Long tailNcId = ((Short) edge.getTailNodeConnector().getID())
                        .longValue();
                Assert.assertTrue(
                        (headNcId.equals(nodeId) && tailNcId.equals(nodeId + 10))
                        || (headNcId.equals(nodeId + 11) && tailNcId.equals(nodeId + 1))
                        || (headNcId.equals(nodeId + 1) && tailNcId.equals(nodeId - 9))
                        || (headNcId.equals(nodeId - 10) && tailNcId.equals(nodeId))
                        );
            }
            i.remove();
        }
        Assert.assertTrue(nodeEdgeMap.isEmpty());
    }

    @Test
    public void testGetEdges() throws ConstructionException {
        TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
        TestSwitchManager swMgr = new TestSwitchManager();
        topoManagerImpl.setSwitchManager(swMgr);
        setNodeEdges(topoManagerImpl, swMgr);

        Map<Edge, Set<Property>> edgeProperty = topoManagerImpl.getEdges();

        for (Iterator<Map.Entry<Edge, Set<Property>>> i = edgeProperty
                .entrySet().iterator(); i.hasNext();) {
            Map.Entry<Edge, Set<Property>> entry = i.next();
            Edge e = entry.getKey();
            NodeConnector headnc = e.getHeadNodeConnector();
            NodeConnector tailnc = e.getTailNodeConnector();

            Long headNodeId = (Long) headnc.getNode().getID();

            Long headNcId = ((Short) headnc.getID()).longValue();
            Long tailNcId = ((Short) tailnc.getID()).longValue();

            if (headNodeId == 1 || headNodeId == 3 || headNodeId == 5) {
                Assert.assertTrue((headNcId.equals(headNodeId) && tailNcId
                        .equals(headNodeId + 10))
                        || (headNcId.equals(headNodeId + 10) && tailNcId
                                .equals(headNodeId))
                                || (headNcId.equals(headNodeId + 1) && tailNcId
                                        .equals(headNodeId + 11))
                                        || (headNcId.equals(headNodeId + 11) && tailNcId
                                                .equals(headNodeId + 1)));
            } else if (headNodeId == 11 || headNodeId == 13 || headNodeId == 15) {
                Assert.assertTrue((headNcId.equals(headNodeId) && tailNcId
                        .equals(headNodeId - 10))
                        || (headNcId.equals(headNodeId) && tailNcId
                                .equals(headNodeId - 10))
                                || (headNcId.equals(headNodeId - 9) && tailNcId
                                        .equals(headNodeId + 1))
                                        || (headNcId.equals(headNodeId + 1) && tailNcId
                                                .equals(headNodeId - 9)));
            }

            Set<Property> prop = entry.getValue();
            for (Property p : prop) {
                String pName;
                long pValue;
                if (p instanceof Bandwidth) {
                    Bandwidth b = (Bandwidth) p;
                    pName = Bandwidth.BandwidthPropName;
                    pValue = b.getValue();
                    Assert.assertTrue(pName.equals(p.getName())
                            && pValue == Bandwidth.BW100Gbps);
                    continue;
                }
                if (p instanceof Latency) {
                    Latency l = (Latency) p;
                    pName = Latency.LatencyPropName;
                    pValue = l.getValue();
                    Assert.assertTrue(pName.equals(p.getName())
                            && pValue == Latency.LATENCY100ns);
                    continue;
                }
                if (p instanceof State) {
                    State state = (State) p;
                    pName = State.StatePropName;
                    pValue = state.getValue();
                    Assert.assertTrue(pName.equals(p.getName())
                            && pValue == State.EDGE_UP);
                    continue;
                }
            }
            i.remove();
        }
        Assert.assertTrue(edgeProperty.isEmpty());
    }

    @Test
    public void testAddDeleteUserLink() {
        TopologyUserLinkConfig link1 = new TopologyUserLinkConfig("default1",
                "OF|1@OF|2", "OF|1@OF|3");
        TopologyUserLinkConfig link2 = new TopologyUserLinkConfig("default1",
                "OF|10@OF|20", "OF|10@OF|30");
        TopologyUserLinkConfig link3 = new TopologyUserLinkConfig("default2",
                "OF|1@OF|2", "OF|1@OF|3");
        TopologyUserLinkConfig link4 = new TopologyUserLinkConfig("default20",
                "OF|10@OF|20", "OF|10@OF|30");

        TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
        TestSwitchManager swMgr = new TestSwitchManager();
        topoManagerImpl.setSwitchManager(swMgr);
        topoManagerImpl.nonClusterObjectCreate();

        swMgr.addNodeConnectors(link1, link2, link3, link4);

        Assert.assertTrue(topoManagerImpl.addUserLink(link1).isSuccess());
        Assert.assertTrue(topoManagerImpl.addUserLink(link2).getCode() == StatusCode.CONFLICT);
        Assert.assertTrue(topoManagerImpl.addUserLink(link3).getCode() == StatusCode.CONFLICT);
        Assert.assertTrue(topoManagerImpl.addUserLink(link4).isSuccess());

        Assert.assertTrue(topoManagerImpl.deleteUserLink(null).getCode() == StatusCode.BADREQUEST);
        Assert.assertTrue(topoManagerImpl.deleteUserLink(link1.getName())
                .isSuccess());
        Assert.assertTrue(topoManagerImpl.deleteUserLink(link4.getName())
                .isSuccess());
        Assert.assertTrue(topoManagerImpl.getUserLinks().isEmpty());

        TopologyUserLinkConfig badlink1 =
            new TopologyUserLinkConfig("bad1", "OF|1@OF|4", "OF|1@OF|5");
        TopologyUserLinkConfig badlink2 =
            new TopologyUserLinkConfig("bad2", "OF|10@OF|7", "OF|7@OF|13");
        Assert.assertEquals(StatusCode.NOTFOUND,
                            topoManagerImpl.addUserLink(badlink1).getCode());
        Assert.assertEquals(StatusCode.NOTFOUND,
                            topoManagerImpl.addUserLink(badlink2).getCode());
    }

    @Test
    public void testGetUserLink() {
        TopologyUserLinkConfig[] link = new TopologyUserLinkConfig[5];
        TopologyUserLinkConfig[] reverseLink = new TopologyUserLinkConfig[5];
        TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
        TestSwitchManager swMgr = new TestSwitchManager();
        topoManagerImpl.setSwitchManager(swMgr);
        topoManagerImpl.nonClusterObjectCreate();

        String name = "Test";
        String srcSwitchId = null;
        String srcNodeConnectorIDType = null;
        String srcPort = null;
        String srcNodeIDType = null;
        String dstNodeIDType = null;
        String dstSwitchId = null;
        String dstNodeConnectorIDType = null;
        String dstPort = null;
        String srcNodeConnector = null;
        String dstNodeConnector = null;

        /* Creating userlinks and checking for their validity */
        link[0] = new TopologyUserLinkConfig(name, srcNodeConnector, dstNodeConnector);
        Assert.assertTrue(link[0].isValid() == false);

        srcNodeConnector = "OF|1@OF|1";
        link[0] = new TopologyUserLinkConfig(name, srcNodeConnector, dstNodeConnector);
        Assert.assertTrue(link[0].isValid() == false);

        dstNodeConnector = "OF|1@OF|2";
        link[0] = new TopologyUserLinkConfig(name, srcNodeConnector, dstNodeConnector);
        Assert.assertTrue(link[0].isValid() == true);

        Integer i;

        for (i = 0; i < 5; i++) {
            link[i] = new TopologyUserLinkConfig();

            name = Integer.toString(i + 1);
            srcSwitchId = Integer.toString(i + 1);
            srcPort = Integer.toString(i + 1);
            dstSwitchId = Integer.toString((i + 1) * 10);
            dstPort = Integer.toString((i + 1) * 10);

            link[i].setName(name);
            srcNodeConnectorIDType = dstNodeConnectorIDType = "INCORRECT";
            srcNodeConnector = srcNodeConnectorIDType+"|"+srcSwitchId+"@"+srcNodeConnectorIDType+"|"+srcPort;
            dstNodeConnector = dstNodeConnectorIDType+"|"+dstSwitchId+"@"+dstNodeConnectorIDType+"|"+dstPort;

            link[i].setSrcNodeConnector(srcNodeConnector);
            Assert.assertTrue(link[i].isValid() == false);

            srcNodeConnectorIDType = "OF";
            srcNodeConnector = srcNodeConnectorIDType+"|"+srcSwitchId+"@"+srcNodeConnectorIDType+"|"+srcPort;
            link[i].setSrcNodeConnector(srcNodeConnector);
            Assert.assertTrue(link[i].isValid() == false);

            dstNodeConnectorIDType = "OF";
            dstNodeConnector = dstNodeConnectorIDType+"|"+dstSwitchId+"@"+dstNodeConnectorIDType+"|"+dstPort;
            link[i].setDstNodeConnector(dstNodeConnector);
            Assert.assertTrue(link[i].isValid() == true);

            reverseLink[i] = new TopologyUserLinkConfig(name, dstNodeConnector, srcNodeConnector);

            Assert.assertEquals(StatusCode.NOTFOUND,
                                topoManagerImpl.addUserLink(link[i]).getCode());
            swMgr.addNodeConnectors(link[i]);
            Assert.assertTrue(topoManagerImpl.addUserLink(link[i]).isSuccess());
        }

        ConcurrentMap<String, TopologyUserLinkConfig> userLinks = topoManagerImpl
                .getUserLinks();
        TopologyUserLinkConfig resultLink;

        for (i = 0; i < 5; i++) {
            resultLink = userLinks.get(((Integer) (i + 1)).toString());

            Assert.assertTrue(resultLink.getName().equals(
                    reverseLink[i].getName()));
            Assert.assertTrue(resultLink.getDstNodeConnector().equals(
                    reverseLink[i].getSrcNodeConnector()));
            Assert.assertTrue(resultLink.getSrcNodeConnector().equals(
                    reverseLink[i].getDstNodeConnector()));
        }
    }

    @Test
    public void testHostLinkMethods() throws ConstructionException,
    UnknownHostException {
        TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
        TestSwitchManager swMgr = new TestSwitchManager();
        topoManagerImpl.setSwitchManager(swMgr);
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

        /*
         * Adding host, nodeConnector to hostsDB for the i = 0,1,2,3. No host
         * added for i = 4
         */
        for (int i = 0; i < 5; i++) {
            if (hostCounter < 4) {
                ea = new EthernetAddress(new byte[] { (byte) 0x0, (byte) 0x0,
                        (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) i });
                String stringIP = new StringBuilder().append(i + 1).append(".")
                        .append(i + 10).append(".").append(i + 20).append(".")
                        .append(i + 30).toString();
                ip = InetAddress.getByName(stringIP);
                h[hostCounter] = new Host(ea, ip);
            } else {
                h[hostCounter] = null;
            }
            hostCounter++;
            nc[i] = NodeConnectorCreator.createOFNodeConnector((short) (i + 1),
                    NodeCreator.createOFNode((long) (i + 1)));
            topoManagerImpl
            .updateHostLink(nc[i], h[i], UpdateType.ADDED, props);
        }

        for (int i = 0; i < 5; i++) {
            Host host = topoManagerImpl.getHostAttachedToNodeConnector(nc[i]);
            if (i == 4) {
                Assert.assertTrue(host == null);
            } else {
                Assert.assertTrue(host.equals(h[i]));
            }
        }

        Set<NodeConnector> ncSet = topoManagerImpl.getNodeConnectorWithHost();
        for (int i = 0; i < 5; i++) {
            Assert.assertTrue(ncSet.remove(nc[i]));
        }
        Assert.assertTrue(ncSet.isEmpty());
    }

    @Test
    public void testGetNodesWithNodeConnectorHost()
            throws ConstructionException, UnknownHostException {
        TopologyManagerImpl topoManagerImpl = new TopologyManagerImpl();
        TestSwitchManager swMgr = new TestSwitchManager();
        topoManagerImpl.setSwitchManager(swMgr);
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

        /*
         * Adding host, nodeconnector, properties of edge to hostsDB for the
         * first three nodes only
         */
        for (int i = 1; i < 6; i++) {
            if (i < 4) {
                ea = new EthernetAddress(new byte[] { (byte) 0x0, (byte) 0x0,
                        (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) i });
                String stringIP = new StringBuilder().append(i).append(".")
                        .append(i + 10).append(".").append(i + 20).append(".")
                        .append(i + 30).toString();
                ip = InetAddress.getByName(stringIP);
                h[hostCounter] = new Host(ea, ip);
            } else {
                h[hostCounter] = null;
            }
            hostCounter++;
            nc[i - 1] = NodeConnectorCreator.createOFNodeConnector((short) i,
                    NodeCreator.createOFNode((long) i));
            topoManagerImpl.updateHostLink(nc[i - 1], h[i - 1],
                    UpdateType.ADDED, props);
        }

        /* Get the nodes which have host connected to its nodeConnector */
        Map<Node, Set<NodeConnector>> nodeNCmap = topoManagerImpl
                .getNodesWithNodeConnectorHost();
        for (int i = 1; i < 6; i++) {
            Node node = nc[i - 1].getNode();
            Set<NodeConnector> ncSet = nodeNCmap.get(nc[i - 1].getNode());

            Assert.assertTrue(ncSet == nodeNCmap.remove(node));
        }

        Assert.assertTrue(nodeNCmap.isEmpty());
    }
}
