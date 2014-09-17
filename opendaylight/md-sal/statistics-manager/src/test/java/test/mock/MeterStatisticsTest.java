package test.mock;

import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatisticsManagerActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.MeterFeatures;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.StatisticsManagerTest;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MeterStatisticsTest extends StatisticsManagerTest {
    private final Object waitObject = new Object();

    @Test(timeout = 5000)
    public void addedMeterOnDemandStatsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        final StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        final Meter meter = getMeter();
        final InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, meter.getKey());

        final WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, meterII, meter);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, meterII, meter);
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        final ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<NodeMeterStatistics> meterStatsOptional = readTx.read(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class)).checkedGet();
        assertTrue(meterStatsOptional.isPresent());
        assertEquals(COUNTER_64_TEST_VALUE, meterStatsOptional.get().getMeterStatistics().getByteInCount());
        assertEquals(COUNTER_64_TEST_VALUE, meterStatsOptional.get().getMeterStatistics().getPacketInCount());
    }

    @Test(timeout = 5000)
    public void deletedMeterStatsRemovalTest() throws ExecutionException, InterruptedException, ReadFailedException {
        final StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        final Meter meter = getMeter();
        final InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, meter.getKey());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, meterII, meter);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, meterII, meter);
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<NodeMeterStatistics> meterStatsOptional = readTx.read(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class)).checkedGet();
        assertTrue(meterStatsOptional.isPresent());
        assertEquals(COUNTER_64_TEST_VALUE, meterStatsOptional.get().getMeterStatistics().getByteInCount());
        assertEquals(COUNTER_64_TEST_VALUE, meterStatsOptional.get().getMeterStatistics().getPacketInCount());

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, meterII);
        assertCommit(writeTx.submit());

        readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<Meter> meterOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, meterII).checkedGet();
        assertFalse(meterOptional.isPresent());
    }

    @Test(timeout = 23000)
    public void getAllStatsFromConnectedNodeTest() throws ExecutionException, InterruptedException {
        final StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

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

    private class ChangeListener implements DataChangeListener {

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }
    }
}

