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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityPortStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.NotificationProviderServiceHelper;
import test.mock.util.ProviderContextMock;
import test.mock.util.RpcProviderRegistryMock;
import test.mock.util.StatisticsManagerTest;
import test.mock.util.TestUtils;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PortStatisticsTest extends StatisticsManagerTest {
    private NodeKey s1Key = new NodeKey(new NodeId("S1"));
    private final Object waitObject = new Object();
    private volatile boolean dataReceived = false;

    @Test
    public void getPortStatisticsTest() throws ExecutionException, InterruptedException, ReadFailedException {
        NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNodeWithFeatures(s1Key, false, FlowFeatureCapabilityPortStats.class);

        InstanceIdentifier<NodeConnector> nodeConnectorII = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, s1Key).child(NodeConnector.class, new NodeConnectorKey(TestUtils.getNodeConnectorId()));


        getDataBroker().registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                nodeConnectorII.augmentation(FlowCapableNodeConnectorStatisticsData.class), new ChangeListener(), AsyncDataBroker.DataChangeScope.BASE);

        synchronized (waitObject) {
            while(!dataReceived) {
                waitObject.wait(21000);
            }
        }

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<FlowCapableNodeConnectorStatisticsData> flowCapableNodeConnectorStatisticsDataOptional =
                readTx.read(LogicalDatastoreType.OPERATIONAL,
                        nodeConnectorII.augmentation(FlowCapableNodeConnectorStatisticsData.class)).checkedGet();
        assertTrue(flowCapableNodeConnectorStatisticsDataOptional.isPresent());
        assertEquals(TestUtils.BIG_INTEGER_TEST_VALUE,
                flowCapableNodeConnectorStatisticsDataOptional.get().getFlowCapableNodeConnectorStatistics()
                        .getReceiveDrops());
        assertEquals(TestUtils.BIG_INTEGER_TEST_VALUE,
                flowCapableNodeConnectorStatisticsDataOptional.get().getFlowCapableNodeConnectorStatistics()
                        .getCollisionCount());
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
