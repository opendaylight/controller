/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.compatibility.adsal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.compatibility.FromSalConversionsUtils;
import org.opendaylight.controller.sal.compatibility.InventoryMapping;
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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForGivenMatchInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsFromFlowTableInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsFromFlowTableOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowStatisticsFromFlowTableOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAllFlowsStatisticsFromAllFlowTablesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.and.statistics.map.list.FlowAndStatisticsMapListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.duration.DurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.Bytes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.BytesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.Packets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.PacketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.NodeConnectorStatisticsUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class FlowStatisticsAdapter implements OpendaylightFlowStatisticsService, IReadServiceListener{

    private static final Logger LOG = LoggerFactory.getLogger(FlowStatisticsAdapter.class);
    private IReadService readDelegate;
    private NotificationProviderService notifier;

    @Override
    public Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput>> getAggregateFlowStatisticsFromFlowTableForAllFlows(
            GetAggregateFlowStatisticsFromFlowTableForAllFlowsInput input) {
        //TODO: No supported API exist in AD-SAL, it can either be implemented by fetching all the stats of the flows and
        // generating aggregate flow statistics out of those individual flow stats.
        return null;
    }

    @Override
    public Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput>> getAggregateFlowStatisticsFromFlowTableForGivenMatch(
            GetAggregateFlowStatisticsFromFlowTableForGivenMatchInput input) {
        //TODO: No supported API exist in AD-SAL, it can either be implemented by fetching all the stats of the flows and
        // generating aggregate flow statistics out of those individual flow stats.
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetAllFlowStatisticsFromFlowTableOutput>> getAllFlowStatisticsFromFlowTable(
            GetAllFlowStatisticsFromFlowTableInput input) {
        GetAllFlowStatisticsFromFlowTableOutput rpcResultType = null;
        boolean rpcResultBool = false;

        try {
            Node adNode = NodeMapping.toADNode(input.getNode());
            List<FlowOnNode> flowsOnNode = readDelegate.readAllFlows(adNode);
            List<FlowAndStatisticsMapList> flowsStatistics = toOdFlowsStatistics(flowsOnNode);
            GetAllFlowStatisticsFromFlowTableOutputBuilder builder = new GetAllFlowStatisticsFromFlowTableOutputBuilder();
            builder.setTransactionId(new TransactionId(new BigInteger("0")));
            rpcResultType = builder.setFlowAndStatisticsMapList(flowsStatistics).build();

            rpcResultBool = true;
        } catch (ConstructionException e) {
            LOG.error(e.getMessage());
        }

        return Futures.immediateFuture(RpcResultBuilder.<GetAllFlowStatisticsFromFlowTableOutput>
                                                status(rpcResultBool).withResult(rpcResultType).build());
    }

    /**
     * Essentially this API will return the same result as getAllFlowStatisticsFromFlowTable
     */
    @Override
    public ListenableFuture<RpcResult<GetAllFlowsStatisticsFromAllFlowTablesOutput>> getAllFlowsStatisticsFromAllFlowTables(
            GetAllFlowsStatisticsFromAllFlowTablesInput input) {

        GetAllFlowsStatisticsFromAllFlowTablesOutput rpcResultType = null;
        boolean rpcResultBool = false;

        try {
            Node adNode = NodeMapping.toADNode(input.getNode());
            List<FlowOnNode> flowsOnNode = readDelegate.readAllFlows(adNode);
            List<FlowAndStatisticsMapList> flowsStatistics = toOdFlowsStatistics(flowsOnNode);
            GetAllFlowsStatisticsFromAllFlowTablesOutputBuilder builder = new GetAllFlowsStatisticsFromAllFlowTablesOutputBuilder();
            builder.setTransactionId(new TransactionId(new BigInteger("0")));
            rpcResultType = builder.setFlowAndStatisticsMapList(flowsStatistics).build();

            rpcResultBool = true;
        } catch (ConstructionException e) {
            LOG.error(e.getMessage());
        }

        return Futures.immediateFuture(RpcResultBuilder.<GetAllFlowsStatisticsFromAllFlowTablesOutput>
                                               status(rpcResultBool).withResult(rpcResultType).build());
    }

    @Override
    public ListenableFuture<RpcResult<GetFlowStatisticsFromFlowTableOutput>> getFlowStatisticsFromFlowTable(
            GetFlowStatisticsFromFlowTableInput input) {
        GetFlowStatisticsFromFlowTableOutput rpcResultType = null;
        boolean rpcResultBool = false;

        try {
            Node node = NodeMapping.toADNode(input.getNode());
            Flow flow = ToSalConversionsUtils.toFlow(input, null);
            FlowOnNode readFlow = readDelegate.readFlow(node, flow);
            List<FlowAndStatisticsMapList> flowOnNodeToFlowStatistics = new ArrayList<FlowAndStatisticsMapList>();
            flowOnNodeToFlowStatistics.add(toOdFlowStatistics(readFlow));
            rpcResultType = new GetFlowStatisticsFromFlowTableOutputBuilder().setFlowAndStatisticsMapList(flowOnNodeToFlowStatistics).build();
            rpcResultBool = true;
        } catch (ConstructionException e) {
            LOG.error(e.getMessage());
        }

        return Futures.immediateFuture(RpcResultBuilder.<GetFlowStatisticsFromFlowTableOutput>
                                              status(rpcResultBool).withResult(rpcResultType).build());
    }

    @Override
    public void nodeFlowStatisticsUpdated(Node node, List<FlowOnNode> flowStatsList) {
        List<FlowAndStatisticsMapList> flowStatistics = toOdFlowsStatistics(flowStatsList);
        FlowsStatisticsUpdateBuilder flowsStatisticsUpdateBuilder = new FlowsStatisticsUpdateBuilder();
        flowsStatisticsUpdateBuilder.setFlowAndStatisticsMapList(flowStatistics);
        flowsStatisticsUpdateBuilder.setMoreReplies(false);
        flowsStatisticsUpdateBuilder.setTransactionId(null);
        flowsStatisticsUpdateBuilder.setId(InventoryMapping.toNodeKey(node).getId());
        notifier.publish(flowsStatisticsUpdateBuilder.build());
    }

    @Override
    public void nodeConnectorStatisticsUpdated(Node node, List<NodeConnectorStatistics> ncStatsList) {
        NodeConnectorStatisticsUpdateBuilder nodeConnectorStatisticsUpdateBuilder = new NodeConnectorStatisticsUpdateBuilder();
        List<NodeConnectorStatisticsAndPortNumberMap> nodeConnectorStatistics = toOdNodeConnectorStatistics(ncStatsList);

        nodeConnectorStatisticsUpdateBuilder.setNodeConnectorStatisticsAndPortNumberMap(nodeConnectorStatistics);
        nodeConnectorStatisticsUpdateBuilder.setMoreReplies(false);
        nodeConnectorStatisticsUpdateBuilder.setTransactionId(null);
        nodeConnectorStatisticsUpdateBuilder.setId(InventoryMapping.toNodeKey(node).getId());
        notifier.publish(nodeConnectorStatisticsUpdateBuilder.build());
    }

    @Override
    public void nodeTableStatisticsUpdated(Node node, List<NodeTableStatistics> tableStatsList) {

        FlowTableStatisticsUpdateBuilder flowTableStatisticsUpdateBuilder = new FlowTableStatisticsUpdateBuilder();

        List<FlowTableAndStatisticsMap>  flowTableStatistics = toOdFlowTableStatistics(tableStatsList);
        flowTableStatisticsUpdateBuilder.setFlowTableAndStatisticsMap(flowTableStatistics);
        flowTableStatisticsUpdateBuilder.setMoreReplies(false);
        flowTableStatisticsUpdateBuilder.setTransactionId(null);
        flowTableStatisticsUpdateBuilder.setId(InventoryMapping.toNodeKey(node).getId());
        notifier.publish(flowTableStatisticsUpdateBuilder.build());
}

        @Override
    public void descriptionStatisticsUpdated(Node node, NodeDescription nodeDescription) {
            // TODO which *StatisticsUpdated interface should be used?

    }

    private List<FlowAndStatisticsMapList> toOdFlowsStatistics(List<FlowOnNode> flowsOnNode) {
        List<FlowAndStatisticsMapList> flowsStatistics = new ArrayList<>();
        for (FlowOnNode flowOnNode : flowsOnNode) {
            flowsStatistics.add(toOdFlowStatistics(flowOnNode));
        }
        return flowsStatistics;
    }

    private FlowAndStatisticsMapList toOdFlowStatistics(FlowOnNode flowOnNode) {
        FlowAndStatisticsMapListBuilder builder = new FlowAndStatisticsMapListBuilder();

        builder.setByteCount(toCounter64(flowOnNode.getByteCount()));
        builder.setPacketCount(toCounter64(flowOnNode.getPacketCount()));
        builder.setDuration(extractDuration(flowOnNode));
        builder.setMatch(FromSalConversionsUtils.toMatch(flowOnNode.getFlow().getMatch()));
        builder.setPriority((int)flowOnNode.getFlow().getPriority());
        builder.setHardTimeout((int)flowOnNode.getFlow().getHardTimeout());
        builder.setIdleTimeout((int)flowOnNode.getFlow().getIdleTimeout());
        //TODO: actions to instruction conversion
        builder.setInstructions(null);
        return builder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.duration.Duration extractDuration(FlowOnNode flowOnNode) {
        DurationBuilder builder = new DurationBuilder();
        builder.setNanosecond(new Counter32((long)flowOnNode.getDurationNanoseconds()));
        builder.setSecond(new Counter32((long)flowOnNode.getDurationSeconds()));
        return builder.build();
    }

    private Counter64 toCounter64(long num) {
        String byteCountStr = String.valueOf(num);
        BigInteger byteCountBigInt = new BigInteger(byteCountStr);
        return new Counter64(byteCountBigInt);
    }

    private List<FlowTableAndStatisticsMap> toOdFlowTableStatistics(List<NodeTableStatistics> tableStatsList) {

        List<FlowTableAndStatisticsMap> flowTableStatsMap = new ArrayList<FlowTableAndStatisticsMap>();
        for (NodeTableStatistics nodeTableStatistics : tableStatsList) {
            FlowTableAndStatisticsMapBuilder flowTableAndStatisticsMapBuilder = new FlowTableAndStatisticsMapBuilder();
            flowTableAndStatisticsMapBuilder.setActiveFlows(new Counter32((long) nodeTableStatistics.getActiveCount()));
            flowTableAndStatisticsMapBuilder.setPacketsLookedUp(toCounter64(nodeTableStatistics.getLookupCount()));
            flowTableAndStatisticsMapBuilder.setPacketsMatched(toCounter64(nodeTableStatistics.getMatchedCount()));
            flowTableAndStatisticsMapBuilder.setActiveFlows(new Counter32((long) nodeTableStatistics.getActiveCount()));
            flowTableAndStatisticsMapBuilder.setTableId(new TableId((short)nodeTableStatistics.getNodeTable().getID()));
            flowTableStatsMap.add(flowTableAndStatisticsMapBuilder.build());
        }

        return flowTableStatsMap;
    }

    private List<NodeConnectorStatisticsAndPortNumberMap> toOdNodeConnectorStatistics(
            List<NodeConnectorStatistics> ncStatsList) {
        List<NodeConnectorStatisticsAndPortNumberMap> nodeConnectorStatisticsList = new ArrayList<NodeConnectorStatisticsAndPortNumberMap>();
        for(NodeConnectorStatistics ofNodeConnectorStatistics : ncStatsList){
            NodeConnectorStatisticsAndPortNumberMapBuilder nodeConnectorStatisticsAndPortNumberMapBuilder = new NodeConnectorStatisticsAndPortNumberMapBuilder();

            nodeConnectorStatisticsAndPortNumberMapBuilder.setBytes(extractBytes(ofNodeConnectorStatistics));
            nodeConnectorStatisticsAndPortNumberMapBuilder.setCollisionCount(toBI(ofNodeConnectorStatistics.getCollisionCount()));
            nodeConnectorStatisticsAndPortNumberMapBuilder.setDuration(null);
            nodeConnectorStatisticsAndPortNumberMapBuilder.setPackets(extractPackets(ofNodeConnectorStatistics));
            nodeConnectorStatisticsAndPortNumberMapBuilder.setReceiveCrcError(toBI(ofNodeConnectorStatistics.getReceiveCRCErrorCount()));
            nodeConnectorStatisticsAndPortNumberMapBuilder.setReceiveDrops(toBI(ofNodeConnectorStatistics.getReceiveDropCount()));
            nodeConnectorStatisticsAndPortNumberMapBuilder.setReceiveErrors(toBI(ofNodeConnectorStatistics.getReceiveErrorCount()));
            nodeConnectorStatisticsAndPortNumberMapBuilder.setReceiveFrameError(toBI(ofNodeConnectorStatistics.getReceiveFrameErrorCount()));
            nodeConnectorStatisticsAndPortNumberMapBuilder.setReceiveOverRunError(toBI(ofNodeConnectorStatistics.getReceiveOverRunErrorCount()));
            nodeConnectorStatisticsAndPortNumberMapBuilder.setTransmitDrops(toBI(ofNodeConnectorStatistics.getTransmitDropCount()));
            nodeConnectorStatisticsAndPortNumberMapBuilder.setTransmitErrors(toBI(ofNodeConnectorStatistics.getTransmitErrorCount()));
            nodeConnectorStatisticsList.add(nodeConnectorStatisticsAndPortNumberMapBuilder.build());
        }

        return nodeConnectorStatisticsList;
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

}
