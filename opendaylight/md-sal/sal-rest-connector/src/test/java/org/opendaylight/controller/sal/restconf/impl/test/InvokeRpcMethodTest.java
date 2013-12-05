package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.*;
import java.util.*;

import org.junit.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.sal.restconf.impl.*;
import org.opendaylight.yangtools.yang.common.*;
import org.opendaylight.yangtools.yang.data.api.*;
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
        modules = TestUtils.resolveModules("/invoke-rpc");
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
            contContext.findModuleByNamespace(new URI("invoke:rpc:module"));
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
