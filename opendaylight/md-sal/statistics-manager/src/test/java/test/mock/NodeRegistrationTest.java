package test.mock;

import org.junit.Test;
import org.opendaylight.controller.md.statistics.manager.StatisticsManagerActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.NotificationProviderServiceMock;
import test.mock.util.ProviderContextMock;
import test.mock.util.RpcProviderRegistryMock;
import test.mock.util.StatisticsManagerTest;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NodeRegistrationTest extends StatisticsManagerTest{
    private NodeKey s1Key = new NodeKey(new NodeId("S1"));

    @Test
    public void nodeRegistrationTest() throws ExecutionException, InterruptedException {
        NotificationProviderServiceMock notificationMock = new NotificationProviderServiceMock();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);
        InstanceIdentifier<FlowCapableNode> nodeII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class);

        assertTrue(activator.getStatisticManager().getRepeatedlyEnforcer().isProvidedFlowNodeActive(nodeII));
    }

    @Test
    public void nodeUnregistrationTest() throws ExecutionException, InterruptedException {
        NotificationProviderServiceMock notificationMock = new NotificationProviderServiceMock();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        InstanceIdentifier<FlowCapableNode> nodeII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class);

        removeNode(s1Key);

        assertFalse(activator.getStatisticManager().getRepeatedlyEnforcer().isProvidedFlowNodeActive(nodeII));
    }


}
