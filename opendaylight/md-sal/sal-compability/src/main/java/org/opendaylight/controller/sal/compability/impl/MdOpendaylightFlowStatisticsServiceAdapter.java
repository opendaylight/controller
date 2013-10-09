package org.opendaylight.controller.sal.compability.impl;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.compability.FromSalConversionsUtils;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeTable;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.reader.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev130819.flow.statistics.Duration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdOpendaylightFlowStatisticsServiceAdapter implements IPluginInReadService {

    private static final Logger LOG = LoggerFactory.getLogger(MdOpendaylightFlowStatisticsServiceAdapter.class);
    private OpendaylightFlowStatisticsService opendaylightFlowStatisticsService;

    @Override
    public FlowOnNode readFlow(Node node, Flow flow, boolean cached) {
        FlowOnNode result = null;

        GetFlowStatisticsInput getFlowStatisticsInput = FromSalConversionsUtils.flowStatisticsInputFrom(flow);
        Future<RpcResult<GetFlowStatisticsOutput>> futureStatisticsOutput = opendaylightFlowStatisticsService
                .getFlowStatistics(getFlowStatisticsInput);

        RpcResult<GetFlowStatisticsOutput> rpcResultStatisticsOutput;
        GetFlowStatisticsOutput getFlowStatisticsOutput;
        try {
            rpcResultStatisticsOutput = futureStatisticsOutput.get();
            if (rpcResultStatisticsOutput != null) {
                getFlowStatisticsOutput = rpcResultStatisticsOutput.getResult();
                if (getFlowStatisticsOutput != null) {

                    long byteCount = getFlowStatisticsOutput.getByteCount().getValue().longValue();

                    Duration duration = getFlowStatisticsOutput.getDuration();
                    int nanoseconds = duration.getNanosecond().getValue().intValue();
                    int seconds = duration.getSecond().getValue().intValue();
                    long packetCount = getFlowStatisticsOutput.getPacketCount().getValue().longValue();

                    result = new FlowOnNode(flow);
                    result.setByteCount(byteCount);
                    result.setDurationNanoseconds(nanoseconds);
                    result.setDurationSeconds(seconds);
                    result.setPacketCount(packetCount);
                    return result;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Read flow not processed", e);
        }
        return null;
    }

    @Override
    public List<FlowOnNode> readAllFlow(Node node, boolean cached) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NodeDescription readDescription(Node node, boolean cached) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NodeConnectorStatistics readNodeConnector(NodeConnector connector, boolean cached) {
        NodeConnectorStatistics result = null;
        GetNodeConnectorStatisticsInput getNodeConnectorStatisticsInput = FromSalConversionsUtils
                .nodeConnectorStatisticsFrom(connector);
        Future<RpcResult<GetNodeConnectorStatisticsOutput>> future = opendaylightFlowStatisticsService
                .getNodeConnectorStatistics(getNodeConnectorStatisticsInput);
        try {
            RpcResult<GetNodeConnectorStatisticsOutput> rpcResult = future.get();
            if (rpcResult != null) {
                GetNodeConnectorStatisticsOutput getNodeConnectorStatisticsOutput = rpcResult.getResult();

                if (getNodeConnectorStatisticsOutput != null) {
                    result = new NodeConnectorStatistics();

                    long colisionCount = getNodeConnectorStatisticsOutput.getCollisionCount().longValue();
                    long receiveCrcErrorCount = getNodeConnectorStatisticsOutput.getReceiveCrcError().longValue();
                    long receiveFrameErrorCount = getNodeConnectorStatisticsOutput.getReceiveFrameError().longValue();
                    long receiveOverRunError = getNodeConnectorStatisticsOutput.getReceiveOverRunError().longValue();

                    long receiveDropCount = getNodeConnectorStatisticsOutput.getReceiveDrops().longValue();
                    long receiveErrorCount = getNodeConnectorStatisticsOutput.getReceiveErrors().longValue();
                    long receivePacketCount = getNodeConnectorStatisticsOutput.getPackets().getReceived().longValue();
                    long receivedByteCount = getNodeConnectorStatisticsOutput.getBytes().getReceived().longValue();

                    long transmitDropCount = getNodeConnectorStatisticsOutput.getTransmitDrops().longValue();
                    long transmitErrorCount = getNodeConnectorStatisticsOutput.getTransmitErrors().longValue();
                    long transmitPacketCount = getNodeConnectorStatisticsOutput.getPackets().getTransmitted()
                            .longValue();
                    long transmitByteCount = getNodeConnectorStatisticsOutput.getBytes().getTransmitted().longValue();

                    result.setCollisionCount(colisionCount);
                    result.setReceiveByteCount(receivedByteCount);
                    result.setReceiveCRCErrorCount(receiveCrcErrorCount);
                    result.setReceiveDropCount(receiveDropCount);
                    result.setReceiveErrorCount(receiveErrorCount);
                    result.setReceiveFrameErrorCount(receiveFrameErrorCount);
                    result.setReceiveOverRunErrorCount(receiveOverRunError);
                    result.setReceivePacketCount(receivePacketCount);
                    result.setTransmitByteCount(transmitByteCount);
                    result.setTransmitDropCount(transmitDropCount);
                    result.setTransmitErrorCount(transmitErrorCount);
                    result.setTransmitPacketCount(transmitPacketCount);
                    return result;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Read node connector not processed", e);
        }

        return result;

    }

    @Override
    public List<NodeConnectorStatistics> readAllNodeConnector(Node node, boolean cached) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public NodeTableStatistics readNodeTable(NodeTable table, boolean cached) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<NodeTableStatistics> readAllNodeTable(Node node, boolean cached) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getTransmitRate(NodeConnector connector) {
        // TODO Auto-generated method stub
        return 0;
    }

    public OpendaylightFlowStatisticsService getOpendaylightFlowStatisticsService() {
        return opendaylightFlowStatisticsService;
    }

    public void setOpendaylightFlowStatisticsService(OpendaylightFlowStatisticsService opendaylightFlowStatisticsService) {
        this.opendaylightFlowStatisticsService = opendaylightFlowStatisticsService;
    }

}
