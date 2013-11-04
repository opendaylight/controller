package org.opendaylight.controller.sal.compatibility.adsal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.common.util.Futures;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.compatibility.NodeMapping;
import org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.reader.FlowOnNode;
import org.opendaylight.controller.sal.reader.IReadService;
import org.opendaylight.controller.sal.reader.IReadServiceListener;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.sal.reader.NodeDescription;
import org.opendaylight.controller.sal.reader.NodeTableStatistics;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllNodeConnectorStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllNodeConnectorStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllNodeConnectorStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowTableStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowTableStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowTableStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetNodeConnectorStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetNodeConnectorStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetNodeConnectorStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.NodeConnectorStatisticsUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.get.all.flow.statistics.output.FlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.get.all.flow.statistics.output.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.get.all.node.connector.statistics.output.NodeConnectorStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.statistics.Duration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.statistics.DurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.Bytes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.BytesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.Packets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.PacketsBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowStatisticsAdapter implements OpendaylightFlowStatisticsService, IReadServiceListener {

    private static final Logger LOG = LoggerFactory.getLogger(FlowStatisticsAdapter.class);
    private IReadService readDelegate;
    private NotificationProviderService notifier;

    @Override
    public Future<RpcResult<GetAllFlowStatisticsOutput>> getAllFlowStatistics(GetAllFlowStatisticsInput input) {
        GetAllFlowStatisticsOutput rpcResultType = null;
        boolean rpcResultBool = false;

        try {
            Node adNode = NodeMapping.toADNode(input.getNode());
            List<FlowOnNode> flowsOnNode = readDelegate.readAllFlows(adNode);
            List<FlowStatistics> flowsStatistics = toOdFlowsStatistics(flowsOnNode);
            GetAllFlowStatisticsOutputBuilder builder = new GetAllFlowStatisticsOutputBuilder();
            rpcResultType = builder.setFlowStatistics(flowsStatistics).build();
            rpcResultBool = true;
        } catch (ConstructionException e) {
            LOG.error(e.getMessage());
        }

        return Futures.immediateFuture(Rpcs.getRpcResult(rpcResultBool, rpcResultType, null));
    }

    @Override
    public Future<RpcResult<GetAllNodeConnectorStatisticsOutput>> getAllNodeConnectorStatistics(
            GetAllNodeConnectorStatisticsInput input) {
        GetAllNodeConnectorStatisticsOutput rpcResultType = null;
        boolean rpcResultBool = false;

        try {
            Node adNode = NodeMapping.toADNode(input.getNode());
            List<NodeConnectorStatistics> nodesConnectorStatistics = readDelegate.readNodeConnectors(adNode);
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.get.all.node.connector.statistics.output.NodeConnectorStatistics> odNodesConnectorStatistics;
            odNodesConnectorStatistics = toOdNodesConnectorStatistics(nodesConnectorStatistics);
            GetAllNodeConnectorStatisticsOutputBuilder builder = new GetAllNodeConnectorStatisticsOutputBuilder();
            rpcResultType = builder.setNodeConnectorStatistics(odNodesConnectorStatistics).build();
            rpcResultBool = true;
        } catch (ConstructionException e) {
            LOG.error(e.getMessage());
        }

        return Futures.immediateFuture(Rpcs.getRpcResult(rpcResultBool, rpcResultType, null));
    }

    @Override
    public Future<RpcResult<GetFlowStatisticsOutput>> getFlowStatistics(GetFlowStatisticsInput input) {
        GetFlowStatisticsOutput rpcResultType = null;
        boolean rpcResultBool = false;

        try {
            Node node = NodeMapping.toADNode(input.getNode());
            Flow flow = ToSalConversionsUtils.toFlow(input);
            FlowOnNode readFlow = readDelegate.readFlow(node, flow);
            FlowStatistics flowOnNodeToFlowStatistics = toOdFlowStatistics(readFlow);
            rpcResultType = new GetFlowStatisticsOutputBuilder(flowOnNodeToFlowStatistics).build();
            rpcResultBool = true;
        } catch (ConstructionException e) {
            LOG.error(e.getMessage());
        }

        return Futures.immediateFuture(Rpcs.getRpcResult(rpcResultBool, rpcResultType, null));
    }

    @Override
    public Future<RpcResult<GetFlowTableStatisticsOutput>> getFlowTableStatistics(GetFlowTableStatisticsInput input) {
        GetFlowTableStatisticsOutput rpcResultType = null;
        boolean rpcResultBool = false;

        try {
            Node node = NodeMapping.toADNode(input.getNode());
            List<NodeTableStatistics> nodesTable = readDelegate.readNodeTable(node);
            NodeTableStatistics nodeTable = null;
            if (!nodesTable.isEmpty()) {
                nodeTable = nodesTable.get(0);
                rpcResultType = toOdTableStatistics(nodeTable);
                rpcResultBool = true;
            }
        } catch (ConstructionException e) {
            LOG.error(e.getMessage());
        }

        return Futures.immediateFuture(Rpcs.getRpcResult(rpcResultBool, rpcResultType, null));
    }

    @Override
    public Future<RpcResult<GetNodeConnectorStatisticsOutput>> getNodeConnectorStatistics(
            GetNodeConnectorStatisticsInput input) {
        GetNodeConnectorStatisticsOutput rpcResultType = null;
        boolean rpcResultBool = false;

        NodeConnectorRef nodeConnector = input.getNodeConnector();
        try {
            NodeConnectorStatistics nodeConnectorStats = readDelegate.readNodeConnector(NodeMapping
                    .toADNodeConnector(nodeConnector));
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.get.all.node.connector.statistics.output.NodeConnectorStatistics odNodeConnectorStatistics = toOdNodeConnectorStatistics(nodeConnectorStats);
            rpcResultType = new GetNodeConnectorStatisticsOutputBuilder(odNodeConnectorStatistics).build();
            rpcResultBool = true;
        } catch (ConstructionException e) {
            LOG.error(e.getMessage());
        }

        return Futures.immediateFuture(Rpcs.getRpcResult(rpcResultBool, rpcResultType, null));
    }

    @Override
    public void descriptionStatisticsUpdated(Node node, NodeDescription nodeDescription) {

        // TODO which *StatisticsUpdated interface should be used?

    }

    @Override
    public void nodeConnectorStatisticsUpdated(Node node, List<NodeConnectorStatistics> ncStatsList) {
        for (NodeConnectorStatistics ndConStats : ncStatsList) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.get.all.node.connector.statistics.output.NodeConnectorStatistics odNodeConnectorStatistics;
            odNodeConnectorStatistics = toOdNodeConnectorStatistics(ndConStats);
            NodeConnectorStatisticsUpdatedBuilder statisticsBuilder = new NodeConnectorStatisticsUpdatedBuilder(
                    odNodeConnectorStatistics);
            notifier.publish(statisticsBuilder.build());
        }
    }

    @Override
    public void nodeFlowStatisticsUpdated(Node node, List<FlowOnNode> flowStatsList) {
        for (FlowOnNode flowOnNode : flowStatsList) {
            FlowStatistics flowStatistics = toOdFlowStatistics(flowOnNode);
            FlowStatisticsUpdatedBuilder statisticsBuilder = new FlowStatisticsUpdatedBuilder(flowStatistics);
            notifier.publish(statisticsBuilder.build());
        }
    }

    @Override
    public void nodeTableStatisticsUpdated(Node node, List<NodeTableStatistics> tableStatsList) {
        // TODO : Not implemented by AD-SAL.
    }

    private List<FlowStatistics> toOdFlowsStatistics(List<FlowOnNode> flowsOnNode) {
        List<FlowStatistics> flowsStatistics = new ArrayList<>();
        for (FlowOnNode flowOnNode : flowsOnNode) {
            flowsStatistics.add(toOdFlowStatistics(flowOnNode));
        }
        return flowsStatistics;
    }

    private FlowStatistics toOdFlowStatistics(FlowOnNode flowOnNode) {
        FlowStatisticsBuilder builder = new FlowStatisticsBuilder();

        builder.setByteCount(toCounter64(flowOnNode.getByteCount()));
        builder.setPacketCount(toCounter64(flowOnNode.getPacketCount()));
        builder.setDuration(extractDuration(flowOnNode));

        return builder.build();
    }

    private Duration extractDuration(FlowOnNode flowOnNode) {
        DurationBuilder builder = new DurationBuilder();
        builder.setNanosecond(toCounter64(flowOnNode.getDurationNanoseconds()));
        builder.setSecond(toCounter64(flowOnNode.getDurationSeconds()));
        return builder.build();
    }

    private Counter64 toCounter64(long num) {
        String byteCountStr = String.valueOf(num);
        BigInteger byteCountBigInt = new BigInteger(byteCountStr);
        return new Counter64(byteCountBigInt);
    }

    private Counter64 toCounter64(int num) {
        String byteCountStr = String.valueOf(num);
        BigInteger byteCountBigInt = new BigInteger(byteCountStr);
        return new Counter64(byteCountBigInt);
    }

    private List<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.get.all.node.connector.statistics.output.NodeConnectorStatistics> toOdNodesConnectorStatistics(
            List<NodeConnectorStatistics> nodesConnectorStatistics) {
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.get.all.node.connector.statistics.output.NodeConnectorStatistics> odNodesConnectorStatistics = new ArrayList<>();
        for (NodeConnectorStatistics nodeConnectorStatistics : nodesConnectorStatistics) {
            odNodesConnectorStatistics.add(toOdNodeConnectorStatistics(nodeConnectorStatistics));
        }
        return odNodesConnectorStatistics;
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.get.all.node.connector.statistics.output.NodeConnectorStatistics toOdNodeConnectorStatistics(
            NodeConnectorStatistics ndConStats) {
        NodeConnectorStatisticsBuilder builder = new NodeConnectorStatisticsBuilder();

        builder.setBytes(extractBytes(ndConStats));
        builder.setCollisionCount(toBI(ndConStats.getCollisionCount()));
        builder.setDuration(null);
        builder.setPackets(extractPackets(ndConStats));
        builder.setReceiveCrcError(toBI(ndConStats.getReceiveCRCErrorCount()));
        builder.setReceiveDrops(toBI(ndConStats.getReceiveDropCount()));
        builder.setReceiveErrors(toBI(ndConStats.getReceiveErrorCount()));
        builder.setReceiveFrameError(toBI(ndConStats.getReceiveFrameErrorCount()));
        builder.setReceiveOverRunError(toBI(ndConStats.getReceiveOverRunErrorCount()));
        builder.setTransmitDrops(toBI(ndConStats.getTransmitDropCount()));
        builder.setTransmitErrors(toBI(ndConStats.getTransmitErrorCount()));

        return builder.build();
    }

    private BigInteger toBI(long num) {
        String numStr = String.valueOf(num);
        return new BigInteger(numStr);
    }

    private Packets extractPackets(NodeConnectorStatistics nodeConnectorStatistics) {
        long receivePacketCount = nodeConnectorStatistics.getReceivePacketCount();
        long transmitPacketCount = nodeConnectorStatistics.getTransmitPacketCount();

        PacketsBuilder builder = new PacketsBuilder();
        builder.setReceived(toBI(receivePacketCount));
        builder.setTransmitted(toBI(transmitPacketCount));

        return builder.build();
    }

    private Bytes extractBytes(NodeConnectorStatistics nodeConnectorStatistics) {
        long transmitByteCount = nodeConnectorStatistics.getTransmitByteCount();
        long receiveByteCount = nodeConnectorStatistics.getReceiveByteCount();

        BytesBuilder builder = new BytesBuilder();
        builder.setReceived(toBI(receiveByteCount));
        builder.setTransmitted(toBI(transmitByteCount));

        return builder.build();
    }

    private GetFlowTableStatisticsOutput toOdTableStatistics(NodeTableStatistics nodeTable) {
        GetFlowTableStatisticsOutputBuilder builder = new GetFlowTableStatisticsOutputBuilder();

        builder.setActive(toCounter64(nodeTable.getActiveCount()));
        builder.setLookup(toCounter64(nodeTable.getLookupCount()));
        builder.setMatched(toCounter64(nodeTable.getMatchedCount()));

        return builder.build();
    }

}
