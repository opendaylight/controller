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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javassist.ClassPool;

import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.test.util.BindingBrokerTestFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class DOMCodecBug02Test extends AbstractDataServiceTest {

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final String NODE_ID = "node:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));

    private static final Map<QName, Object> NODE_KEY_BI = Collections.<QName, Object> singletonMap(NODE_ID_QNAME,
            NODE_ID);

    private static final InstanceIdentifier<Nodes> NODES_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .toInstance();

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier NODES_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .toInstance();

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .child(Node.class, NODE_KEY).toInstance();

    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier NODE_INSTANCE_ID_BI = //
    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder() //
            .node(Nodes.QNAME) //
            .nodeWithKey(Node.QNAME, NODE_KEY_BI) //
            .toInstance();
    private static final NodeRef NODE_REF = new NodeRef(NODE_INSTANCE_ID_BA);

    /**
     * This test is ignored, till found out better way to test generation
     * of classes without leaking of instances from previous run
     *
     * @throws Exception
     */

    @Override
    public void setUp() {
        ListeningExecutorService executor = MoreExecutors.sameThreadExecutor();
        BindingBrokerTestFactory factory = new BindingBrokerTestFactory();
        factory.setExecutor(executor);
        factory.setClassPool(new ClassPool());
        factory.setStartWithParsedSchema(getStartWithSchema());
        testContext = factory.getTestContext();
        testContext.start();

        baDataService = testContext.getBindingDataBroker();
        biDataService = testContext.getDomDataBroker();
        dataStore = testContext.getDomDataStore();
        mappingService = testContext.getBindingToDomMappingService();
    };

    @Test
    public void testSchemaContextNotAvailable() throws Exception {

        ExecutorService testExecutor = Executors.newFixedThreadPool(1);
        testContext.loadYangSchemaFromClasspath();
        Future<Future<RpcResult<TransactionStatus>>> future = testExecutor.submit(new Callable<Future<RpcResult<TransactionStatus>>>() {
            @Override
            public Future<RpcResult<TransactionStatus>> call() throws Exception {
                NodesBuilder nodesBuilder = new NodesBuilder();
                nodesBuilder.setNode(Collections.<Node> emptyList());
                DataModificationTransaction transaction = baDataService.beginTransaction();
                transaction.putOperationalData(NODES_INSTANCE_ID_BA, nodesBuilder.build());
                return transaction.commit();
            }
        });


        RpcResult<TransactionStatus> result = future.get().get();
        assertEquals(TransactionStatus.COMMITED, result.getResult());

        Nodes nodes = checkForNodes();
        assertNotNull(nodes);

    }

    private Nodes checkForNodes() {
        return (Nodes) baDataService.readOperationalData(NODES_INSTANCE_ID_BA);

    }

    @Override
    protected boolean getStartWithSchema() {
        return false;
    }

}
