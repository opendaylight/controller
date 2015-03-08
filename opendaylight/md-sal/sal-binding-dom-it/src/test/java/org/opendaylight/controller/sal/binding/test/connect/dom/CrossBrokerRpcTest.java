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

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.test.util.BindingBrokerTestFactory;
import org.opendaylight.controller.sal.binding.test.util.BindingTestContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.KnockKnockInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.KnockKnockInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.KnockKnockOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.KnockKnockOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.of.migration.test.model.rev150210.OpendaylightOfMigrationTestModelService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.rpc.routing.rev140701.TestContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public class CrossBrokerRpcTest {

    protected RpcProviderRegistry providerRegistry;
    protected DOMRpcProviderService provisionRegistry;
    private BindingTestContext testContext;
    private DOMRpcService biRpcInvoker;
    private MessageCapturingFlowService knockService;

    public static final TopLevelListKey NODE_A = new TopLevelListKey("a");
    public static final TopLevelListKey NODE_B = new TopLevelListKey("b");
    public static final TopLevelListKey NODE_C = new TopLevelListKey("c");

    private static final QName NODE_ID_QNAME = QName.create(TopLevelList.QNAME, "name");
    private static final QName KNOCK_KNOCK_QNAME = QName.create(KnockKnockOutput.QNAME, "knock-knock");
    private static final SchemaPath KNOCK_KNOCK_PATH = SchemaPath.create(true, KNOCK_KNOCK_QNAME);

    public static final InstanceIdentifier<Top> NODES_PATH = InstanceIdentifier.builder(Top.class).build();
    public static final InstanceIdentifier<TopLevelList> BA_NODE_A_ID = NODES_PATH.child(TopLevelList.class, NODE_A);
    public static final InstanceIdentifier<TopLevelList> BA_NODE_B_ID = NODES_PATH.child(TopLevelList.class, NODE_B);
    public static final InstanceIdentifier<TopLevelList> BA_NODE_C_ID = NODES_PATH.child(TopLevelList.class, NODE_C);

    public static final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier BI_NODE_C_ID = createBINodeIdentifier(NODE_C);


    @Before
    public void setup() {
        BindingBrokerTestFactory testFactory = new BindingBrokerTestFactory();
        testFactory.setExecutor(MoreExecutors.sameThreadExecutor());
        testFactory.setStartWithParsedSchema(true);
        testContext = testFactory.getTestContext();

        testContext.start();
        providerRegistry = testContext.getBindingRpcRegistry();
        provisionRegistry = testContext.getDomRpcRegistry();
        biRpcInvoker = testContext.getDomRpcInvoker();
        assertNotNull(providerRegistry);
        assertNotNull(provisionRegistry);

        knockService = MessageCapturingFlowService.create(providerRegistry);

    }

    @Test
    public void bindingRoutedRpcProvider_DomInvokerTest() throws Exception {

        knockService//
                .registerPath(TestContext.class, BA_NODE_A_ID) //
                .registerPath(TestContext.class, BA_NODE_B_ID) //
                .setKnockKnockResult(knockResult(true, "open"));

        OpendaylightOfMigrationTestModelService baKnockInvoker =
                providerRegistry.getRpcService(OpendaylightOfMigrationTestModelService.class);
        assertNotSame(knockService, baKnockInvoker);

        KnockKnockInput knockKnockA = knockKnock(BA_NODE_A_ID) //
                .setQuestion("who's there?").build();

        ContainerNode knockKnockDom = toDomRpc(KNOCK_KNOCK_QNAME, knockKnockA);
        assertNotNull(knockKnockDom);
        DOMRpcResult domResult = biRpcInvoker.invokeRpc(KNOCK_KNOCK_PATH, knockKnockDom).get();
        assertNotNull(domResult);
        assertNotNull("DOM result is successful.", domResult.getResult());
        assertTrue("Bidning Add Flow RPC was captured.", knockService.getReceivedKnocks().containsKey(BA_NODE_A_ID));
        assertEquals(knockKnockA, knockService.getReceivedKnocks().get(BA_NODE_A_ID).iterator().next());
    }

    @Test
    public void bindingRpcInvoker_DomRoutedProviderTest() throws Exception {
        KnockKnockOutputBuilder builder = new KnockKnockOutputBuilder();
        builder.setAnswer("open");
        final KnockKnockOutput output = builder.build();

        provisionRegistry.registerRpcImplementation(new DOMRpcImplementation() {

            @Override
            public CheckedFuture<DOMRpcResult, DOMRpcException> invokeRpc(DOMRpcIdentifier rpc, NormalizedNode<?, ?> input) {
                ContainerNode result = testContext.getCodec().getCodecFactory().toNormalizedNodeRpcData(output);
                return Futures.<DOMRpcResult, DOMRpcException>immediateCheckedFuture(new DefaultDOMRpcResult(result));
            }
        }, DOMRpcIdentifier.create(KNOCK_KNOCK_PATH, BI_NODE_C_ID));

        OpendaylightOfMigrationTestModelService baKnockInvoker =
                providerRegistry.getRpcService(OpendaylightOfMigrationTestModelService.class);
        Future<RpcResult<KnockKnockOutput>> baResult = baKnockInvoker.knockKnock((knockKnock(BA_NODE_C_ID).setQuestion("Who's there?").build()));
        assertNotNull(baResult);
        assertEquals(output, baResult.get().getResult());
    }

    private ContainerNode toDomRpcInput(DataObject addFlowA) {
        return testContext.getCodec().getCodecFactory().toNormalizedNodeRpcData(addFlowA);
    }

    @After
    public void teardown() throws Exception {
        testContext.close();
    }

    private static org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier createBINodeIdentifier(TopLevelListKey listKey) {
        return org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.builder().node(Top.QNAME)
                .node(TopLevelList.QNAME)
                .nodeWithKey(TopLevelList.QNAME, NODE_ID_QNAME, listKey.getName()).build();
    }

    private Future<RpcResult<KnockKnockOutput>> knockResult(boolean success, String answer) {
        KnockKnockOutput output = new KnockKnockOutputBuilder() //
                .setAnswer(answer).build();
        RpcResult<KnockKnockOutput> result = RpcResultBuilder.<KnockKnockOutput>status(success).withResult(output).build();
        return Futures.immediateFuture(result);
    }

    private static KnockKnockInputBuilder knockKnock(InstanceIdentifier<TopLevelList> listId) {
        KnockKnockInputBuilder builder = new KnockKnockInputBuilder();
        builder.setKnockerId(listId);
        return builder;
    }

    private ContainerNode toDomRpc(QName rpcName, KnockKnockInput knockInput) {
        return toDomRpcInput(knockInput);
    }
}
