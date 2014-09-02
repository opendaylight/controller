package test.mock;

import com.google.common.base.Optional;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.statistics.manager.StatisticsManagerActivator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import test.mock.util.GroupMockGenerator;
import test.mock.util.NotificationProviderServiceMock;
import test.mock.util.ProviderContextMock;
import test.mock.util.RpcProviderRegistryMock;
import test.mock.util.StatisticsManagerTest;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertTrue;

public class GroupStatisticsTest extends StatisticsManagerTest {
    private NodeKey s1Key = new NodeKey(new NodeId("S1"));

    @Test
    public void getStatisticsForAddedGroupTest() throws ExecutionException, InterruptedException, ReadFailedException {
        NotificationProviderServiceMock notificationMock = new NotificationProviderServiceMock();
        RpcProviderRegistryMock rpcRegistry = new RpcProviderRegistryMock(notificationMock);
        ProviderContextMock providerContext = new ProviderContextMock(rpcRegistry, getDataBroker(), notificationMock.getNotifBroker());

        StatisticsManagerActivator activator = new StatisticsManagerActivator();
        activator.onSessionInitiated(providerContext);

        addFlowCapableNode(s1Key);

        Group group = GroupMockGenerator.getRandomGroup();
        InstanceIdentifier<Group> groupII = InstanceIdentifier.create(Nodes.class).child(Node.class, s1Key)
                .augmentation(FlowCapableNode.class).child(Group.class, group.getKey());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.CONFIGURATION, groupII, group);
        assertCommit(writeTx.submit());
        Thread.sleep(2000);

        ReadOnlyTransaction readTx = getDataBroker().newReadOnlyTransaction();
        Optional<Group> groupOptional = readTx.read(LogicalDatastoreType.OPERATIONAL, groupII).checkedGet();
        assertTrue(groupOptional.isPresent());
    }
}
