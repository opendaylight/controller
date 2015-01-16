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

import java.math.BigInteger;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.NodeFlowRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
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
    private MessageCapturingFlowService flowService;

    public static final NodeId NODE_A = new NodeId("a");
    public static final NodeId NODE_B = new NodeId("b");
    public static final NodeId NODE_C = new NodeId("c");
    public static final NodeId NODE_D = new NodeId("d");

    private static final QName NODE_ID_QNAME = QName.create(Node.QNAME, "id");
    private static final QName ADD_FLOW_QNAME = QName.create(NodeFlowRemoved.QNAME, "add-flow");

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

        flowService = MessageCapturingFlowService.create(baRpcRegistry);

    }

    @Test
    public void bindingRoutedRpcProvider_DomInvokerTest() throws Exception {

        flowService//
                .registerPath(NodeContext.class, BA_NODE_A_ID) //
                .registerPath(NodeContext.class, BA_NODE_B_ID) //
                .setAddFlowResult(addFlowResult(true, 10));

        SalFlowService baFlowInvoker = baRpcRegistry.getRpcService(SalFlowService.class);
        assertNotSame(flowService, baFlowInvoker);

        AddFlowInput addFlowA = addFlow(BA_NODE_A_ID) //
                .setPriority(100).setBarrier(true).build();

        CompositeNode addFlowDom = toDomRpc(ADD_FLOW_QNAME, addFlowA);
        assertNotNull(addFlowDom);
        RpcResult<CompositeNode> domResult = biRpcInvoker.invokeRpc(ADD_FLOW_QNAME, addFlowDom).get();
        assertNotNull(domResult);
        assertTrue("DOM result is successful.", domResult.isSuccessful());
        assertTrue("Bidning Add Flow RPC was captured.", flowService.getReceivedAddFlows().containsKey(BA_NODE_A_ID));
        assertEquals(addFlowA, flowService.getReceivedAddFlows().get(BA_NODE_A_ID).iterator().next());
    }

    @Test
    public void bindingRpcInvoker_DomRoutedProviderTest() throws Exception {
        AddFlowOutputBuilder builder = new AddFlowOutputBuilder();
        builder.setTransactionId(new TransactionId(BigInteger.valueOf(10)));
        final AddFlowOutput output = builder.build();
        org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration registration = biRpcRegistry.addRoutedRpcImplementation(ADD_FLOW_QNAME, new RpcImplementation() {
            @Override
            public Set<QName> getSupportedRpcs() {
                return ImmutableSet.of(ADD_FLOW_QNAME);
            }

            @Override
            public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(QName rpc, CompositeNode input) {
                CompositeNode result = testContext.getBindingToDomMappingService().toDataDom(output);
                return Futures.immediateFuture(RpcResultBuilder.<CompositeNode>success(result).build());
            }
        });
        registration.registerPath(NodeContext.QNAME, BI_NODE_C_ID);

        SalFlowService baFlowInvoker = baRpcRegistry.getRpcService(SalFlowService.class);
        Future<RpcResult<AddFlowOutput>> baResult = baFlowInvoker.addFlow(addFlow(BA_NODE_C_ID).setPriority(500).build());
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
        return InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(node)).build();
    }

    private static org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier createBINodeIdentifier(NodeId node) {
        return org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder().node(Nodes.QNAME)
                .nodeWithKey(Node.QNAME, NODE_ID_QNAME, node.getValue()).build();
    }

    private Future<RpcResult<AddFlowOutput>> addFlowResult(boolean success, long xid) {
        AddFlowOutput output = new AddFlowOutputBuilder() //
                .setTransactionId(new TransactionId(BigInteger.valueOf(xid))).build();
        RpcResult<AddFlowOutput> result = RpcResultBuilder.<AddFlowOutput>status(success).withResult(output).build();
        return Futures.immediateFuture(result);
    }

    private static AddFlowInputBuilder addFlow(InstanceIdentifier<Node> nodeId) {
        AddFlowInputBuilder builder = new AddFlowInputBuilder();
        builder.setNode(new NodeRef(nodeId));
        return builder;
    }

    private CompositeNode toDomRpc(QName rpcName, AddFlowInput addFlowA) {
        return new CompositeNodeTOImpl(rpcName, null,
                Collections.<org.opendaylight.yangtools.yang.data.api.Node<?>> singletonList(toDomRpcInput(addFlowA)));
    }
}
