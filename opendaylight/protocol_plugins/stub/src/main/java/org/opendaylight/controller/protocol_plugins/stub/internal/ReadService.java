package org.opendaylight.controller.protocol_plugins.stub.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.dm.Component;
//import org.opendaylight.controller.protocol_plugin_stubs.IPluginReadServiceFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IPluginInReadService;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;

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
        Flow flow = new Flow();

        Match match = new Match();
        try {
            match.setField(MatchType.NW_DST, InetAddress.getByName("1.1.1.1"));
        } catch (UnknownHostException e) {

        }
        flow.setMatch(match);
        Action action = new Drop();

        List<Action> actions = new ArrayList<Action>();
        actions.add(action);
        flow.setActions(actions);
        flow.setPriority((short)3500);
        flow.setIdleTimeout((short)1000);
        flow.setHardTimeout((short)2000);
        flow.setId(12345);
        
        FlowOnNode fn1 = new FlowOnNode(flow);
        fn1.setByteCount(100);
        fn1.setDurationNanoseconds(400);
        fn1.setDurationSeconds(40);
        fn1.setTableId((byte) 0x1);
        fn1.setPacketCount(200);

        ArrayList<FlowOnNode> list = new ArrayList<FlowOnNode>();
        list.add(fn1);
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
        try{
            NodeConnector nc = new NodeConnector("STUB", 0xCAFE, node);
            stats.setNodeConnector(nc);
        }catch(ConstructionException e){
            //couldn't create nodeconnector.
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

}
