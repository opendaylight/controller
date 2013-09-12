package org.opendaylight.controller.switchmanager;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.Status;

public class SubnetConfigTest {

    @Test
    public void configuration() throws ConstructionException {
        // Create the node connector string list
        Node node1 = new Node(Node.NodeIDType.OPENFLOW, 1L);
        Node node2 = new Node(Node.NodeIDType.OPENFLOW, 2L);
        Node node3 = new Node(Node.NodeIDType.OPENFLOW, 3L);
        NodeConnector nc1 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)1, node1);
        NodeConnector nc2 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)2, node2);
        NodeConnector nc3 = new NodeConnector(NodeConnector.NodeConnectorIDType.OPENFLOW, (short)3, node3);
        List<String> portList = new ArrayList<String>();
        portList.add(nc1.toString());
        portList.add(nc2.toString());
        portList.add(nc3.toString());

        // Full subnet creation
        SubnetConfig config = new SubnetConfig("eng", "11.1.1.254/16", portList);
        Status status = config.validate();
        Assert.assertTrue(status.isSuccess());

        // No port set specified
        config = new SubnetConfig("eng", "11.1.1.254/16", null);
        status = config.validate();
        Assert.assertTrue(status.isSuccess());

        // Empty port set
        config = new SubnetConfig("eng", "11.1.1.254/16", new ArrayList<String>(0));
        status = config.validate();
        Assert.assertTrue(status.isSuccess());

        // Zero subnet
        config = new SubnetConfig("eng", "1.2.3.254/1", null);
        status = config.validate();
        Assert.assertFalse(status.isSuccess());

        // Port set with invalid port notation
        List<String> badPortList = new ArrayList<String>();
        badPortList.add("1/1");
        config = new SubnetConfig("eng", "1.2.3.254/1", badPortList);
        status = config.validate();
        Assert.assertFalse(status.isSuccess());
    }
}
