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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityFlowStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.NotificationProviderServiceHelper;
import test.mock.util.ProviderContextMock;
import test.mock.util.RpcProviderRegistryMock;
import test.mock.util.StatisticsManagerTest;
import test.mock.util.TestUtils;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FlowStatisticsTest extends StatisticsManagerTest {
    private NodeKey s1Key = new NodeKey(new NodeId("S1"));
    private final Object waitObject = new Object();
    private volatile boolean dataReceived = false;

    @Test
    public void addedFlowOnDemandStatsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        Flow flow = TestUtils.getFlow();

        InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(flow.getTableId()))
                .child(Flow.class, flow.getKey());
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(flow.getTableId()));
        Table table = new TableBuilder().setKey(new TableKey(flow.getTableId())).setFlow(Collections.<Flow>emptyList()).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, tableII, table);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, flowII, flow);
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                flowII.augmentation(FlowStatisticsData.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            while(!dataReceived) {
                waitObject.wait(1000);
            }
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<FlowStatisticsData> flowStatDataOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, flowII.augmentation(FlowStatisticsData.class))
                .checkedGet();
        assertTrue(flowStatDataOptional.isPresent());
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, flowStatDataOptional.get().getFlowStatistics().getByteCount());

    }

    @Test
    public void deletedFlowStatsRemovalTest() throws ExecutionException, InterruptedException, ReadFailedException {
        NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        Flow flow = TestUtils.getFlow();

        InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(flow.getTableId()))
                .child(Flow.class, flow.getKey());
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(flow.getTableId()));
        Table table = new TableBuilder().setKey(new TableKey(flow.getTableId())).setFlow(Collections.<Flow>emptyList()).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, tableII, table);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, flowII, flow);
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                flowII.augmentation(FlowStatisticsData.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            while(!dataReceived) {
                waitObject.wait(1000);
            }
        }
        dataReceived = false;

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<FlowStatisticsData> flowStatDataOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, flowII.augmentation(FlowStatisticsData.class))
                .checkedGet();
        assertTrue(flowStatDataOptional.isPresent());
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, flowStatDataOptional.get().getFlowStatistics().getByteCount());

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, flowII);
        assertCommit(writeTx.submit());

        synchronized (waitObject) {
            while(!dataReceived) {
                waitObject.wait(1000);
            }
        }

        readTx = getDataBroker().newReadOnlyTransaction();
        flowStatDataOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, flowII.augmentation(FlowStatisticsData.class))
                .checkedGet();
        assertFalse(flowStatDataOptional.isPresent());
    }

    @Test
    public void getAllStatsWhenNodeIsConnectedTest() throws ExecutionException, InterruptedException, ReadFailedException {
        NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNodeWithFeatures(s1Key, false, FlowFeatureCapabilityFlowStats.class);

        Flow flow = TestUtils.getFlow();

        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(flow.getTableId()));

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                tableII.child(Flow.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            while(!dataReceived) {
                waitObject.wait(21000);
            }
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<Table> tableOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flow.getTableId()))).checkedGet();
        assertTrue(tableOptional.isPresent());
        FlowStatisticsData flowStats = tableOptional.get().getFlow().get(0).getAugmentation(FlowStatisticsData.class);
        assertTrue(flowStats != null);
        assertEquals(TestUtils.COUNTER_64_TEST_VALUE, flowStats.getFlowStatistics().getByteCount());
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
