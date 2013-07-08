package org.opendaylight.controller.protocol_plugins.stub.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Controller;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.FloodAll;
import org.opendaylight.controller.sal.action.HwPath;
import org.opendaylight.controller.sal.action.Loopback;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.PopVlan;
import org.opendaylight.controller.sal.action.PushVlan;
import org.opendaylight.controller.sal.action.SetDlDst;
import org.opendaylight.controller.sal.action.SetDlSrc;
import org.opendaylight.controller.sal.action.SetDlType;
import org.opendaylight.controller.sal.action.SetNwDst;
import org.opendaylight.controller.sal.action.SetNwSrc;
import org.opendaylight.controller.sal.action.SetNwTos;
import org.opendaylight.controller.sal.action.SetTpDst;
import org.opendaylight.controller.sal.action.SetTpSrc;
import org.opendaylight.controller.sal.action.SetVlanCfi;
import org.opendaylight.controller.sal.action.SetVlanId;
import org.opendaylight.controller.sal.action.SetVlanPcp;
import org.opendaylight.controller.sal.action.SwPath;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IPluginInReadService;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Stub Implementation for IPluginInReadService used by SAL
 *
 *
 */
public class ReadService implements IPluginInReadService {
    private static final Logger logger = LoggerFactory
            .getLogger(ReadService.class);

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
    }

    @Override
    public FlowOnNode readFlow(Node node, Flow flow, boolean cached) {
        FlowOnNode fn1 = new FlowOnNode(flow);
        fn1.setByteCount(100);
        fn1.setDurationNanoseconds(400);
        fn1.setDurationSeconds(40);
        fn1.setTableId((byte) 0x1);
        fn1.setPacketCount(200);
        return fn1;
    }

    @Override
    public List<FlowOnNode> readAllFlow(Node node, boolean cached) {

        ArrayList<FlowOnNode> list = new ArrayList<FlowOnNode>();
        ArrayList<Action> actionList = new ArrayList<Action>();
        actionList.add(new Drop()); //IT assumes this is first element
        actionList.add(new Loopback());
        actionList.add(new Flood());
        actionList.add(new FloodAll());
        actionList.add(new Controller());
        actionList.add(new SwPath());
        actionList.add(new HwPath());
        try {
            actionList.add(new Output(new NodeConnector("STUB", 0xCAFE, node)));
        } catch (ConstructionException e) {

        }
        byte dst[] = { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5 };
        byte src[] = { (byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 1 };
        actionList.add(new SetDlSrc(src));
        actionList.add(new SetDlDst(dst));
        actionList.add(new SetDlType(10));

        actionList.add(new SetVlanId(2));
        actionList.add(new SetVlanPcp(3));
        actionList.add(new SetVlanCfi(1));

        actionList.add(new PopVlan());
        actionList.add(new PushVlan(0x8100, 1, 1, 1234));

        try {
            actionList.add(new SetNwSrc(InetAddress.getByName("2.2.2.2")));
            actionList.add(new SetNwDst(InetAddress.getByName("1.1.1.1")));
        } catch (UnknownHostException e) {

        }
        actionList.add(new SetNwTos(0x10));
        actionList.add(new SetTpSrc(4201));
        actionList.add(new SetTpDst(8080));

        short priority = 3500; //IT assumes this value
        for (Action a : actionList) {
            Flow flow = new Flow();
            Match match = new Match();
            try {
                match.setField(MatchType.NW_DST,
                        InetAddress.getByName("1.1.1.1"));
            } catch (UnknownHostException e) {

            }
            flow.setMatch(match);
            List<Action> actions = new ArrayList<Action>();
            actions.add(a);
            flow.setActions(actions);
            flow.setPriority(priority++);
            flow.setIdleTimeout((short) 1000);
            flow.setHardTimeout((short) 2000);
            flow.setId(12345);

            FlowOnNode fn1 = new FlowOnNode(flow);
            fn1.setByteCount(100);
            fn1.setDurationNanoseconds(400);
            fn1.setDurationSeconds(40);
            fn1.setTableId((byte) 0x1);
            fn1.setPacketCount(200);

            list.add(fn1);
        }
        return list;
    }

    @Override
    public NodeDescription readDescription(Node node, boolean cached) {
        NodeDescription desc = new NodeDescription();
        desc.setDescription("This is a sample node description");
        desc.setHardware("stub hardware");
        desc.setSoftware("stub software");
        desc.setSerialNumber("123");
        desc.setManufacturer("opendaylight");
        return desc;
    }

    @Override
    public NodeConnectorStatistics readNodeConnector(NodeConnector connector,
            boolean cached) {
        NodeConnectorStatistics stats = new NodeConnectorStatistics();
        stats.setNodeConnector(connector);
        stats.setCollisionCount(4);
        stats.setReceiveByteCount(1000);
        stats.setReceiveCRCErrorCount(1);
        stats.setReceiveDropCount(2);
        stats.setReceiveErrorCount(3);
        stats.setReceiveFrameErrorCount(5);
        stats.setReceiveOverRunErrorCount(6);
        stats.setReceivePacketCount(250);
        stats.setTransmitByteCount(5000);
        stats.setTransmitDropCount(50);
        stats.setTransmitErrorCount(10);
        stats.setTransmitPacketCount(500);

        return stats;
    }

    @Override
    public List<NodeConnectorStatistics> readAllNodeConnector(Node node,
            boolean cached) {
        NodeConnectorStatistics stats = new NodeConnectorStatistics();
        try {
            NodeConnector nc = new NodeConnector("STUB", 0xCAFE, node);
            stats.setNodeConnector(nc);
        } catch (ConstructionException e) {
            // couldn't create nodeconnector.
        }
        stats.setCollisionCount(4);
        stats.setReceiveByteCount(1000);
        stats.setReceiveCRCErrorCount(1);
        stats.setReceiveDropCount(2);
        stats.setReceiveErrorCount(3);
        stats.setReceiveFrameErrorCount(5);
        stats.setReceiveOverRunErrorCount(6);
        stats.setReceivePacketCount(250);
        stats.setTransmitByteCount(5000);
        stats.setTransmitDropCount(50);
        stats.setTransmitErrorCount(10);
        stats.setTransmitPacketCount(500);

        List<NodeConnectorStatistics> result = new ArrayList<NodeConnectorStatistics>();
        result.add(stats);
        return result;
    }

    @Override
    public long getTransmitRate(NodeConnector connector) {
        return 100;
    }

    @Override
    public NodeTableStatistics readNodeTable(NodeTable table, boolean b) {
        NodeTableStatistics stats = new NodeTableStatistics();
        stats.setNodeTable(table);
        stats.setActiveCount(4);
        stats.setLookupCount(4);
        stats.setMatchedCount(4);

        return stats;
    }

    @Override
    public List<NodeTableStatistics> readAllNodeTable(Node node, boolean cached) {
        NodeTableStatistics stats = new NodeTableStatistics();
        try {
            NodeTable nt = new NodeTable(NodeTable.NodeTableIDType.OPENFLOW, Byte.valueOf("10"), node);
            stats.setNodeTable(nt);
        } catch (ConstructionException e) {
            // couldn't create nodetable.
        }

        stats.setActiveCount(4);
        stats.setLookupCount(4);
        stats.setMatchedCount(4);

        List<NodeTableStatistics> result = new ArrayList<NodeTableStatistics>();
        result.add(stats);
        return result;
    }
}
