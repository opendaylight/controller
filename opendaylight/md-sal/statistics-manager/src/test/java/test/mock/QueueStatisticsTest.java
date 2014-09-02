package test.mock;

import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatisticsManagerActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.NotificationProviderServiceMock;
import test.mock.util.PortMockGenerator;
import test.mock.util.ProviderContextMock;
import test.mock.util.QueueMockGenerator;
import test.mock.util.RpcProviderRegistryMock;
import test.mock.util.StatisticsManagerTest;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;

public class QueueStatisticsTest extends StatisticsManagerTest {
    private NodeKey s1Key = new NodeKey(new NodeId("S1"));

    @Test
    public void getStatisticsForAddedQueueTest() throws ExecutionException, InterruptedException, ReadFailedException {
        NotificationProviderServiceMock notificationMock = new NotificationProviderServiceMock();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        Port port = PortMockGenerator.getRandomPort();
        NodeConnectorBuilder ncBuilder = new NodeConnectorBuilder();
        FlowCapableNodeConnectorBuilder fcncBuilder = new FlowCapableNodeConnectorBuilder();
        fcncBuilder.setConfiguration(port.getConfiguration());
        fcncBuilder.setPortNumber(port.getPortNumber());
        fcncBuilder.setQueue(Collections.<Queue>emptyList());
        ncBuilder.setKey(new NodeConnectorKey(new NodeConnectorId("connector.1")));
        ncBuilder.addAugmentation(FlowCapableNodeConnector.class, fcncBuilder.build());


        Queue queue = QueueMockGenerator.getRandomQueueWithPortNum(port.getPortNumber().getUint32());
        InstanceIdentifier<Queue> queueII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .child(NodeConnector.class, ncBuilder.getKey()).augmentation(FlowCapableNodeConnector.class)
                .child(Queue.class, queue.getKey());
        InstanceIdentifier<NodeConnector> nodeConnectorII = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).child(NodeConnector.class, ncBuilder.getKey());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, nodeConnectorII, ncBuilder.build());
        writeTx.put(LogicalDatastoreType.CONFIGURATION, queueII, queue);
        assertCommit(writeTx.submit());
        Thread.sleep(2000);

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<Queue> groupOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, queueII).checkedGet();
        assertTrue(groupOptional.isPresent());
    }
}
