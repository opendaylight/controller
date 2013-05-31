package org.opendaylight.controller.statistics.northbound;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.controller.sal.utils.NodeCreator;

public class StatisticsNorthboundTest extends TestCase {

    @Test
    public void testFlowStatistics() {
        List<FlowOnNode> fon = new ArrayList<FlowOnNode>();
        Node node = NodeCreator.createOFNode(1L);
        FlowStatistics fs = new FlowStatistics(node, fon);
        Assert.assertTrue(fs.getNode().equals(node));
        Assert.assertTrue(fs.getFlowStats().equals(fon));

        Node node2 = NodeCreator.createOFNode(2L);
        fs.setNode(node2);
        Assert.assertTrue(fs.getNode().equals(node2));
        fs.setNode(node2);
        Assert.assertTrue(fs.getNode().equals(node2));
        fs.setFlowStats(null);
        Assert.assertTrue(fs.getFlowStats() == null);
    }

    @Test
    public void testAllFlowStatistics() {
        List<FlowStatistics> fs = new ArrayList<FlowStatistics>();
        AllFlowStatistics afs = new AllFlowStatistics(fs);
        Assert.assertTrue(afs.getFlowStatistics().equals(fs));
        afs.setFlowStatistics(null);
        Assert.assertTrue(afs.getFlowStatistics() == null);
    }

    @Test
    public void testPortStatistics() {
        List<NodeConnectorStatistics> ncs = new ArrayList<NodeConnectorStatistics>();
        Node node = NodeCreator.createOFNode(1L);
        PortStatistics ps = new PortStatistics(node, ncs);

        Assert.assertTrue(ps.getNode().equals(node));
        Assert.assertTrue(ps.getPortStats().equals(ncs));
        Node node2 = NodeCreator.createOFNode(2L);
        ps.setNode(node2);
        Assert.assertTrue(ps.getNode().equals(node2));
        ps.setFlowStats(null);
        Assert.assertTrue(ps.getPortStats() == null);
    }

    @Test
    public void testAllPortStatistics() {
        List<PortStatistics> ps = new ArrayList<PortStatistics>();
        AllPortStatistics aps = new AllPortStatistics(ps);
        Assert.assertTrue(aps.getPortStatistics().equals(ps));
        aps.setPortStatistics(null);
        Assert.assertTrue(aps.getPortStatistics() == null);
    }
    
    @Test
    public void testTableStatistics() {
        List<NodeTableStatistics> nts = new ArrayList<NodeTableStatistics>();
        Node node = NodeCreator.createOFNode(1L);
        TableStatistics ts = new TableStatistics(node, nts);

        Assert.assertTrue(ts.getNode().equals(node));
        Assert.assertTrue(ts.getTableStats().equals(nts));
        Node node2 = NodeCreator.createOFNode(2L);
        ts.setNode(node2);
        Assert.assertTrue(ts.getNode().equals(node2));
        ts.setTableStats(null);
        Assert.assertTrue(ts.getTableStats() == null);
    }
    
    @Test
    public void testAllTableStatistics() {
        List<TableStatistics> ts = new ArrayList<TableStatistics>();
        AllTableStatistics ats = new AllTableStatistics(ts);
        Assert.assertTrue(ats.getTableStatistics().equals(ts));
        ats.setTableStatistics(null);
        Assert.assertTrue(ats.getTableStatistics() == null);
    }

}
