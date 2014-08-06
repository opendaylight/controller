package test.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterConfigStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import test.mock.util.NotificationProviderServiceHelper;
import test.mock.util.ProviderContextMock;
import test.mock.util.RpcProviderRegistryMock;
import test.mock.util.StatisticsManagerTest;
import test.mock.util.TestUtils;

import com.google.common.base.Optional;

public class MeterStatisticsTest extends StatisticsManagerTest {
    private final NodeKey s1Key = new NodeKey(new NodeId("S1"));
    private final Object waitObject = new Object();
    private volatile boolean dataReceived = false;

    @Test
    public void addedMeterOnDemandStatsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        final NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        final RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        final ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        final StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        final Meter meter = TestUtils.getMeter();
        final InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, meter.getKey());

        final WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, meterII, meter);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, meterII, meter);
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            while(!dataReceived) {
                waitObject.wait(1000);
            }
        }

        final ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<NodeMeterStatistics> meterStatsOptional = readTx.read(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class)).checkedGet();
        assertTrue(meterStatsOptional.isPresent());
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, meterStatsOptional.get().getMeterStatistics().getByteInCount());
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, meterStatsOptional.get().getMeterStatistics().getPacketInCount());
    }

    @Test
    public void deletedMeterStatsRemovalTest() throws ExecutionException, InterruptedException, ReadFailedException {
        final NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        final RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        final ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        final StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        final Meter meter = TestUtils.getMeter();
        final InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, meter.getKey());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, meterII, meter);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, meterII, meter);
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            while(!dataReceived) {
                waitObject.wait(1000);
            }
        }
        dataReceived = false;

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<NodeMeterStatistics> meterStatsOptional = readTx.read(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class)).checkedGet();
        assertTrue(meterStatsOptional.isPresent());
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, meterStatsOptional.get().getMeterStatistics().getByteInCount());
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, meterStatsOptional.get().getMeterStatistics().getPacketInCount());

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, meterII);
        assertCommit(writeTx.submit());

        synchronized (waitObject) {
            while(!dataReceived) {
                waitObject.wait(1000);
            }
        }

        readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<Meter> meterOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, meterII).checkedGet();
        assertFalse(meterOptional.isPresent());
    }

    @Test
    public void getAllStatsFromConnectedNodeTest() throws ExecutionException, InterruptedException {
        final NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        final RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        final ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        final StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNodeWithFeatures(s1Key, true);

        final InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, TestUtils.getMeter().getKey());
        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            while(!dataReceived) {
                waitObject.wait(21000);
            }
        }

        final ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<Meter> optionalMeter = readTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).augmentation(FlowCapableNode.class)
                .child(Meter.class, TestUtils.getMeter().getKey())).get();

        assertTrue(optionalMeter.isPresent());
        assertTrue(optionalMeter.get().getAugmentation(NodeMeterConfigStats.class) != null);
        final NodeMeterStatistics meterStats = optionalMeter.get().getAugmentation(NodeMeterStatistics.class);
        assertTrue(meterStats != null);
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, meterStats.getMeterStatistics().getByteInCount());
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, meterStats.getMeterStatistics().getPacketInCount());
    }

    private class ChangeListener implements DataChangeListener {

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            synchronized (waitObject) {
                dataReceived = true;
                waitObject.notify();
            }
        }
    }
}
