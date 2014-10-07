package test.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityFlowStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityGroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityPortStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityQueueStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityTableStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupDescStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.features.GroupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.StatisticsManagerTest;

public class StatCollectorTest extends StatisticsManagerTest {
    private final Object waitObject = new Object();

    @Test(timeout = 200000)
    public void getAllFlowStatsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        setupStatisticsManager();

        addFlowCapableNodeWithFeatures(s1Key, false, FlowFeatureCapabilityFlowStats.class);

        final Flow flow = getFlow();

        final InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(flow.getTableId()));

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                tableII.child(Flow.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        final ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<Table> tableOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId()))).checkedGet();
        assertTrue(tableOptional.isPresent());
        final FlowStatisticsData flowStats = tableOptional.get().getFlow().get(0).getAugmentation(FlowStatisticsData.class);
        assertTrue(flowStats != null);
        assertEquals(COUNTER_64_TEST_VALUE, flowStats.getFlowStatistics().getByteCount());
    }

    @Test(timeout = 200000)
    public void getAllGroupStatsFeatureNotAdvertisedTest() throws ExecutionException, InterruptedException {
        setupStatisticsManager();

        addFlowCapableNodeWithFeatures(s1Key, true);

        final InstanceIdentifier<Group> groupII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Group.class, getGroup().getKey());
        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                groupII.augmentation(NodeGroupStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<Group> optionalGroup = readTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).augmentation(FlowCapableNode.class)
                .child(Group.class, getGroup().getKey())).get();

        assertTrue(optionalGroup.isPresent());
        assertTrue(optionalGroup.get().getAugmentation(NodeGroupDescStats.class) != null);
        final NodeGroupStatistics groupStats = optionalGroup.get().getAugmentation(NodeGroupStatistics.class);
        assertTrue(groupStats != null);
        assertEquals(COUNTER_64_TEST_VALUE, groupStats.getGroupStatistics().getByteCount());

        readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<GroupFeatures> optionalGroupFeatures = readTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).augmentation(NodeGroupFeatures.class).child(GroupFeatures.class)).get();
        assertTrue(optionalGroupFeatures.isPresent());
        assertEquals(1, optionalGroupFeatures.get().getMaxGroups().size());
        assertEquals(MAX_GROUPS_TEST_VALUE, optionalGroupFeatures.get().getMaxGroups().get(0));
    }

    @Test(timeout = 200000)
    public void getAllGroupStatsFeatureAdvertisedTest() throws ExecutionException, InterruptedException {
        setupStatisticsManager();

        addFlowCapableNodeWithFeatures(s1Key, false, FlowFeatureCapabilityGroupStats.class);

        final InstanceIdentifier<Group> groupII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Group.class, getGroup().getKey());
        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                groupII.augmentation(NodeGroupStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<Group> optionalGroup = readTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).augmentation(FlowCapableNode.class)
                .child(Group.class, getGroup().getKey())).get();

        assertTrue(optionalGroup.isPresent());
        assertTrue(optionalGroup.get().getAugmentation(NodeGroupDescStats.class) != null);
        final NodeGroupStatistics groupStats = optionalGroup.get().getAugmentation(NodeGroupStatistics.class);
        assertTrue(groupStats != null);
        assertEquals(COUNTER_64_TEST_VALUE, groupStats.getGroupStatistics().getByteCount());

        readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<GroupFeatures> optionalGroupFeatures = readTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).augmentation(NodeGroupFeatures.class).child(GroupFeatures.class)).get();
        assertTrue(optionalGroupFeatures.isPresent());
        assertEquals(1, optionalGroupFeatures.get().getMaxGroups().size());
        assertEquals(MAX_GROUPS_TEST_VALUE, optionalGroupFeatures.get().getMaxGroups().get(0));
    }

    @Test(timeout = 200000)
    public void getAllMeterStatsTest() throws ExecutionException, InterruptedException {
        setupStatisticsManager();

        addFlowCapableNodeWithFeatures(s1Key, true);

        final InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, getMeter().getKey());
        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<Meter> optionalMeter = readTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).augmentation(FlowCapableNode.class)
                .child(Meter.class, getMeter().getKey())).get();

        assertTrue(optionalMeter.isPresent());
        assertTrue(optionalMeter.get().getAugmentation(NodeMeterConfigStats.class) != null);
        final NodeMeterStatistics meterStats = optionalMeter.get().getAugmentation(NodeMeterStatistics.class);
        assertTrue(meterStats != null);
        assertEquals(COUNTER_64_TEST_VALUE, meterStats.getMeterStatistics().getByteInCount());
        assertEquals(COUNTER_64_TEST_VALUE, meterStats.getMeterStatistics().getPacketInCount());

        readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<MeterFeatures> optionalMeterFeautures = readTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).augmentation(NodeMeterFeatures.class).child(MeterFeatures.class)).get();
        assertTrue(optionalMeterFeautures.isPresent());
        assertEquals(COUNTER_32_TEST_VALUE, optionalMeterFeautures.get().getMaxMeter());
    }

    @Test(timeout = 200000)
    public void getAllQueueStatsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        setupStatisticsManager();

        addFlowCapableNodeWithFeatures(s1Key, false, FlowFeatureCapabilityQueueStats.class);

        final NodeConnectorBuilder ncBuilder = new NodeConnectorBuilder();
        final FlowCapableNodeConnectorBuilder fcncBuilder = new FlowCapableNodeConnectorBuilder();
        ncBuilder.setKey(new NodeConnectorKey(getNodeConnectorId()));
        ncBuilder.addAugmentation(FlowCapableNodeConnector.class, fcncBuilder.build());

        final InstanceIdentifier<NodeConnector> nodeConnectorII = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key)
                .child(NodeConnector.class, ncBuilder.getKey());

        final WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, nodeConnectorII, ncBuilder.build());
        final InstanceIdentifier<Queue> queueII = nodeConnectorII.augmentation(FlowCapableNodeConnector.class)
                .child(Queue.class, getQueue().getKey());
        final QueueBuilder qBuilder = new QueueBuilder(getQueue());
        writeTx.put(LogicalDatastoreType.OPERATIONAL, queueII, qBuilder.build());
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                queueII.augmentation(FlowCapableNodeConnectorQueueStatisticsData.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        final ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<Queue> queueOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, queueII).checkedGet();
        assertTrue(queueOptional.isPresent());
        final FlowCapableNodeConnectorQueueStatisticsData queueStats =
                queueOptional.get().getAugmentation(FlowCapableNodeConnectorQueueStatisticsData.class);
        assertTrue(queueStats != null);
        assertEquals(COUNTER_64_TEST_VALUE,
                queueStats.getFlowCapableNodeConnectorQueueStatistics().getTransmittedBytes());
    }

    @Test(timeout = 200000)
    public void getAllPortStatsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        setupStatisticsManager();

        addFlowCapableNodeWithFeatures(s1Key, false, FlowFeatureCapabilityPortStats.class);

        final InstanceIdentifier<NodeConnector> nodeConnectorII = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).child(NodeConnector.class, new NodeConnectorKey(getNodeConnectorId()));

        NodeConnectorBuilder ncBuilder = new NodeConnectorBuilder();
        ncBuilder.setKey(new NodeConnectorKey(getNodeConnectorId()));
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, nodeConnectorII, ncBuilder.build());
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                nodeConnectorII.augmentation(FlowCapableNodeConnectorStatisticsData.class),
                new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        final ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<FlowCapableNodeConnectorStatisticsData> flowCapableNodeConnectorStatisticsDataOptional =
                readTx.read(LogicalDatastoreType.OPERATIONAL,
                        nodeConnectorII.augmentation(FlowCapableNodeConnectorStatisticsData.class)).checkedGet();
        assertTrue(flowCapableNodeConnectorStatisticsDataOptional.isPresent());
        assertEquals(BIG_INTEGER_TEST_VALUE,
                flowCapableNodeConnectorStatisticsDataOptional.get().getFlowCapableNodeConnectorStatistics()
                        .getReceiveDrops());
        assertEquals(BIG_INTEGER_TEST_VALUE,
                flowCapableNodeConnectorStatisticsDataOptional.get().getFlowCapableNodeConnectorStatistics()
                        .getCollisionCount());
    }

    @Test(timeout = 200000)
    public void getAllTableStatsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        setupStatisticsManager();

        addFlowCapableNodeWithFeatures(s1Key, false, FlowFeatureCapabilityTableStats.class);

        final TableId tableId = getTableId();
        final InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(tableId.getValue()));

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                tableII.augmentation(FlowTableStatisticsData.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        final ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<FlowTableStatisticsData> flowTableStatisticsDataOptional = readTx.read(
                LogicalDatastoreType.OPERATIONAL, tableII.augmentation(FlowTableStatisticsData.class)).checkedGet();
        assertTrue(flowTableStatisticsDataOptional.isPresent());
        assertEquals(COUNTER_32_TEST_VALUE,
                flowTableStatisticsDataOptional.get().getFlowTableStatistics().getActiveFlows());
        assertEquals(COUNTER_64_TEST_VALUE,
                flowTableStatisticsDataOptional.get().getFlowTableStatistics().getPacketsLookedUp());
    }

    public class ChangeListener implements DataChangeListener {

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }
    }
}
