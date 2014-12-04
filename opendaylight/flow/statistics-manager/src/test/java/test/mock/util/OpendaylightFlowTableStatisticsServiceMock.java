package test.mock.util;

import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.and.statistics.map.FlowTableAndStatisticsMapKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class OpendaylightFlowTableStatisticsServiceMock implements OpendaylightFlowTableStatisticsService {
    NotificationProviderServiceHelper notifService;

    public OpendaylightFlowTableStatisticsServiceMock(NotificationProviderServiceHelper notifService) {
        this.notifService = notifService;
    }

    @Override
    public Future<RpcResult<GetFlowTablesStatisticsOutput>> getFlowTablesStatistics(GetFlowTablesStatisticsInput input) {
        GetFlowTablesStatisticsOutputBuilder builder = new GetFlowTablesStatisticsOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        FlowTableStatisticsUpdateBuilder ftsBuilder = new FlowTableStatisticsUpdateBuilder();
        FlowTableAndStatisticsMapBuilder ftasmBuilder = new FlowTableAndStatisticsMapBuilder();
        List<FlowTableAndStatisticsMap> tableAndStatisticsMaps = new ArrayList<>();
        ftasmBuilder.setKey(new FlowTableAndStatisticsMapKey(StatisticsManagerTest.getTableId()));
        ftasmBuilder.setActiveFlows(StatisticsManagerTest.COUNTER_32_TEST_VALUE);
        tableAndStatisticsMaps.add(ftasmBuilder.build());
        ftsBuilder.setTransactionId(transId);
        ftsBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        ftsBuilder.setFlowTableAndStatisticsMap(tableAndStatisticsMaps);
        ftsBuilder.setMoreReplies(true);
        notifService.pushDelayedNotification(ftsBuilder.build(), 0); // 1st notification
        ftsBuilder.setMoreReplies(false);
        ftasmBuilder.setPacketsLookedUp(StatisticsManagerTest.COUNTER_64_TEST_VALUE);
        tableAndStatisticsMaps.clear();
        tableAndStatisticsMaps.add(ftasmBuilder.build());
        ftsBuilder.setFlowTableAndStatisticsMap(tableAndStatisticsMaps);
        notifService.pushDelayedNotification(ftsBuilder.build(), 0); // 2nd notification
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

}
