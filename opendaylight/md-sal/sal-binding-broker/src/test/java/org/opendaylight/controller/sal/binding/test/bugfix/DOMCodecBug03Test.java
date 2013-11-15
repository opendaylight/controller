package org.opendaylight.controller.sal.binding.test.bugfix;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import static org.junit.Assert.*;

public class DOMCodecBug03Test extends AbstractDataServiceTest implements DataChangeListener {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final String NODE_ID = "openflow:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<Nodes> NODES_INSTANCE_ID_BA = InstanceIdentifier.builder() //
            .node(Nodes.class) //
            .toInstance();

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier NODES_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .toInstance();

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder() //
            .node(Nodes.class) //
            .child(Node.class, NODE_KEY).toInstance();

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier NODE_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
            .toInstance();
    private static final NodeRef NODE_REF = new NodeRef(NODE_INSTANCE_ID_BA);
    private DataChangeEvent<InstanceIdentifier<?>, DataObject> receivedChangeEvent;

    
    
    /**
     * Test for Bug 148
     * 
     * @throws Exception
     */
    @Test
    public void testAugmentSerialization() throws Exception {

        
        baDataService.registerDataChangeListener(NODES_INSTANCE_ID_BA, this);
        
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(NODE_ID));
        nodeBuilder.setKey(NODE_KEY);
        DataModificationTransaction transaction = baDataService.beginTransaction();
        
        
        FlowCapableNodeBuilder fnub = new FlowCapableNodeBuilder();
        fnub.setHardware("Hardware Foo");
        fnub.setManufacturer("Manufacturer Foo");
        fnub.setSerialNumber("Serial Foo");
        fnub.setDescription("Description Foo");
        fnub.setSoftware("JUnit emulated");
        FlowCapableNode fnu = fnub.build();
        nodeBuilder.addAugmentation(FlowCapableNode.class, fnu);
        Node original = nodeBuilder.build();
        transaction.putOperationalData(NODE_INSTANCE_ID_BA, original);
        
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());
        
        assertNotNull(receivedChangeEvent);
        
        verifyNodes((Nodes) receivedChangeEvent.getUpdatedOperationalSubtree(),original);
        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);
        Nodes nodes = checkForNodes();
        verifyNodes(nodes,original);
        
        
    }

    private void verifyNodes(Nodes nodes,Node original) {
        assertNotNull(nodes);
        assertNotNull(nodes.getNode());
        assertEquals(1, nodes.getNode().size());
        Node readedNode = nodes.getNode().get(0);
        assertEquals(original.getId(), readedNode.getId());
        assertEquals(original.getKey(), readedNode.getKey());
        
        FlowCapableNode fnu = original.getAugmentation(FlowCapableNode.class);
        FlowCapableNode readedAugment = readedNode.getAugmentation(FlowCapableNode.class);
        assertNotNull(fnu);
        assertEquals(fnu.getDescription(), readedAugment.getDescription());
        assertEquals(fnu.getSerialNumber(), readedAugment.getSerialNumber());
        
    }

    private void assertBindingIndependentVersion(
            org.opendaylight.yangtools.yang.data.api.InstanceIdentifier nodeId) {
        CompositeNode node = biDataService.readOperationalData(nodeId);
        assertNotNull(node);
    }

    private Nodes checkForNodes() {
        return (Nodes) baDataService.readOperationalData(NODES_INSTANCE_ID_BA);
    }
    
    @Override
    public void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        receivedChangeEvent = change;
    }

}
