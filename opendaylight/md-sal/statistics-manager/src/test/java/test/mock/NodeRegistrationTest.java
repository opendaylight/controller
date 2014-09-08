package test.mock;

import org.junit.Test;
import org.opendaylight.controller.md.statistics.manager.StatisticsManagerActivator;
import org.opendaylight.controller.md.statistics.manager.StatisticsManagerProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.NotificationProviderServiceHelper;
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
        NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        StatisticsManagerProvider statisticsManagerProvider = new StatisticsManagerProvider(activator);
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);
        InstanceIdentifier<Node> nodeII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key);

        assertTrue(statisticsManagerProvider.getStatisticsManager().getStatCollector().isProvidedFlowNodeActive(nodeII));
    }

    @Test
    public void nodeUnregistrationTest() throws ExecutionException, InterruptedException {
        NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        StatisticsManagerProvider statisticsManagerProvider = new StatisticsManagerProvider(activator);
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        InstanceIdentifier<Node> nodeII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key);

        removeNode(s1Key);

        assertFalse(statisticsManagerProvider.getStatisticsManager().getStatCollector().isProvidedFlowNodeActive(nodeII));
    }


}
