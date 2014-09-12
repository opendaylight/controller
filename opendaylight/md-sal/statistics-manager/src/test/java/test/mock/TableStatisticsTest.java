package test.mock;

import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatisticsManagerActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityTableStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.StatisticsManagerTest;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TableStatisticsTest extends StatisticsManagerTest {
    private final Object waitObject = new Object();

    @Test(timeout = 23000)
    public void getTableStatisticsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNodeWithFeatures(s1Key, false, FlowFeatureCapabilityTableStats.class);

        TableId tableId = getTableId();
        InstanceIdentifier<Table> tableII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Table.class, new TableKey(tableId.getValue()));

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                tableII.augmentation(FlowTableStatisticsData.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<FlowTableStatisticsData> flowTableStatisticsDataOptional = readTx.read(
                LogicalDatastoreType.OPERATIONAL, tableII.augmentation(FlowTableStatisticsData.class)).checkedGet();
        assertTrue(flowTableStatisticsDataOptional.isPresent());
        assertEquals(COUNTER_32_TEST_VALUE,
                flowTableStatisticsDataOptional.get().getFlowTableStatistics().getActiveFlows());
        assertEquals(COUNTER_64_TEST_VALUE,
                flowTableStatisticsDataOptional.get().getFlowTableStatistics().getPacketsLookedUp());
    }

    private class ChangeListener implements DataChangeListener {

        @Override
        public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
            synchronized (waitObject) {
                waitObject.notify();
            }
        }
    }
}
