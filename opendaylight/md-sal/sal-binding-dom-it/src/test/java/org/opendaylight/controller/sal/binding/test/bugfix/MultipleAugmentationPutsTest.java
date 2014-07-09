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

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.test.AugmentationVerifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;


@SuppressWarnings("deprecation")
public class MultipleAugmentationPutsTest extends AbstractDataServiceTest implements DataChangeListener {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final String NODE_ID = "openflow:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<Nodes> NODES_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .toInstance();

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA =
            NODES_INSTANCE_ID_BA.child(Node.class, NODE_KEY);

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
    @Test( timeout = 15000)
    public void testAugmentSerialization() throws Exception {

        baDataService.registerDataChangeListener(NODES_INSTANCE_ID_BA, this);

        Node flowCapableNode = createTestNode(FlowCapableNode.class, flowCapableNodeAugmentation());
        commitNodeAndVerifyTransaction(flowCapableNode);

        assertNotNull(receivedChangeEvent);
        verifyNode((Nodes) receivedChangeEvent.getUpdatedOperationalSubtree(), flowCapableNode);

        Nodes nodes = checkForNodes();
        verifyNode(nodes, flowCapableNode).assertHasAugmentation(FlowCapableNode.class);
        assertBindingIndependentVersion(NODE_INSTANCE_ID_BI);
        testNodeRemove();
    }

    private <T extends Augmentation<Node>> Node createTestNode(final Class<T> augmentationClass, final T augmentation) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(NODE_ID));
        nodeBuilder.setKey(NODE_KEY);
        nodeBuilder.addAugmentation(augmentationClass, augmentation);
        return nodeBuilder.build();
    }

    private DataModificationTransaction commitNodeAndVerifyTransaction(final Node original) throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.putOperationalData(NODE_INSTANCE_ID_BA, original);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());
        return transaction;
    }

    private void testNodeRemove() throws Exception {
        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.removeOperationalData(NODE_INSTANCE_ID_BA);
        RpcResult<TransactionStatus> result = transaction.commit().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        Node node = (Node) baDataService.readOperationalData(NODE_INSTANCE_ID_BA);
        assertNull(node);
    }

    private AugmentationVerifier<Node> verifyNode(final Nodes nodes, final Node original) {
        assertNotNull(nodes);
        assertNotNull(nodes.getNode());
        assertEquals(1, nodes.getNode().size());
        Node readedNode = nodes.getNode().get(0);
        assertEquals(original.getId(), readedNode.getId());
        assertEquals(original.getKey(), readedNode.getKey());
        return new AugmentationVerifier<Node>(readedNode);
    }

    private void assertBindingIndependentVersion(final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier nodeId) {
        CompositeNode node = biDataService.readOperationalData(nodeId);
        assertNotNull(node);
    }

    private Nodes checkForNodes() {
        return (Nodes) baDataService.readOperationalData(NODES_INSTANCE_ID_BA);
    }

    private FlowCapableNode flowCapableNodeAugmentation() {
        FlowCapableNodeBuilder fnub = new FlowCapableNodeBuilder();
        fnub.setHardware("Hardware Foo");
        fnub.setManufacturer("Manufacturer Foo");
        fnub.setSerialNumber("Serial Foo");
        fnub.setDescription("Description Foo");
        fnub.setSoftware("JUnit emulated");
        FlowCapableNode fnu = fnub.build();
        return fnu;
    }

    @Override
    public void onDataChanged(final DataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        receivedChangeEvent = change;
    }

}
