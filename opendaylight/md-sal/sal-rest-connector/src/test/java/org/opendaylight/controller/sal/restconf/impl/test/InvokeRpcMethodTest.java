/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.Module;

public class InvokeRpcMethodTest {

    private static Set<Module> modules;

    private class AnswerImpl implements Answer<RpcResult<CompositeNode>> {
        @Override
        public RpcResult<CompositeNode> answer(InvocationOnMock invocation) throws Throwable {
            CompositeNode compNode = (CompositeNode) invocation.getArguments()[1];
            return new DummyRpcResult.Builder<CompositeNode>().result(compNode).isSuccessful(true).build();
        }
    }

    @BeforeClass
    public static void initialization() {
        modules = TestUtils.loadModulesFrom("/invoke-rpc");
        assertEquals(1, modules.size());
        Module module = TestUtils.resolveModule("invoke-rpc-module", modules);
        assertNotNull(module);
    }

    /**
     * Test method invokeRpc in RestconfImpl class tests if composite node as
     * input parameter of method invokeRpc (second argument) is wrapped to
     * parent composite node which has QName equals to QName of rpc (resolved
     * from string - first argument).
     */
    @Test
    public void invokeRpcMethodTest() {
        ControllerContext contContext = ControllerContext.getInstance();
        contContext.onGlobalContextUpdated(TestUtils.loadSchemaContext(modules));
        try {
            contContext.findModuleNameByNamespace(new URI("invoke:rpc:module"));
        } catch (URISyntaxException e) {
            assertTrue("Uri wasn't created sucessfuly", false);
        }

        BrokerFacade mockedBrokerFacade = mock(BrokerFacade.class);

        RestconfImpl restconf = RestconfImpl.getInstance();
        restconf.setBroker(mockedBrokerFacade);
        restconf.setControllerContext(contContext);

        when(mockedBrokerFacade.invokeRpc(any(QName.class), any(CompositeNode.class))).thenAnswer(new AnswerImpl());

        StructuredData structData = restconf.invokeRpc("invoke-rpc-module:rpc-test", preparePayload());

        CompositeNode rpcCompNode = structData.getData();
        CompositeNode cont = null;
        assertEquals("invoke:rpc:module", rpcCompNode.getNodeType().getNamespace().toString());
        assertEquals("rpc-test", rpcCompNode.getNodeType().getLocalName());

        for (Node<?> node : rpcCompNode.getChildren()) {
            if (node.getNodeType().getLocalName().equals("cont")
                    && node.getNodeType().getNamespace().toString().equals("nmspc")) {
                if (node instanceof CompositeNode) {
                    cont = (CompositeNode) node;
                }
            }
        }
        assertNotNull(cont);

    }

    private CompositeNode preparePayload() {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont", "nmspc", "2013-12-04"), null, null, ModifyAction.CREATE, null);
        MutableSimpleNode<?> lf = NodeFactory.createMutableSimpleNode(
                TestUtils.buildQName("lf", "nmspc", "2013-12-04"), cont, "any value", ModifyAction.CREATE, null);
        cont.getChildren().add(lf);
        cont.init();

        return cont;
    }

}
