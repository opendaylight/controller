/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.test.sal.binding.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.inject.Inject;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class DataServiceTest extends AbstractTest {

    protected DataBrokerService consumerDataService;


    @Inject
    Broker broker2;

    @Before
    public void setUp() throws Exception {
    }

    /*
     *
     * Ignored this, because classes here are constructed from
     * very different class loader as MD-SAL is run into,
     * this is code is run from different classloader.
     *
     */
    @Test
    @Ignore
    public void test() throws Exception {
        BindingAwareConsumer consumer1 = new BindingAwareConsumer() {

            @Override
            public void onSessionInitialized(final ConsumerContext session) {
                consumerDataService = session.getSALService(DataBrokerService.class);
            }
        };
        broker.registerConsumer(consumer1, getBundleContext());

        assertNotNull(consumerDataService);


        DataModificationTransaction transaction = consumerDataService.beginTransaction();
        assertNotNull(transaction);

        InstanceIdentifier<Node> node1 = createNodeRef("0");
        DataObject  node = consumerDataService.readConfigurationData(node1);
        assertNull(node);
        Node nodeData1 = createNode("0");

        transaction.putConfigurationData(node1, nodeData1);
        Future<RpcResult<TransactionStatus>> commitResult = transaction.commit();
        assertNotNull(commitResult);

        RpcResult<TransactionStatus> result = commitResult.get();

        assertNotNull(result);
        assertNotNull(result.getResult());
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        Node readedData = (Node) consumerDataService.readConfigurationData(node1);
        assertNotNull(readedData);
        assertEquals(nodeData1.getKey(), readedData.getKey());


        DataModificationTransaction transaction2 = consumerDataService.beginTransaction();
        assertNotNull(transaction);

        transaction2.removeConfigurationData(node1);

        Future<RpcResult<TransactionStatus>> commitResult2 = transaction2.commit();
        assertNotNull(commitResult2);

        RpcResult<TransactionStatus> result2 = commitResult2.get();

        assertNotNull(result2);
        assertNotNull(result2.getResult());
        assertEquals(TransactionStatus.COMMITED, result2.getResult());

        DataObject readedData2 = consumerDataService.readConfigurationData(node1);
        assertNull(readedData2);


    }


    private static InstanceIdentifier<Node> createNodeRef(final String string) {
        NodeKey key = new NodeKey(new NodeId(string));
        return  InstanceIdentifier.builder(Nodes.class).child(Node.class, key).build();
    }

    private static Node createNode(final String string) {
        NodeBuilder ret = new NodeBuilder();
        NodeId id = new NodeId(string);
        ret.setKey(new NodeKey(id));
        ret.setId(id);
        return ret.build();
    }
}
