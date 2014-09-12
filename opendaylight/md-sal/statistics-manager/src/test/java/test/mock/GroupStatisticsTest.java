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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityGroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.StatisticsManagerTest;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GroupStatisticsTest extends StatisticsManagerTest {
    private final Object waitObject = new Object();

    @Test(timeout = 3000)
    public void addedFlowOnDemandStatsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        Group group = getGroup();

        InstanceIdentifier<Group> groupII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Group.class, group.getKey());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, groupII, group);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, groupII, group);
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                groupII.augmentation(NodeGroupStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<NodeGroupStatistics> groupOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, groupII.augmentation(NodeGroupStatistics.class)).checkedGet();
        assertTrue(groupOptional.isPresent());
        assertEquals(COUNTER_64_TEST_VALUE, groupOptional.get().getGroupStatistics().getByteCount());
    }

    @Test(timeout = 5000)
    public void deletedGroupStasRemovalTest() throws ExecutionException, InterruptedException, ReadFailedException {
        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        Group group = getGroup();
        InstanceIdentifier<Group> groupII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Group.class, group.getKey());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, groupII, group);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, groupII, group);
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                groupII.augmentation(NodeGroupStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<NodeGroupStatistics> groupOptional = readTx.read(LogicalDatastoreType.OPERATIONAL,
                groupII.augmentation(NodeGroupStatistics.class)).checkedGet();
        assertTrue(groupOptional.isPresent());
        assertEquals(COUNTER_64_TEST_VALUE, groupOptional.get().getGroupStatistics().getByteCount());

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, groupII);
        assertCommit(writeTx.submit());

        readTx = getDataBroker().newReadOnlyTransaction();
        groupOptional = readTx.read(LogicalDatastoreType.OPERATIONAL,
                groupII.augmentation(NodeGroupStatistics.class)).checkedGet();
        assertFalse(groupOptional.isPresent());

    }

    @Test(timeout = 23000)
    public void getAllStatsFromConnectedNodeTest() throws ExecutionException, InterruptedException {
        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNodeWithFeatures(s1Key, false, FlowFeatureCapabilityGroupStats.class);

        InstanceIdentifier<Group> groupII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Group.class, getGroup().getKey());
        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                groupII.augmentation(NodeGroupStatistics.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<Group> optionalGroup = readTx.read(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).augmentation(FlowCapableNode.class)
                .child(Group.class, getGroup().getKey())).get();

        assertTrue(optionalGroup.isPresent());
//        assertTrue(optionalGroup.get().getAugmentation(NodeGroupDescStats.class) != null);
        NodeGroupStatistics groupStats = optionalGroup.get().getAugmentation(NodeGroupStatistics.class);
        assertTrue(groupStats != null);
        assertEquals(COUNTER_64_TEST_VALUE, groupStats.getGroupStatistics().getByteCount());
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
