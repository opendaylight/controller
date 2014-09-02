package test.mock.util;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.GetFlowTablesStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.util.concurrent.Future;

public class OpendaylightFlowTableStatisticsServiceMock implements OpendaylightFlowTableStatisticsService {
    NotificationProviderServiceMock notifService = new NotificationProviderServiceMock();

    public OpendaylightFlowTableStatisticsServiceMock(NotificationProviderServiceMock notifService) {
        this.notifService = notifService;
    }

    @Override
    public Future<RpcResult<GetFlowTablesStatisticsOutput>> getFlowTablesStatistics(GetFlowTablesStatisticsInput input) {
        GetFlowTablesStatisticsOutputBuilder builder = new GetFlowTablesStatisticsOutputBuilder();
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
