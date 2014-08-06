package test.mock.util;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetAllGroupStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupDescriptionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupFeaturesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupFeaturesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupFeaturesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupStatisticsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GetGroupStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupDescStatsUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.GroupStatisticsUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.OpendaylightGroupStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.desc.stats.reply.GroupDescStatsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.reply.GroupStatsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class OpendaylightGroupStatisticsServiceMock implements OpendaylightGroupStatisticsService {
    NotificationProviderServiceHelper notifService;
    AtomicLong transNum = new AtomicLong();

    public OpendaylightGroupStatisticsServiceMock(NotificationProviderServiceHelper notifService) {
        this.notifService = notifService;
    }

    @Override
    public Future<RpcResult<GetAllGroupStatisticsOutput>> getAllGroupStatistics(GetAllGroupStatisticsInput input) {
        GetAllGroupStatisticsOutputBuilder builder = new GetAllGroupStatisticsOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(transNum.incrementAndGet()));
        builder.setTransactionId(transId);
        List<GroupStats> groupStats = new ArrayList<>();
        GroupStatsBuilder gsBuilder = new GroupStatsBuilder();
        GroupStatisticsUpdatedBuilder gsuBuilder = new GroupStatisticsUpdatedBuilder();
        gsBuilder.setKey(new GroupStatsKey(TestUtils.getGroup().getGroupId()));
        gsBuilder.setByteCount(TestUtils.COUNTER_64_TEST_VALUE);
        groupStats.add(gsBuilder.build());
        builder.setGroupStats(groupStats);
        gsuBuilder.setTransactionId(transId);
        gsuBuilder.setMoreReplies(false);
        gsuBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        gsuBuilder.setGroupStats(groupStats);
        notifService.pushDelayedNotification(gsuBuilder.build(), 500);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetGroupDescriptionOutput>> getGroupDescription(GetGroupDescriptionInput input) {
        GetGroupDescriptionOutputBuilder builder = new GetGroupDescriptionOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(transNum.incrementAndGet()));
        builder.setTransactionId(transId);
        List<GroupDescStats> groupDescStats = new ArrayList<>();
        GroupDescStatsUpdatedBuilder gdsuBuilder = new GroupDescStatsUpdatedBuilder();
        GroupDescStatsBuilder gdsBuilder = new GroupDescStatsBuilder();
        gdsBuilder.setKey(new GroupDescStatsKey(TestUtils.getGroup().getGroupId()));
        gdsBuilder.setBuckets(TestUtils.getGroup().getBuckets());
        gdsBuilder.setContainerName(TestUtils.getGroup().getContainerName());
        gdsBuilder.setGroupName(TestUtils.getGroup().getGroupName());
        gdsBuilder.setGroupType(TestUtils.getGroup().getGroupType());
        groupDescStats.add(gdsBuilder.build());
        builder.setGroupDescStats(groupDescStats);
        gdsuBuilder.setTransactionId(transId);
        gdsuBuilder.setMoreReplies(false);
        gdsuBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        gdsuBuilder.setGroupDescStats(groupDescStats);
        notifService.pushDelayedNotification(gdsuBuilder.build(), 100);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetGroupFeaturesOutput>> getGroupFeatures(GetGroupFeaturesInput input) {
        GetGroupFeaturesOutputBuilder builder = new GetGroupFeaturesOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(transNum.incrementAndGet()));
        builder.setTransactionId(transId);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetGroupStatisticsOutput>> getGroupStatistics(GetGroupStatisticsInput input) {
        GetGroupStatisticsOutputBuilder builder = new GetGroupStatisticsOutputBuilder();
        TransactionId transId = new TransactionId(BigInteger.valueOf(transNum.incrementAndGet()));
        builder.setTransactionId(transId);
        GroupStatsBuilder gsBuilder = new GroupStatsBuilder();
        List<GroupStats> groupStats = new ArrayList<>();
        gsBuilder.setKey(new GroupStatsKey(input.getGroupId()));
        gsBuilder.setByteCount(TestUtils.COUNTER_64_TEST_VALUE);
        groupStats.add(gsBuilder.build());
        GroupStatisticsUpdatedBuilder gsuBuilder = new GroupStatisticsUpdatedBuilder();
        gsuBuilder.setTransactionId(transId);
        gsuBuilder.setMoreReplies(false);
        gsuBuilder.setId(input.getNode().getValue().firstKeyOf(Node.class, NodeKey.class).getId());
        gsuBuilder.setGroupStats(groupStats);
        notifService.pushDelayedNotification(gsuBuilder.build(), 100);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
