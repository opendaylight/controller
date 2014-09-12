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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityQueueStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.StatisticsManagerTest;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QueueStatisticsTest extends StatisticsManagerTest {
    private final Object waitObject = new Object();

    @Test(timeout = 3000)
    public void addedQueueOnDemandStatsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        Port port = getPort();

        NodeConnectorBuilder ncBuilder = new NodeConnectorBuilder();
        FlowCapableNodeConnectorBuilder fcncBuilder = new FlowCapableNodeConnectorBuilder();
        fcncBuilder.setConfiguration(port.getConfiguration());
        fcncBuilder.setPortNumber(port.getPortNumber());
        fcncBuilder.setQueue(Collections.<Queue>emptyList());
        ncBuilder.setKey(new NodeConnectorKey(new NodeConnectorId("connector.1")));
        ncBuilder.addAugmentation(FlowCapableNodeConnector.class, fcncBuilder.build());


        Queue queue = getQueue();
        InstanceIdentifier<Queue> queueII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .child(NodeConnector.class, ncBuilder.getKey()).augmentation(FlowCapableNodeConnector.class)
                .child(Queue.class, queue.getKey());
        InstanceIdentifier<NodeConnector> nodeConnectorII = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).child(NodeConnector.class, ncBuilder.getKey());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, nodeConnectorII, ncBuilder.build());
        writeTx.put(LogicalDatastoreType.CONFIGURATION, queueII, queue);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, nodeConnectorII, ncBuilder.build());
        writeTx.put(LogicalDatastoreType.OPERATIONAL, queueII, queue);
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                queueII.augmentation(FlowCapableNodeConnectorQueueStatisticsData.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<FlowCapableNodeConnectorQueueStatisticsData> queueStatsOptional = readTx.read(LogicalDatastoreType.OPERATIONAL,
                queueII.augmentation(FlowCapableNodeConnectorQueueStatisticsData.class)).checkedGet();
        assertTrue(queueStatsOptional.isPresent());
        assertEquals(COUNTER_64_TEST_VALUE,
                queueStatsOptional.get().getFlowCapableNodeConnectorQueueStatistics().getTransmittedBytes());
    }

    @Test(timeout = 5000)
    public void deletedQueueStatsRemovalTest() throws ExecutionException, InterruptedException, ReadFailedException {
        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        Port port = getPort();

        NodeConnectorBuilder ncBuilder = new NodeConnectorBuilder();
        FlowCapableNodeConnectorBuilder fcncBuilder = new FlowCapableNodeConnectorBuilder();
        fcncBuilder.setConfiguration(port.getConfiguration());
        fcncBuilder.setPortNumber(port.getPortNumber());
        fcncBuilder.setQueue(Collections.<Queue>emptyList());
        ncBuilder.setKey(new NodeConnectorKey(new NodeConnectorId("connector.1")));
        ncBuilder.addAugmentation(FlowCapableNodeConnector.class, fcncBuilder.build());


        Queue queue = getQueue();
        InstanceIdentifier<Queue> queueII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .child(NodeConnector.class, ncBuilder.getKey()).augmentation(FlowCapableNodeConnector.class)
                .child(Queue.class, queue.getKey());
        InstanceIdentifier<NodeConnector> nodeConnectorII = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).child(NodeConnector.class, ncBuilder.getKey());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, nodeConnectorII, ncBuilder.build());
        writeTx.put(LogicalDatastoreType.CONFIGURATION, queueII, queue);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, nodeConnectorII, ncBuilder.build());
        writeTx.put(LogicalDatastoreType.OPERATIONAL, queueII, queue);
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                queueII.augmentation(FlowCapableNodeConnectorQueueStatisticsData.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<FlowCapableNodeConnectorQueueStatisticsData> queueStatsOptional = readTx.read(LogicalDatastoreType.OPERATIONAL,
                queueII.augmentation(FlowCapableNodeConnectorQueueStatisticsData.class)).checkedGet();
        assertTrue(queueStatsOptional.isPresent());
        assertEquals(COUNTER_64_TEST_VALUE,
                queueStatsOptional.get().getFlowCapableNodeConnectorQueueStatistics().getTransmittedBytes());

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, queueII);
        assertCommit(writeTx.submit());

        readTx = getDataBroker().newReadOnlyTransaction();
        queueStatsOptional = readTx.read(LogicalDatastoreType.OPERATIONAL,
                queueII.augmentation(FlowCapableNodeConnectorQueueStatisticsData.class)).checkedGet();
        assertFalse(queueStatsOptional.isPresent());
    }

    @Test(timeout = 23000)
    public void getAllStatsFromConnectedNodeTest() throws ExecutionException, InterruptedException, ReadFailedException {
        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNodeWithFeatures(s1Key, false, FlowFeatureCapabilityQueueStats.class);

        NodeConnectorBuilder ncBuilder = new NodeConnectorBuilder();
        FlowCapableNodeConnectorBuilder fcncBuilder = new FlowCapableNodeConnectorBuilder();
        ncBuilder.setKey(new NodeConnectorKey(getNodeConnectorId()));
        ncBuilder.addAugmentation(FlowCapableNodeConnector.class, fcncBuilder.build());

        InstanceIdentifier<NodeConnector> nodeConnectorII = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key)
                .child(NodeConnector.class, ncBuilder.getKey());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, nodeConnectorII, ncBuilder.build());
        InstanceIdentifier<Queue> queueII = nodeConnectorII.augmentation(FlowCapableNodeConnector.class)
                .child(Queue.class, getQueue().getKey());
        assertCommit(writeTx.submit());

        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                queueII.augmentation(FlowCapableNodeConnectorQueueStatisticsData.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            waitObject.wait();
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<Queue> queueOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, queueII).checkedGet();
        assertTrue(queueOptional.isPresent());
        FlowCapableNodeConnectorQueueStatisticsData queueStats =
                queueOptional.get().getAugmentation(FlowCapableNodeConnectorQueueStatisticsData.class);
        assertTrue(queueStats != null);
        assertEquals(COUNTER_64_TEST_VALUE,
                queueStats.getFlowCapableNodeConnectorQueueStatistics().getTransmittedBytes());
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
