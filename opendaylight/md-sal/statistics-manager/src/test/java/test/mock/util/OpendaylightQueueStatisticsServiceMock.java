package test.mock.util;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.QueueStatisticsUpdateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMapBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.queue.id.and.statistics.map.QueueIdAndStatisticsMapKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class OpendaylightQueueStatisticsServiceMock implements OpendaylightQueueStatisticsService {
    NotificationProviderServiceHelper notifService;
    AtomicLong transNum = new AtomicLong();

    public OpendaylightQueueStatisticsServiceMock(NotificationProviderServiceHelper notifService) {
        this.notifService = notifService;
    }

    @Override
    public Future<RpcResult<GetAllQueuesStatisticsFromAllPortsOutput>> getAllQueuesStatisticsFromAllPorts(GetAllQueuesStatisticsFromAllPortsInput input) {
        GetAllQueuesStatisticsFromAllPortsOutputBuilder builder = new GetAllQueuesStatisticsFromAllPortsOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        QueueStatisticsUpdateBuilder qsuBuilder = new QueueStatisticsUpdateBuilder();
        QueueIdAndStatisticsMapBuilder qiasmBuilder = new QueueIdAndStatisticsMapBuilder();
        List<QueueIdAndStatisticsMap> queueIdAndStatisticsMaps = new ArrayList<>();
        qsuBuilder.setMoreReplies(false);
        qsuBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        qsuBuilder.setTransactionId(transId);
        qiasmBuilder.setTransmittedBytes(StatisticsManagerTest.COUNTER_64_TEST_VALUE);
        qiasmBuilder.setKey(new QueueIdAndStatisticsMapKey(StatisticsManagerTest.getNodeConnectorId(), StatisticsManagerTest.getQueue().getQueueId()));
        queueIdAndStatisticsMaps.add(qiasmBuilder.build());
        qsuBuilder.setQueueIdAndStatisticsMap(queueIdAndStatisticsMaps);
        notifService.pushDelayedNotification(qsuBuilder.build(), 100);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetAllQueuesStatisticsFromGivenPortOutput>> getAllQueuesStatisticsFromGivenPort(GetAllQueuesStatisticsFromGivenPortInput input) {
        GetAllQueuesStatisticsFromGivenPortOutputBuilder builder = new GetAllQueuesStatisticsFromGivenPortOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetQueueStatisticsFromGivenPortOutput>> getQueueStatisticsFromGivenPort(GetQueueStatisticsFromGivenPortInput input) {
        GetQueueStatisticsFromGivenPortOutputBuilder builder = new GetQueueStatisticsFromGivenPortOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        QueueIdAndStatisticsMapBuilder qiasmBuilder = new QueueIdAndStatisticsMapBuilder();
        List<QueueIdAndStatisticsMap> queueIdAndStatisticsMaps = new ArrayList<>();
        qiasmBuilder.setKey(new QueueIdAndStatisticsMapKey(input.getNodeConnectorId(), input.getQueueId()));
        qiasmBuilder.setTransmittedBytes(StatisticsManagerTest.COUNTER_64_TEST_VALUE);
        queueIdAndStatisticsMaps.add(qiasmBuilder.build());
        QueueStatisticsUpdateBuilder qsuBuilder = new QueueStatisticsUpdateBuilder();
        qsuBuilder.setMoreReplies(false);
        qsuBuilder.setTransactionId(transId);
        qsuBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        qsuBuilder.setQueueIdAndStatisticsMap(queueIdAndStatisticsMaps);
        notifService.pushDelayedNotification(qsuBuilder.build(), 100);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
