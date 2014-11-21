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
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
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

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = //
            NODES_INSTANCE_ID_BA.builder() //
            .child(Node.class, NODE_KEY).toInstance();

    private static final InstanceIdentifier<FlowCapableNode> ALL_FLOW_CAPABLE_NODES = //
            NODES_INSTANCE_ID_BA.builder() //
            .child(Node.class) //
            .augmentation(FlowCapableNode.class) //
            .build();

    private static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier NODE_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
            .toInstance();
    private static final InstanceIdentifier<FlowCapableNode> FLOW_AUGMENTATION_PATH =
            NODE_INSTANCE_ID_BA.builder() //
            .augmentation(FlowCapableNode.class) //
            .build();

    private SettableFuture<DataChangeEvent<InstanceIdentifier<?>, DataObject>> lastReceivedChangeEvent;

    /**
     * Test for Bug 148
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void putNodeAndAugmentation() throws Exception {
        lastReceivedChangeEvent = SettableFuture.create();
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
        InstanceIdentifier<FlowCapableNode> augmentIdentifier = NODE_INSTANCE_ID_BA
                .augmentation(FlowCapableNode.class);
        DataModificationTransaction augmentedTransaction = baDataService.beginTransaction();
        augmentedTransaction.putOperationalData(augmentIdentifier, fnu);


        lastReceivedChangeEvent = SettableFuture.create();
        result = augmentedTransaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        DataChangeEvent<InstanceIdentifier<?>, DataObject> potential = lastReceivedChangeEvent.get(1000,TimeUnit.MILLISECONDS);
        assertNotNull(potential);
        assertTrue(potential.getCreatedOperationalData().containsKey(FLOW_AUGMENTATION_PATH));

        lastReceivedChangeEvent = SettableFuture.create();

        Node augmentedNode = (Node) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
        assertNotNull(node);
        assertEquals(NODE_KEY, augmentedNode.getKey());
        System.out.println("Before assertion");
        assertNotNull(augmentedNode.getAugmentation(FlowCapableNode.class));
        FlowCapableNode readedAugmentation = augmentedNode.getAugmentation(FlowCapableNode.class);
        assertEquals(fnu.getDescription(), readedAugmentation.getDescription());
        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);
        testNodeRemove();
        assertTrue(lastReceivedChangeEvent.get(1000,TimeUnit.MILLISECONDS).getRemovedOperationalData().contains(FLOW_AUGMENTATION_PATH));
    }

    @Test
    @Ignore
    public void putNodeWithAugmentation() throws Exception {
        lastReceivedChangeEvent = SettableFuture.create();
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


        DataChangeEvent<InstanceIdentifier<?>, DataObject> potential = lastReceivedChangeEvent.get(1000,TimeUnit.MILLISECONDS);
        assertNotNull(potential);
        assertTrue(potential.getCreatedOperationalData().containsKey(FLOW_AUGMENTATION_PATH));
        lastReceivedChangeEvent = SettableFuture.create();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        FlowCapableNode readedAugmentation = (FlowCapableNode) baDataService.readOperationalData(
                NODE_INSTANCE_ID_BA.augmentation(FlowCapableNode.class));
        assertNotNull(readedAugmentation);

        assertEquals(fnu.getHardware(), readedAugmentation.getHardware());

        testPutNodeConnectorWithAugmentation();
        lastReceivedChangeEvent = SettableFuture.create();
        testNodeRemove();

        assertTrue(lastReceivedChangeEvent.get(1000,TimeUnit.MILLISECONDS).getRemovedOperationalData().contains(FLOW_AUGMENTATION_PATH));
    }

    private void testPutNodeConnectorWithAugmentation() throws Exception {
        NodeConnectorKey ncKey = new NodeConnectorKey(new NodeConnectorId("test:0:0"));
        InstanceIdentifier<NodeConnector> ncPath = NODE_INSTANCE_ID_BA
                .child(NodeConnector.class, ncKey);
        InstanceIdentifier<FlowCapableNodeConnector> ncAugmentPath = ncPath
                .augmentation(FlowCapableNodeConnector.class);

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

    private void assertBindingIndependentVersion(final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier nodeId) {
        CompositeNode node = biDataService.readOperationalData(nodeId);
        assertNotNull(node);
    }

    @Override
    public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        lastReceivedChangeEvent.set(change);
    }

}
