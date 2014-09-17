package test.mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.statistics.manager.StatisticsManagerActivator;
import org.opendaylight.controller.md.statistics.manager.StatisticsManagerProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import test.mock.util.StatisticsManagerTest;

public class NodeRegistrationTest extends StatisticsManagerTest {

//    @Test
    public void nodeRegistrationTest() throws ExecutionException, InterruptedException {
        final StatisticsManagerActivator activator = new StatisticsManagerActivator();
        final StatisticsManagerProvider statisticsManagerProvider = new StatisticsManagerProvider(activator);
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);
        Thread.sleep(1000);
        final InstanceIdentifier<Node> nodeII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key);

        assertTrue(statisticsManagerProvider.getStatisticsManager().isProvidedFlowNodeActive(nodeII));
    }

//    @Test
    public void nodeUnregistrationTest() throws ExecutionException, InterruptedException {
        final StatisticsManagerActivator activator = new StatisticsManagerActivator();
        final StatisticsManagerProvider statisticsManagerProvider = new StatisticsManagerProvider(activator);
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);
        Thread.sleep(1000);
        final InstanceIdentifier<Node> nodeII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key);

        assertTrue(statisticsManagerProvider.getStatisticsManager().isProvidedFlowNodeActive(nodeII));

        removeNode(s1Key);
        Thread.sleep(1000);
        assertFalse(statisticsManagerProvider.getStatisticsManager().isProvidedFlowNodeActive(nodeII));
    }
}

