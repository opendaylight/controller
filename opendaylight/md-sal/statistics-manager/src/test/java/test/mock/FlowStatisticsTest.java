package test.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import test.mock.util.StatisticsManagerTest;

import com.google.common.base.Optional;

public class FlowStatisticsTest extends StatisticsManagerTest {
    private final Object waitObject = new Object();

//    @Test(timeout = 5000)
    public void addedFlowOnDemandStatsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        final StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        final Flow flow = getFlow();

        final InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(flow.getTableId()))
                .child(Flow.class, flow.getKey());
        final InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(flow.getTableId()));
        final Table table = new TableBuilder().setKey(new TableKey(flow.getTableId())).setFlow(Collections.<Flow>emptyList()).build();

        final WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, tableII, table);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, flowII, flow);
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                flowII.augmentation(FlowStatisticsData.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        final ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        final Optional<FlowStatisticsData> flowStatDataOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, flowII.augmentation(FlowStatisticsData.class))
                .checkedGet();
        assertTrue(flowStatDataOptional.isPresent());
        assertEquals(COUNTER_64_TEST_VALUE, flowStatDataOptional.get().getFlowStatistics().getByteCount());

    }

//    @Test(timeout = 5000)
    public void deletedFlowStatsRemovalTest() throws ExecutionException, InterruptedException, ReadFailedException {
        final StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        final Flow flow = getFlow();

        final InstanceIdentifier<Flow> flowII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(flow.getTableId()))
                .child(Flow.class, flow.getKey());
        final InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(flow.getTableId()));
        final Table table = new TableBuilder().setKey(new TableKey(flow.getTableId())).setFlow(Collections.<Flow>emptyList()).build();

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, tableII, table);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowII, flow);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, tableII, table);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, flowII, flow);

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                flowII.augmentation(FlowStatisticsData.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        assertCommit(writeTx.submit());

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<Flow> flowStatDataOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, flowII).checkedGet();
        assertTrue(flowStatDataOptional.isPresent());
//        assertEquals(COUNTER_64_TEST_VALUE, flowStatDataOptional.get().getFlowStatistics().getByteCount());

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, flowII);
        assertCommit(writeTx.submit());

        readTx = getDataBroker().newReadOnlyTransaction();
        flowStatDataOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, flowII).checkedGet();
        assertFalse(flowStatDataOptional.isPresent());
    }

//    @Test(timeout = 23000)
    public void getAllStatsWhenNodeIsConnectedTest() throws ExecutionException, InterruptedException, ReadFailedException {
        final StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

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

    public class ChangeListener implements DataChangeListener {

        @Override
        public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }
    }
}

