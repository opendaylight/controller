/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.bugfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.flow.node.SupportedActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
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

public class PutAugmentationTest extends AbstractDataServiceTest implements DataChangeListener {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final String NODE_ID = "openflow:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<Nodes> NODES_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .toInstance();

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier//
            .builder(NODES_INSTANCE_ID_BA) //
            .child(Node.class, NODE_KEY).toInstance();

    private static final InstanceIdentifier<SupportedActions> SUPPORTED_ACTIONS_INSTANCE_ID_BA = InstanceIdentifier//
            .builder(NODES_INSTANCE_ID_BA) //
            .child(Node.class, NODE_KEY) //
            .augmentation(FlowCapableNode.class) //
            .child(SupportedActions.class).toInstance();

    private static final InstanceIdentifier<FlowCapableNode> ALL_FLOW_CAPABLE_NODES = InstanceIdentifier //
            .builder(NODES_INSTANCE_ID_BA) //
            .child(Node.class) //
            .augmentation(FlowCapableNode.class) //
            .build();

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier NODE_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
            .toInstance();
    private static final QName SUPPORTED_ACTIONS_QNAME = QName.create(FlowCapableNode.QNAME,
            SupportedActions.QNAME.getLocalName());

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier SUPPORTED_ACTIONS_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
            .node(SUPPORTED_ACTIONS_QNAME) //
            .toInstance();
    private static final InstanceIdentifier<FlowCapableNode> FLOW_AUGMENTATION_PATH = InstanceIdentifier //
            .builder(NODE_INSTANCE_ID_BA) //
            .augmentation(FlowCapableNode.class) //
            .build();

    private DataChangeEvent<InstanceIdentifier<?>, DataObject> lastReceivedChangeEvent;

    /**
     * Test for Bug 148
     *
     * @throws Exception
     */
    @Test
    public void putNodeAndAugmentation() throws Exception {

        baDataService.registerDataChangeListener(ALL_FLOW_CAPABLE_NODES, this);


        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(NODE_ID));
        nodeBuilder.setKey(NODE_KEY);
        DataModificationTransaction baseTransaction = baDataService.beginTransaction();
        baseTransaction.putOperationalData(NODE_INSTANCE_ID_BA, nodeBuilder.build());
        RpcResult<TransactionStatus> result = baseTransaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        Node node = (Node) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
        assertNotNull(node);
        assertEquals(NODE_KEY, node.getKey());

        FlowCapableNodeBuilder fnub = new FlowCapableNodeBuilder();
        fnub.setHardware("Hardware Foo");
        fnub.setManufacturer("Manufacturer Foo");
        fnub.setSerialNumber("Serial Foo");
        fnub.setDescription("Description Foo");
        fnub.setSoftware("JUnit emulated");
        FlowCapableNode fnu = fnub.build();
        InstanceIdentifier<FlowCapableNode> augmentIdentifier = InstanceIdentifier.builder(NODE_INSTANCE_ID_BA)
                .augmentation(FlowCapableNode.class).toInstance();
        DataModificationTransaction augmentedTransaction = baDataService.beginTransaction();
        augmentedTransaction.putOperationalData(augmentIdentifier, fnu);

        result = augmentedTransaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        assertNotNull(lastReceivedChangeEvent);
        assertTrue(lastReceivedChangeEvent.getCreatedOperationalData().containsKey(FLOW_AUGMENTATION_PATH));

