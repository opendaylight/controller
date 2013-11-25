package org.opendaylight.controller.sal.binding.test.bugfix;
import java.util.Collections;
import java.util.Map;


import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;



import static org.junit.Assert.*;

public class DOMCodecBug03Test extends AbstractDataServiceTest implements DataChangeListener {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final String NODE_ID = "openflow:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<Nodes> NODES_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .toInstance();


    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(NODES_INSTANCE_ID_BA) //
            .child(Node.class, NODE_KEY).toInstance();

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier NODE_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
            .toInstance();
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
        
        
        
        testAddingNodeConnector();
        
        
        
        testNodeRemove();
        
        
    }

    private void testAddingNodeConnector() throws Exception {
        
        NodeConnectorId ncId = new NodeConnectorId("openflow:1:bar");
        NodeConnectorKey nodeKey = new NodeConnectorKey(ncId );
        InstanceIdentifier<NodeConnector> ncInstanceId = InstanceIdentifier.builder(NODE_INSTANCE_ID_BA).child(NodeConnector.class, nodeKey).toInstance();
        NodeConnectorBuilder ncBuilder = new NodeConnectorBuilder();
        ncBuilder.setId(ncId);
        ncBuilder.setKey(nodeKey);
        NodeConnector connector = ncBuilder.build();
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.putOperationalData(ncInstanceId, connector);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());
        Node node = (Node) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
        assertNotNull(node);
        assertNotNull(node.getNodeConnector());
        assertFalse(node.getNodeConnector().isEmpty());
        NodeConnector readedNc = node.getNodeConnector().get(0);
        assertNotNull(readedNc);
        
        
        
        
    }

    private void testNodeRemove() throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.removeOperationalData(NODE_INSTANCE_ID_BA);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());
        
        Node node = (Node) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
        assertNull(node);
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
