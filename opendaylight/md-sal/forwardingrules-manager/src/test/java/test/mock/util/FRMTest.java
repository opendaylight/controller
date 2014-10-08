package test.mock.util;

import org.junit.Before;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.SwitchFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeUpdatedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

public abstract class FRMTest extends AbstractDataBrokerTest{

    protected NotificationProviderServiceHelper notificationMock = new NotificationProviderServiceHelper();
    protected RpcProviderRegistry rpcRegistry;
    protected NodeKey s1Key = new NodeKey(new NodeId("S1"));
    protected TableKey tableKey = new TableKey((short) 2);

    @Before
    public void init() {
        rpcRegistry = new RpcProviderRegistryMock();
    }

    protected void addFlowCapableNode(final NodeKey nodeKey) throws ExecutionException, InterruptedException {
        final Nodes nodes = new NodesBuilder().setNode(Collections.<Node>emptyList()).build();
        final InstanceIdentifier<Node> flowNodeIdentifier = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, nodeKey);

        final FlowCapableNodeBuilder fcnBuilder = new FlowCapableNodeBuilder();
        final SwitchFeaturesBuilder sfBuilder = new SwitchFeaturesBuilder();
        sfBuilder.setMaxTables((short) 2);
        fcnBuilder.setSwitchFeatures(sfBuilder.build());
        final NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(nodeKey);
        final FlowCapableNode flowCapableNode = fcnBuilder.build();
        nodeBuilder.addAugmentation(FlowCapableNode.class, flowCapableNode);
        final Node node = nodeBuilder.build();

        final WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class), nodes);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, flowNodeIdentifier, node);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Nodes.class), nodes);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowNodeIdentifier, node);
        assertCommit(writeTx.submit());

        final NodeUpdatedBuilder nuBuilder = new NodeUpdatedBuilder(node);
        final FlowCapableNodeUpdatedBuilder fcnuBuilder = new FlowCapableNodeUpdatedBuilder(flowCapableNode);
        nuBuilder.setNodeRef(new NodeRef(flowNodeIdentifier));
        nuBuilder.addAugmentation(FlowCapableNodeUpdated.class, fcnuBuilder.build());
        notificationMock.pushNotification(nuBuilder.build());
    }

    protected void removeNode(final NodeKey nodeKey) throws ExecutionException, InterruptedException {
        final InstanceIdentifier<Node> nodeII = InstanceIdentifier.create(Nodes.class).child(Node.class, nodeKey);

        final WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.OPERATIONAL, nodeII);
        writeTx.submit().get();

        final NodeRemovedBuilder nrBuilder = new NodeRemovedBuilder();
        nrBuilder.setNodeRef(new NodeRef(nodeII));
        notificationMock.pushNotification(nrBuilder.build());
    }
}
