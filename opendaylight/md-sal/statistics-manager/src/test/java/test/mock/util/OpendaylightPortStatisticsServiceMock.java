package test.mock.util;

import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetAllNodeConnectorsStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetNodeConnectorStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetNodeConnectorStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.GetNodeConnectorStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.NodeConnectorStatisticsUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.OpendaylightPortStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.node.connector.statistics.and.port.number.map.NodeConnectorStatisticsAndPortNumberMapKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class OpendaylightPortStatisticsServiceMock implements OpendaylightPortStatisticsService {
    NotificationProviderServiceHelper notifService;

    public OpendaylightPortStatisticsServiceMock(NotificationProviderServiceHelper notifService) {
        this.notifService = notifService;
    }

    @Override
    public Future<RpcResult<GetAllNodeConnectorsStatisticsOutput>> getAllNodeConnectorsStatistics(GetAllNodeConnectorsStatisticsInput input) {
        GetAllNodeConnectorsStatisticsOutputBuilder builder = new GetAllNodeConnectorsStatisticsOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        NodeConnectorStatisticsUpdateBuilder ncsuBuilder = new NodeConnectorStatisticsUpdateBuilder();
        NodeConnectorStatisticsAndPortNumberMapBuilder ncsapnmBuilder = new NodeConnectorStatisticsAndPortNumberMapBuilder();
        List<NodeConnectorStatisticsAndPortNumberMap> nodeConnectorStatisticsAndPortNumberMaps = new ArrayList<>();
        ncsapnmBuilder.setKey(new NodeConnectorStatisticsAndPortNumberMapKey(StatisticsManagerTest.getNodeConnectorId()));
        ncsapnmBuilder.setReceiveDrops(StatisticsManagerTest.BIG_INTEGER_TEST_VALUE);
        nodeConnectorStatisticsAndPortNumberMaps.add(ncsapnmBuilder.build());
        ncsuBuilder.setTransactionId(transId);
        ncsuBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        ncsuBuilder.setNodeConnectorStatisticsAndPortNumberMap(nodeConnectorStatisticsAndPortNumberMaps);
        ncsuBuilder.setMoreReplies(true);
        notifService.pushDelayedNotification(ncsuBuilder.build(), 0); // 1st notification
        ncsuBuilder.setMoreReplies(false);
        ncsapnmBuilder.setCollisionCount(StatisticsManagerTest.BIG_INTEGER_TEST_VALUE);
        nodeConnectorStatisticsAndPortNumberMaps.clear();
        nodeConnectorStatisticsAndPortNumberMaps.add(ncsapnmBuilder.build());
        ncsuBuilder.setNodeConnectorStatisticsAndPortNumberMap(nodeConnectorStatisticsAndPortNumberMaps);
        notifService.pushDelayedNotification(ncsuBuilder.build(), 10); // 2nd notification
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetNodeConnectorStatisticsOutput>> getNodeConnectorStatistics(GetNodeConnectorStatisticsInput input) {
        GetNodeConnectorStatisticsOutputBuilder builder = new GetNodeConnectorStatisticsOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
