package org.opendaylight.controller.switchmanager.northbound;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import junit.framework.TestCase;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.switchmanager.SubnetConfig;

public class SwitchManagerNorthboundTest extends TestCase {

        @Test
        public void testNodes() {
                List<NodeProperties> nodeProperties = new ArrayList<NodeProperties>();
                Nodes nodes = new Nodes(nodeProperties);
                Assert.assertTrue(nodes.getNodeProperties().equals(nodeProperties));
                nodes.setNodeProperties(null);
                Assert.assertTrue(nodes.getNodeProperties() == null);
        }

        @Test
        public void testNodeProperties() {
                Node node = NodeCreator.createOFNode(1L);
                NodeProperties np= new NodeProperties(node, null);
                Assert.assertTrue(np.getNode().equals(node));
                Assert.assertTrue(np.getProperties() == null);

                Node node2 = NodeCreator.createOFNode(2L);
                np.setNode(node2);
                Assert.assertTrue(np.getNode().equals(node2));

                Set<Property> props = new HashSet<Property>();
                np.setProperties(props);
                Assert.assertTrue(np.getProperties().equals(props));
        }

        @Test
        public void testNodeConnectors() {
                List<NodeConnectorProperties> nodeConnectorProperties = new ArrayList<NodeConnectorProperties>();
                NodeConnectors ncs = new NodeConnectors(nodeConnectorProperties);
                Assert.assertTrue(ncs.getNodeConnectorProperties().equals(nodeConnectorProperties));
                ncs.setNodeConnectorProperties(null);
                Assert.assertTrue(ncs.getNodeConnectorProperties() == null);
        }

        @Test
        public void testNodeConnectorProperties() {
                Node node = NodeCreator.createOFNode(1L);
                NodeConnector port = NodeConnectorCreator.createOFNodeConnector((short) 24, node);

                NodeConnectorProperties ncp= new NodeConnectorProperties(port, null);
                Assert.assertTrue(ncp.getProperties() == null);
                Assert.assertTrue(ncp.getNodeConnector().equals(port));

                NodeConnector port2 = NodeConnectorCreator.createOFNodeConnector((short) 33, node);
                ncp.setNodeConnector(port2);
                Assert.assertTrue(ncp.getNodeConnector().equals(port2));

                Set<Property> props = new HashSet<Property>();
                ncp.setProperties(props);
                Assert.assertTrue(ncp.getProperties().equals(props));
        }

        @Test
        public void testSubnets() {
            List<String> ports1 = new ArrayList<String>();
            ports1.add("OF|00:00:00:00:00:00:00:01/1");
            SubnetConfig sc1 = new SubnetConfig("test1", "10.0.0.1/8", ports1);
            SubnetConfig sc2 = new SubnetConfig("test2", "8.8.8.8/8", ports1);
            List<SubnetConfig> l = new ArrayList<SubnetConfig>();
            l.add(sc1);
            l.add(sc2);
            Subnets s = new Subnets(l);

            Assert.assertTrue(s.getSubnetConfigs().size() == 2);

            Assert.assertTrue(s.getSubnetConfigs().contains(sc1));
            Assert.assertTrue(s.getSubnetConfigs().contains(sc2));
        }
}
