/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.connect.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.concurrent.Future;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class BrokerIntegrationTest extends AbstractDataServiceTest {

    @Test
    public void simpleModifyOperation() throws Exception {

        NodeRef node1 = createNodeRef("0");
        DataObject node = baDataService.readConfigurationData(node1.getValue());
        assertNull(node);
        Node nodeData1 = createNode("0");

        DataModificationTransaction transaction = baDataService.beginTransaction();
        transaction.putConfigurationData(node1.getValue(), nodeData1);
        Future<RpcResult<TransactionStatus>> commitResult = transaction.commit();
        assertNotNull(commitResult);

        RpcResult<TransactionStatus> result = commitResult.get();

        assertNotNull(result);
        assertNotNull(result.getResult());
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        Node readedData = (Node) baDataService.readConfigurationData(node1.getValue());
        assertNotNull(readedData);
        assertEquals(nodeData1.getKey(), readedData.getKey());

        NodeRef nodeFoo = createNodeRef("foo");
        NodeRef nodeBar = createNodeRef("bar");
        Node nodeFooData = createNode("foo");
        Node nodeBarData = createNode("bar");

        DataModificationTransaction insertMoreTr = baDataService.beginTransaction();
        insertMoreTr.putConfigurationData(nodeFoo.getValue(), nodeFooData);
        insertMoreTr.putConfigurationData(nodeBar.getValue(), nodeBarData);
        RpcResult<TransactionStatus> result2 = insertMoreTr.commit().get();

        assertNotNull(result2);
        assertNotNull(result2.getResult());
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        Nodes allNodes = (Nodes) baDataService.readConfigurationData(InstanceIdentifier.builder(Nodes.class)
                .toInstance());
        assertNotNull(allNodes);
        assertNotNull(allNodes.getNode());
        assertEquals(3, allNodes.getNode().size());

        /**
         * We create transaction no 2
         *
         */
        DataModificationTransaction removalTransaction = baDataService.beginTransaction();
        assertNotNull(transaction);

        /**
         * We remove node 1
         *
         */
        removalTransaction.removeConfigurationData(node1.getValue());

        /**
         * We commit transaction
         */
        Future<RpcResult<TransactionStatus>> commitResult2 = removalTransaction.commit();
        assertNotNull(commitResult2);

        RpcResult<TransactionStatus> result3 = commitResult2.get();

        assertNotNull(result3);
        assertNotNull(result3.getResult());
        assertEquals(TransactionStatus.COMMITED, result2.getResult());

        DataObject readedData2 = baDataService.readConfigurationData(node1.getValue());
        assertNull(readedData2);
    }

    private static NodeRef createNodeRef(final String string) {
        NodeKey key = new NodeKey(new NodeId(string));
        InstanceIdentifier<Node> path = InstanceIdentifier.builder(Nodes.class).child(Node.class, key)
                .toInstance();
        return new NodeRef(path);
    }

    private static Node createNode(final String string) {
        NodeBuilder ret = new NodeBuilder();
        ret.setId(new NodeId(string));
        ret.setKey(new NodeKey(ret.getId()));
        return ret.build();
    }
}
