package test.mock.util;

import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.AggregateFlowStatisticsUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowsStatisticsUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForGivenMatchInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutputBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class OpendaylightFlowStatisticsServiceMock implements OpendaylightFlowStatisticsService {
    NotificationProviderServiceHelper notifService;

    public OpendaylightFlowStatisticsServiceMock(NotificationProviderServiceHelper notifService) {
        this.notifService = notifService;
    }

    @Override
    public Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput>> getAggregateFlowStatisticsFromFlowTableForAllFlows(GetAggregateFlowStatisticsFromFlowTableForAllFlowsInput input) {
        GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutputBuilder builder = new GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput>> getAggregateFlowStatisticsFromFlowTableForGivenMatch(GetAggregateFlowStatisticsFromFlowTableForGivenMatchInput input) {
        GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutputBuilder builder = new GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        AggregateFlowStatisticsUpdateBuilder afsuBuilder = new AggregateFlowStatisticsUpdateBuilder();
        afsuBuilder.setMoreReplies(false);
        afsuBuilder.setTransactionId(transId);
        afsuBuilder.setByteCount(StatisticsManagerTest.COUNTER_64_TEST_VALUE);
        afsuBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        notifService.pushDelayedNotification(afsuBuilder.build(), 100);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetAllFlowStatisticsFromFlowTableOutput>> getAllFlowStatisticsFromFlowTable(GetAllFlowStatisticsFromFlowTableInput input) {
        GetAllFlowStatisticsFromFlowTableOutputBuilder builder = new GetAllFlowStatisticsFromFlowTableOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetAllFlowsStatisticsFromAllFlowTablesOutput>> getAllFlowsStatisticsFromAllFlowTables(GetAllFlowsStatisticsFromAllFlowTablesInput input) {
        GetAllFlowsStatisticsFromAllFlowTablesOutputBuilder builder = new GetAllFlowsStatisticsFromAllFlowTablesOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        List<FlowAndStatisticsMapList> flowAndStatisticsMapLists = new ArrayList<>();
        FlowsStatisticsUpdateBuilder flowsStatisticsUpdateBuilder = new FlowsStatisticsUpdateBuilder();
        flowsStatisticsUpdateBuilder.setTransactionId(transId);
        flowsStatisticsUpdateBuilder.setMoreReplies(false);
        flowsStatisticsUpdateBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        FlowAndStatisticsMapListBuilder flowAndStatisticsMapListBuilder = new FlowAndStatisticsMapListBuilder(StatisticsManagerTest.getFlow());
        flowAndStatisticsMapListBuilder.setTableId(StatisticsManagerTest.getFlow().getTableId());
        flowAndStatisticsMapListBuilder.setContainerName(StatisticsManagerTest.getFlow().getContainerName());
        flowAndStatisticsMapListBuilder.setBarrier(StatisticsManagerTest.getFlow().isBarrier());
        flowAndStatisticsMapListBuilder.setByteCount(StatisticsManagerTest.COUNTER_64_TEST_VALUE);
        flowAndStatisticsMapLists.add(flowAndStatisticsMapListBuilder.build());
        flowsStatisticsUpdateBuilder.setFlowAndStatisticsMapList(flowAndStatisticsMapLists);
        notifService.pushDelayedNotification(flowsStatisticsUpdateBuilder.build(), 100);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetFlowStatisticsFromFlowTableOutput>> getFlowStatisticsFromFlowTable(GetFlowStatisticsFromFlowTableInput input) {
        GetFlowStatisticsFromFlowTableOutputBuilder builder = new GetFlowStatisticsFromFlowTableOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        List<FlowAndStatisticsMapList> flowAndStatisticsMapLists = new ArrayList<>();
        FlowsStatisticsUpdateBuilder flowsStatisticsUpdateBuilder = new FlowsStatisticsUpdateBuilder();
        flowsStatisticsUpdateBuilder.setTransactionId(transId);
        flowsStatisticsUpdateBuilder.setMoreReplies(false);
        flowsStatisticsUpdateBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        FlowAndStatisticsMapListBuilder flowAndStatisticsMapListBuilder = new FlowAndStatisticsMapListBuilder(input);
        flowAndStatisticsMapListBuilder.setTableId(input.getTableId());
        flowAndStatisticsMapListBuilder.setContainerName(input.getContainerName());
        flowAndStatisticsMapListBuilder.setBarrier(input.isBarrier());
        flowAndStatisticsMapListBuilder.setByteCount(StatisticsManagerTest.COUNTER_64_TEST_VALUE);
        flowAndStatisticsMapLists.add(flowAndStatisticsMapListBuilder.build());
        flowsStatisticsUpdateBuilder.setFlowAndStatisticsMapList(flowAndStatisticsMapLists);
        notifService.pushDelayedNotification(flowsStatisticsUpdateBuilder.build(), 100);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

}
