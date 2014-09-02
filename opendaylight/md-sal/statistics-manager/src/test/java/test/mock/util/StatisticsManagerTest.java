package test.mock.util;

import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FeatureCapability;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityFlowStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityGroupStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityPortStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityQueueStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowFeatureCapabilityTableStats;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.SwitchFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class StatisticsManagerTest extends AbstractDataBrokerTest{

    public void addFlowCapableNode(NodeKey nodeKey) throws ExecutionException, InterruptedException {
        Nodes nodes = new NodesBuilder().setNode(Collections.<Node>emptyList()).build();
        InstanceIdentifier<Node> flowNodeIdentifier = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, nodeKey);

        FlowCapableNodeBuilder fcnBuilder = new FlowCapableNodeBuilder();
        SwitchFeaturesBuilder sfBuilder = new SwitchFeaturesBuilder();
        List<Class<? extends FeatureCapability>> capabilities = new ArrayList<>();
        capabilities.add(FlowFeatureCapabilityTableStats.class);
        capabilities.add(FlowFeatureCapabilityFlowStats.class);
        capabilities.add(FlowFeatureCapabilityGroupStats.class);
        capabilities.add(FlowFeatureCapabilityPortStats.class);
        capabilities.add(FlowFeatureCapabilityQueueStats.class);
        sfBuilder.setCapabilities(capabilities);
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(nodeKey);
        fcnBuilder.setSwitchFeatures(sfBuilder.build());
        nodeBuilder.addAugmentation(FlowCapableNode.class, fcnBuilder.build());

        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class), nodes);
        writeTx.put(LogicalDatastoreType.OPERATIONAL, flowNodeIdentifier, nodeBuilder.build());
        writeTx.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Nodes.class), nodes);
        writeTx.put(LogicalDatastoreType.CONFIGURATION, flowNodeIdentifier, nodeBuilder.build());
        assertCommit(writeTx.submit());
    }

    public void removeNode(NodeKey nodeKey) throws ExecutionException, InterruptedException {
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class).child(Node.class, nodeKey));
        writeTx.submit().get();
    }
}
