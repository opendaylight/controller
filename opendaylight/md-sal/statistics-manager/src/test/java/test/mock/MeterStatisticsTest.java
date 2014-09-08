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

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MeterStatisticsTest extends StatisticsManagerTest {
    private NodeKey s1Key = new NodeKey(new NodeId("S1"));
    private final Object waitObject = new Object();
    private volatile boolean dataReceived = false;

    @Test
    public void addedMeterOnDemandStatsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        Meter meter = TestUtils.getMeter();
        InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
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

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<NodeMeterStatistics> meterStatsOptional = readTx.read(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class)).checkedGet();
        assertTrue(meterStatsOptional.isPresent());
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, meterStatsOptional.get().getMeterStatistics().getByteInCount());
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, meterStatsOptional.get().getMeterStatistics().getPacketInCount());
    }

    @Test
    public void deletedMeterStatsRemovalTest() throws ExecutionException, InterruptedException, ReadFailedException {
        NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        Meter meter = TestUtils.getMeter();
        InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
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
        Optional<NodeMeterStatistics> meterStatsOptional = readTx.read(LogicalDatastoreType.OPERATIONAL,
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
        Optional<Meter> meterOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, meterII).checkedGet();
        assertFalse(meterOptional.isPresent());
    }

    @Test
    public void getAllStatsFromConnectedNodeTest() throws ExecutionException, InterruptedException {
        NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNodeWithFeatures(s1Key, true);

        InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, TestUtils.getMeter().getKey());
        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                meterII.augmentation(NodeMeterStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            while(!dataReceived) {
                waitObject.wait(21000);
            }
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<Meter> optionalMeter = readTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).augmentation(FlowCapableNode.class)
                .child(Meter.class, TestUtils.getMeter().getKey())).get();

        assertTrue(optionalMeter.isPresent());
        assertTrue(optionalMeter.get().getAugmentation(NodeMeterConfigStats.class) != null);
        NodeMeterStatistics meterStats = optionalMeter.get().getAugmentation(NodeMeterStatistics.class);
        assertTrue(meterStats != null);
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, meterStats.getMeterStatistics().getByteInCount());
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, meterStats.getMeterStatistics().getPacketInCount());
    }

    private class ChangeListener implements DataChangeListener {

        @Override
        public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            synchronized (waitObject) {
                dataReceived = true;
                waitObject.notify();
            }
        }
    }
}
