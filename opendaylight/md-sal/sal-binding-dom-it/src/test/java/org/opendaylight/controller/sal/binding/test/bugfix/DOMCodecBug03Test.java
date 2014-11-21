/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.bugfix;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.SupportedActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.SupportedActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.supported.actions.ActionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.supported.actions.ActionTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.SupportType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class DOMCodecBug03Test extends AbstractDataServiceTest implements DataChangeListener {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final String NODE_ID = "openflow:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<Nodes> NODES_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .toInstance();


    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = NODES_INSTANCE_ID_BA.child(Node.class, NODE_KEY);


    private static final InstanceIdentifier<SupportedActions> SUPPORTED_ACTIONS_INSTANCE_ID_BA = //
            NODES_INSTANCE_ID_BA.builder() //
            .child(Node.class, NODE_KEY) //
            .augmentation(FlowCapableNode.class) //
            .child(SupportedActions.class)
            .toInstance();


    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier NODE_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
            .toInstance();
    private static final QName SUPPORTED_ACTIONS_QNAME = QName.create(FlowCapableNode.QNAME, SupportedActions.QNAME.getLocalName());


    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier SUPPORTED_ACTIONS_INSTANCE_ID_BI = //
            org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
                    .node(Nodes.QNAME) //
                    .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
                    .node(SUPPORTED_ACTIONS_QNAME) //
                    .toInstance();

    private final SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> receivedChangeEvent = SettableFuture.create();



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

        DataChangeEvent<InstanceIdentifier<?>, DataObject> potential = receivedChangeEvent.get(1000,TimeUnit.MILLISECONDS);
        assertNotNull(potential);

        verifyNodes((Nodes) potential.getUpdatedOperationalSubtree(),original);
        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);
        Nodes nodes = checkForNodes();
        verifyNodes(nodes,original);

        testAddingNodeConnector();
        testNodeRemove();

    }

    @Test
    public void testAugmentNestedSerialization() throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();

        SupportedActionsBuilder actions = new SupportedActionsBuilder();
        ActionTypeBuilder action = new ActionTypeBuilder();
        action.setAction("foo-action");
        action.setSupportState(SupportType.Native);
        List<ActionType> actionTypes = Collections.singletonList(action.build());
        actions.setActionType(actionTypes );

        transaction.putOperationalData(SUPPORTED_ACTIONS_INSTANCE_ID_BA, actions.build());
        RpcResult<TransactionStatus> putResult = transaction.commit().get();
        assertNotNull(putResult);
        assertEquals(TransactionStatus.COMMITED, putResult.getResult());
        SupportedActions readedTable = (SupportedActions) baDataService.readOperationalData(SUPPORTED_ACTIONS_INSTANCE_ID_BA);
        assertNotNull(readedTable);

        CompositeNode biSupportedActions = biDataService.readOperationalData(SUPPORTED_ACTIONS_INSTANCE_ID_BI);
        assertNotNull(biSupportedActions);

    }

    private void testAddingNodeConnector() throws Exception {

        NodeConnectorId ncId = new NodeConnectorId("openflow:1:bar");
        NodeConnectorKey nodeKey = new NodeConnectorKey(ncId );
        InstanceIdentifier<NodeConnector> ncInstanceId = NODE_INSTANCE_ID_BA.child(NodeConnector.class, nodeKey);
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

    private void verifyNodes(final Nodes nodes,final Node original) {
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
            final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier nodeId) {
        CompositeNode node = biDataService.readOperationalData(nodeId);
        assertNotNull(node);
    }

    private Nodes checkForNodes() {
        return (Nodes) baDataService.readOperationalData(NODES_INSTANCE_ID_BA);
    }

    @Override
    public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        receivedChangeEvent.set(change);
    }

}
