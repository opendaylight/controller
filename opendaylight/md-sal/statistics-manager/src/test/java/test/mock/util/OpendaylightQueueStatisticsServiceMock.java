package test.mock.util;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromAllPortsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromGivenPortInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromGivenPortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetAllQueuesStatisticsFromGivenPortOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetQueueStatisticsFromGivenPortInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetQueueStatisticsFromGivenPortOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.GetQueueStatisticsFromGivenPortOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.OpendaylightQueueStatisticsService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.util.concurrent.Future;

public class OpendaylightQueueStatisticsServiceMock implements OpendaylightQueueStatisticsService {
    NotificationProviderServiceMock notifService = new NotificationProviderServiceMock();

    public OpendaylightQueueStatisticsServiceMock(NotificationProviderServiceMock notifService) {
        this.notifService = notifService;
    }

    @Override
    public Future<RpcResult<GetAllQueuesStatisticsFromAllPortsOutput>> getAllQueuesStatisticsFromAllPorts(GetAllQueuesStatisticsFromAllPortsInput input) {
        GetAllQueuesStatisticsFromAllPortsOutputBuilder builder = new GetAllQueuesStatisticsFromAllPortsOutputBuilder();
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetAllQueuesStatisticsFromGivenPortOutput>> getAllQueuesStatisticsFromGivenPort(GetAllQueuesStatisticsFromGivenPortInput input) {
        GetAllQueuesStatisticsFromGivenPortOutputBuilder builder = new GetAllQueuesStatisticsFromGivenPortOutputBuilder();
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetQueueStatisticsFromGivenPortOutput>> getQueueStatisticsFromGivenPort(GetQueueStatisticsFromGivenPortInput input) {
        GetQueueStatisticsFromGivenPortOutputBuilder builder = new GetQueueStatisticsFromGivenPortOutputBuilder();
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
