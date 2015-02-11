/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.connect.dom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.test.util.BindingBrokerTestFactory;
import org.opendaylight.controller.sal.binding.test.util.BindingTestContext;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.KnockKnockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.KnockKnockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.KnockKnockOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.KnockKnockOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.OpendaylightOfMigrationTestModelService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.aug.grouping.List1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.TestContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

public class CrossBrokerRpcTest {

    protected RpcProviderRegistry baRpcRegistry;
    protected RpcProvisionRegistry biRpcRegistry;
    private BindingTestContext testContext;
    private RpcImplementation biRpcInvoker;
    private MessageCapturingFlowService knockService;

    public static final NodeId NODE_A = new NodeId("a");
    public static final NodeId NODE_B = new NodeId("b");
    public static final NodeId NODE_C = new NodeId("c");
    public static final NodeId NODE_D = new NodeId("d");

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final QName KNOCK_KNOCK_QNAME = QName.create(List1.QNAME, "knock-knock");

    public static final InstanceIdentifier<Node> BA_NODE_A_ID = createBANodeIdentifier(NODE_A);
    public static final InstanceIdentifier<Node> BA_NODE_B_ID = createBANodeIdentifier(NODE_B);
    public static final InstanceIdentifier<Node> BA_NODE_C_ID = createBANodeIdentifier(NODE_C);
    public static final InstanceIdentifier<Node> BA_NODE_D_ID = createBANodeIdentifier(NODE_D);

    public static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier BI_NODE_A_ID = createBINodeIdentifier(NODE_A);
    public static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier BI_NODE_B_ID = createBINodeIdentifier(NODE_B);
    public static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier BI_NODE_C_ID = createBINodeIdentifier(NODE_C);
    public static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier BI_NODE_D_ID = createBINodeIdentifier(NODE_D);



    @Before
    public void setup() {
        BindingBrokerTestFactory testFactory = new BindingBrokerTestFactory();
        testFactory.setExecutor(MoreExecutors.sameThreadExecutor());
        testFactory.setStartWithParsedSchema(true);
        testContext = testFactory.getTestContext();

        testContext.start();
        baRpcRegistry = testContext.getBindingRpcRegistry();
        biRpcRegistry = testContext.getDomRpcRegistry();
        biRpcInvoker = testContext.getDomRpcInvoker();
        assertNotNull(baRpcRegistry);
        assertNotNull(biRpcRegistry);

        knockService = MessageCapturingFlowService.create(baRpcRegistry);

    }

    @Test
    public void bindingRoutedRpcProvider_DomInvokerTest() throws Exception {

        knockService//
                .registerPath(TestContext.class, BA_NODE_A_ID) //
                .registerPath(TestContext.class, BA_NODE_B_ID) //
                .setKnockKnockResult(knockResult(true, "open"));

        OpendaylightOfMigrationTestModelService baKnockInvoker =
                baRpcRegistry.getRpcService(OpendaylightOfMigrationTestModelService.class);
        assertNotSame(knockService, baKnockInvoker);

        KnockKnockInput knockKnockA = knockKnock(BA_NODE_A_ID) //
                .setQuestion("who's there?").build();

        CompositeNode knockKnockDom = toDomRpc(KNOCK_KNOCK_QNAME, knockKnockA);
        assertNotNull(knockKnockDom);
        RpcResult<CompositeNode> domResult = biRpcInvoker.invokeRpc(KNOCK_KNOCK_QNAME, knockKnockDom).get();
        assertNotNull(domResult);
        assertTrue("DOM result is successful.", domResult.isSuccessful());
        assertTrue("Bidning Add Flow RPC was captured.", knockService.getReceivedKnocks().containsKey(BA_NODE_A_ID));
        assertEquals(knockKnockA, knockService.getReceivedKnocks().get(BA_NODE_A_ID).iterator().next());
    }

    @Test
    public void bindingRpcInvoker_DomRoutedProviderTest() throws Exception {
        KnockKnockOutputBuilder builder = new KnockKnockOutputBuilder();
        builder.setAnswer("open");
        final KnockKnockOutput output = builder.build();
        org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration registration = biRpcRegistry.addRoutedRpcImplementation(KNOCK_KNOCK_QNAME, new RpcImplementation() {
            @Override
            public Set<QName> getSupportedRpcs() {
                return ImmutableSet.of(KNOCK_KNOCK_QNAME);
            }

            @Override
            public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, CompositeNode input) {
                CompositeNode result = testContext.getBindingToDomMappingService().toDataDom(output);
                return Futures.immediateFuture(RpcResultBuilder.<CompositeNode>success(result).build());
            }
        });
        registration.registerPath(NodeContext.QNAME, BI_NODE_C_ID);

        OpendaylightOfMigrationTestModelService baKnockInvoker =
                baRpcRegistry.getRpcService(OpendaylightOfMigrationTestModelService.class);
        Future<RpcResult<KnockKnockOutput>> baResult = baKnockInvoker.knockKnock((knockKnock(BA_NODE_C_ID).setQuestion("Who's there?").build()));
        assertNotNull(baResult);
        assertEquals(output,baResult.get().getResult());
    }

    private CompositeNode toDomRpcInput(DataObject addFlowA) {
        return testContext.getBindingToDomMappingService().toDataDom(addFlowA);
    }

    @After
    public void teardown() throws Exception {
        testContext.close();
    }

    private static InstanceIdentifier<Node> createBANodeIdentifier(NodeId node) {
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(node)).toInstance();
    }

    private static org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier createBINodeIdentifier(NodeId node) {
        return org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder().node(Nodes.QNAME)
                .nodeWithKey(Node.QNAME, NODE_ID_QNAME, node.getValue()).toInstance();
    }

    private Future<RpcResult<KnockKnockOutput>> knockResult(boolean success, String answer) {
        KnockKnockOutput output = new KnockKnockOutputBuilder() //
                .setAnswer(answer).build();
        RpcResult<KnockKnockOutput> result = RpcResultBuilder.<KnockKnockOutput>status(success).withResult(output).build();
        return Futures.immediateFuture(result);
    }

    private static KnockKnockInputBuilder knockKnock(InstanceIdentifier<Node> nodeId) {
        KnockKnockInputBuilder builder = new KnockKnockInputBuilder();
        builder.setKnockerId(nodeId);
        return builder;
    }

    private CompositeNode toDomRpc(QName rpcName, KnockKnockInput knockInput) {
        return new CompositeNodeTOImpl(rpcName, null,
                Collections.<org.opendaylight.yangtools.yang.data.api.Node<?>> singletonList(toDomRpcInput(knockInput)));
    }
}