        Node augmentedNode = (Node) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
        assertNotNull(node);
        assertEquals(NODE_KEY, augmentedNode.getKey());
        System.out.println("Before assertion");
        assertNotNull(augmentedNode.getAugmentation(FlowCapableNode.class));
        FlowCapableNode readedAugmentation = augmentedNode.getAugmentation(FlowCapableNode.class);
        assertEquals(fnu.getDescription(), readedAugmentation.getDescription());
        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);
        testNodeRemove();
        assertTrue(lastReceivedChangeEvent.getRemovedOperationalData().contains(FLOW_AUGMENTATION_PATH));
    }

    @Test
    public void putNodeWithAugmentation() throws Exception {

        baDataService.registerDataChangeListener(ALL_FLOW_CAPABLE_NODES, this);

        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(NODE_ID));
        nodeBuilder.setKey(NODE_KEY);
        FlowCapableNodeBuilder fnub = new FlowCapableNodeBuilder();
        fnub.setHardware("Hardware Foo");
        fnub.setManufacturer("Manufacturer Foo");
        fnub.setSerialNumber("Serial Foo");
        fnub.setDescription("Description Foo");
        fnub.setSoftware("JUnit emulated");
        FlowCapableNode fnu = fnub.build();

        nodeBuilder.addAugmentation(FlowCapableNode.class, fnu);
        DataModificationTransaction baseTransaction = baDataService.beginTransaction();
        baseTransaction.putOperationalData(NODE_INSTANCE_ID_BA, nodeBuilder.build());
        RpcResult<TransactionStatus> result = baseTransaction.commit().get();

        assertNotNull(lastReceivedChangeEvent);
        assertTrue(lastReceivedChangeEvent.getCreatedOperationalData().containsKey(FLOW_AUGMENTATION_PATH));
        lastReceivedChangeEvent = null;
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        FlowCapableNode readedAugmentation = (FlowCapableNode) baDataService.readOperationalData(InstanceIdentifier
                .builder(NODE_INSTANCE_ID_BA).augmentation(FlowCapableNode.class).toInstance());
        assertNotNull(readedAugmentation);

        assertEquals(fnu.getHardware(), readedAugmentation.getHardware());

        testPutNodeConnectorWithAugmentation();
        lastReceivedChangeEvent = null;
        testNodeRemove();

        assertTrue(lastReceivedChangeEvent.getRemovedOperationalData().contains(FLOW_AUGMENTATION_PATH));
    }

    private void testPutNodeConnectorWithAugmentation() throws Exception {
        NodeConnectorKey ncKey = new NodeConnectorKey(new NodeConnectorId("test:0:0"));
        InstanceIdentifier<NodeConnector> ncPath = InstanceIdentifier.builder(NODE_INSTANCE_ID_BA)
                .child(NodeConnector.class, ncKey).toInstance();
        InstanceIdentifier<FlowCapableNodeConnector> ncAugmentPath = InstanceIdentifier.builder(ncPath)
                .augmentation(FlowCapableNodeConnector.class).toInstance();

        NodeConnectorBuilder nc = new NodeConnectorBuilder();
        nc.setKey(ncKey);

        FlowCapableNodeConnectorBuilder fncb = new FlowCapableNodeConnectorBuilder();
        fncb.setName("Baz");
        nc.addAugmentation(FlowCapableNodeConnector.class, fncb.build());

        DataModificationTransaction baseTransaction = baDataService.beginTransaction();
        baseTransaction.putOperationalData(ncPath, nc.build());
        RpcResult<TransactionStatus> result = baseTransaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        FlowCapableNodeConnector readedAugmentation = (FlowCapableNodeConnector) baDataService
                .readOperationalData(ncAugmentPath);
        assertNotNull(readedAugmentation);
        assertEquals(fncb.getName(), readedAugmentation.getName());
    }

    private void testNodeRemove() throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.removeOperationalData(NODE_INSTANCE_ID_BA);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        Node node = (Node) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
        assertNull(node);
    }

    private void verifyNodes(Nodes nodes, Node original) {
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

    private void assertBindingIndependentVersion(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier nodeId) {
        CompositeNode node = biDataService.readOperationalData(nodeId);
        assertNotNull(node);
    }

    private Nodes checkForNodes() {
        return (Nodes) baDataService.readOperationalData(NODES_INSTANCE_ID_BA);
    }

    @Override
    public void onDataChanged(DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        lastReceivedChangeEvent = change;
    }

}
