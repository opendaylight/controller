package test.mock;

import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatisticsManagerActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.MeterMockGenerator;
import test.mock.util.NotificationProviderServiceMock;
import test.mock.util.ProviderContextMock;
import test.mock.util.RpcProviderRegistryMock;
import test.mock.util.StatisticsManagerTest;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;

public class MeterStatisticsTest extends StatisticsManagerTest {
    private NodeKey s1Key = new NodeKey(new NodeId("S1"));

    @Test
    public void getStatisticsForAddedMeterTest() throws ExecutionException, InterruptedException, ReadFailedException {
        NotificationProviderServiceMock notificationMock = new NotificationProviderServiceMock();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        Meter meter = MeterMockGenerator.getRandomMeter();
        InstanceIdentifier<Meter> meterII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Meter.class, meter.getKey());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, meterII, meter);
        assertCommit(writeTx.submit());
        Thread.sleep(2000);

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<Meter> groupOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, meterII).checkedGet();
        assertTrue(groupOptional.isPresent());
    }
}
