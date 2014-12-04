package test.mock.util;

import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterConfigStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetAllMeterStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterFeaturesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterFeaturesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterFeaturesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.GetMeterStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterConfigStatsUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterFeaturesUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.MeterStatisticsUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.OpendaylightMeterStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.config.stats.reply.MeterConfigStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.statistics.reply.MeterStatsKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class OpendaylightMeterStatisticsServiceMock implements OpendaylightMeterStatisticsService {
    NotificationProviderServiceHelper notifService;

    public OpendaylightMeterStatisticsServiceMock(NotificationProviderServiceHelper notifService) {
        this.notifService = notifService;
    }

    @Override
    public Future<RpcResult<GetAllMeterConfigStatisticsOutput>> getAllMeterConfigStatistics(GetAllMeterConfigStatisticsInput input) {
        GetAllMeterConfigStatisticsOutputBuilder builder = new GetAllMeterConfigStatisticsOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        List<MeterConfigStats> meterConfigStats = new ArrayList<>();
        MeterConfigStatsBuilder mcsBuilder = new MeterConfigStatsBuilder();
        mcsBuilder.setMeterId(StatisticsManagerTest.getMeter().getMeterId());
        mcsBuilder.setMeterName(StatisticsManagerTest.getMeter().getMeterName());
        mcsBuilder.setContainerName(StatisticsManagerTest.getMeter().getContainerName());
        meterConfigStats.add(mcsBuilder.build());
        builder.setMeterConfigStats(meterConfigStats);
        MeterConfigStatsUpdatedBuilder mscuBuilder = new MeterConfigStatsUpdatedBuilder();
        mscuBuilder.setTransactionId(transId);
        mscuBuilder.setMoreReplies(false);
        mscuBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        mscuBuilder.setMeterConfigStats(meterConfigStats);
        notifService.pushDelayedNotification(mscuBuilder.build(), 100);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetAllMeterStatisticsOutput>> getAllMeterStatistics(GetAllMeterStatisticsInput input) {
        GetAllMeterStatisticsOutputBuilder builder = new GetAllMeterStatisticsOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        MeterStatsBuilder msBuilder = new MeterStatsBuilder();
        msBuilder.setByteInCount(StatisticsManagerTest.COUNTER_64_TEST_VALUE);
        msBuilder.setPacketInCount(StatisticsManagerTest.COUNTER_64_TEST_VALUE);
        msBuilder.setKey(new MeterStatsKey(StatisticsManagerTest.getMeter().getMeterId()));
        List<MeterStats> meterStats = new ArrayList<>();
        meterStats.add(msBuilder.build());
        MeterStatisticsUpdatedBuilder msuBuilder = new MeterStatisticsUpdatedBuilder();
        msuBuilder.setTransactionId(transId);
        msuBuilder.setMoreReplies(false);
        msuBuilder.setMeterStats(meterStats);
        msuBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        notifService.pushDelayedNotification(msuBuilder.build(), 100);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetMeterFeaturesOutput>> getMeterFeatures(GetMeterFeaturesInput input) {
        GetMeterFeaturesOutputBuilder builder = new GetMeterFeaturesOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        MeterFeaturesUpdatedBuilder mfuBuilder = new MeterFeaturesUpdatedBuilder();
        mfuBuilder.setTransactionId(transId);
        mfuBuilder.setMoreReplies(false);
        mfuBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        mfuBuilder.setMaxMeter(StatisticsManagerTest.COUNTER_32_TEST_VALUE);
        notifService.pushDelayedNotification(mfuBuilder.build(), 100);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetMeterStatisticsOutput>> getMeterStatistics(GetMeterStatisticsInput input) {
        GetMeterStatisticsOutputBuilder builder = new GetMeterStatisticsOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(TestUtils.getNewTransactionId()));
        builder.setTransactionId(transId);
        MeterStatsBuilder msBuilder = new MeterStatsBuilder();
        msBuilder.setKey(new MeterStatsKey(input.getMeterId()));
        msBuilder.setByteInCount(StatisticsManagerTest.COUNTER_64_TEST_VALUE);
        msBuilder.setPacketInCount(StatisticsManagerTest.COUNTER_64_TEST_VALUE);
        List<MeterStats> meterStats = new ArrayList<>();
        meterStats.add(msBuilder.build());
        MeterStatisticsUpdatedBuilder msuBuilder = new MeterStatisticsUpdatedBuilder();
        msuBuilder.setTransactionId(transId);
        msuBuilder.setMoreReplies(false);
        msuBuilder.setMeterStats(meterStats);
        msuBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        notifService.pushDelayedNotification(msuBuilder.build(), 100);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
