package test.mock.util;

import com.google.common.util.concurrent.Futures;
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class OpendaylightFlowStatisticsServiceMock implements OpendaylightFlowStatisticsService {
    NotificationProviderServiceMock notifService = new NotificationProviderServiceMock();

    public OpendaylightFlowStatisticsServiceMock(NotificationProviderServiceMock notifService) {
        this.notifService = notifService;
    }

    @Override
    public Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutput>> getAggregateFlowStatisticsFromFlowTableForAllFlows(GetAggregateFlowStatisticsFromFlowTableForAllFlowsInput input) {
        GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutputBuilder builder = new GetAggregateFlowStatisticsFromFlowTableForAllFlowsOutputBuilder();
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput>> getAggregateFlowStatisticsFromFlowTableForGivenMatch(GetAggregateFlowStatisticsFromFlowTableForGivenMatchInput input) {
        GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutputBuilder builder = new GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutputBuilder();
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetAllFlowStatisticsFromFlowTableOutput>> getAllFlowStatisticsFromFlowTable(GetAllFlowStatisticsFromFlowTableInput input) {
        GetAllFlowStatisticsFromFlowTableOutputBuilder builder = new GetAllFlowStatisticsFromFlowTableOutputBuilder();
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetAllFlowsStatisticsFromAllFlowTablesOutput>> getAllFlowsStatisticsFromAllFlowTables(GetAllFlowsStatisticsFromAllFlowTablesInput input) {
        GetAllFlowsStatisticsFromAllFlowTablesOutputBuilder builder = new GetAllFlowsStatisticsFromAllFlowTablesOutputBuilder();
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetFlowStatisticsFromFlowTableOutput>> getFlowStatisticsFromFlowTable(GetFlowStatisticsFromFlowTableInput input) {
        GetFlowStatisticsFromFlowTableOutputBuilder builder = new GetFlowStatisticsFromFlowTableOutputBuilder();
        builder.setTransactionId(new TransactionId(BigInteger.valueOf(1)));
        List<FlowAndStatisticsMapList> flowAndStatisticsMapLists = new ArrayList<>();
        FlowsStatisticsUpdateBuilder flowsStatisticsUpdateBuilder = new FlowsStatisticsUpdateBuilder();
        flowsStatisticsUpdateBuilder.setTransactionId(new TransactionId(BigInteger.valueOf(1)));
        flowsStatisticsUpdateBuilder.setMoreReplies(false);
        flowsStatisticsUpdateBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        FlowAndStatisticsMapList build = new FlowAndStatisticsMapListBuilder(input).setTableId(input.getTableId()).setContainerName(input.getContainerName())
                .setBarrier(input.isBarrier()).build();
        flowAndStatisticsMapLists.add(build);
        flowsStatisticsUpdateBuilder.setFlowAndStatisticsMapList(flowAndStatisticsMapLists);
        builder.setFlowAndStatisticsMapList(flowAndStatisticsMapLists);
        notifService.pushDelayedNotif(flowsStatisticsUpdateBuilder.build());
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
