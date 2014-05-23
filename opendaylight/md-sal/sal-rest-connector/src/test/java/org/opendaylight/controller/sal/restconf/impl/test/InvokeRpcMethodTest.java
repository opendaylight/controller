/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2014 Brocade Communications Systems, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.MutableCompositeNode;
import org.opendaylight.yangtools.yang.data.api.MutableSimpleNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.util.concurrent.ListenableFuture;

public class InvokeRpcMethodTest {

    private RestconfImpl restconfImpl = null;
    private static ControllerContext controllerContext = null;

    private class AnswerImpl implements Answer<RpcResult<CompositeNode>> {
        @Override
        public RpcResult<CompositeNode> answer(final InvocationOnMock invocation) throws Throwable {
            CompositeNode compNode = (CompositeNode) invocation.getArguments()[1];
            return new DummyRpcResult.Builder<CompositeNode>().result(compNode).isSuccessful(true).build();
        }
    }

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = new HashSet<Module>( TestUtils
                .loadModulesFrom("/full-versions/yangs") );
        allModules.addAll( TestUtils.loadModulesFrom("/invoke-rpc") );
        assertNotNull(allModules);
        Module module = TestUtils.resolveModule("invoke-rpc-module", allModules);
        assertNotNull(module);
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext = spy( ControllerContext.getInstance() );
        controllerContext.setSchemas(schemaContext);

    }

    @Before
    public void initMethod()
    {
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setControllerContext( controllerContext );
    }

    /**
     * Test method invokeRpc in RestconfImpl class tests if composite node as
     * input parameter of method invokeRpc (second argument) is wrapped to
     * parent composite node which has QName equals to QName of rpc (resolved
     * from string - first argument).
     */
    @Test
    public void invokeRpcMtethodTest() {
        ControllerContext contContext = controllerContext;
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
        assertTrue(structData == null);

    }

    private CompositeNode preparePayload() {
        MutableCompositeNode cont = NodeFactory.createMutableCompositeNode(
                TestUtils.buildQName("cont", "nmspc", "2013-12-04"), null, null, ModifyAction.CREATE, null);
        MutableSimpleNode<?> lf = NodeFactory.createMutableSimpleNode(
                TestUtils.buildQName("lf", "nmspc", "2013-12-04"), cont, "any value", ModifyAction.CREATE, null);
        cont.getValue().add(lf);
        cont.init();

        return cont;
    }

    @Test
    public void testInvokeRpcWithNoPayloadRpc_FailNoErrors() {
        RpcResult<CompositeNode> rpcResult = mock(RpcResult.class);
        when(rpcResult.isSuccessful()).thenReturn(false);

        ArgumentCaptor<CompositeNode> payload = ArgumentCaptor
                .forClass(CompositeNode.class);
        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(
                brokerFacade.invokeRpc(
                        eq(QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)cancel-toast")),
                        payload.capture())).thenReturn(rpcResult);

        restconfImpl.setBroker(brokerFacade);

        try {
            restconfImpl.invokeRpc("toaster:cancel-toast", "");
            fail("Expected an exception to be thrown.");
        } catch (ResponseException e) {
            assertEquals(e.getMessage(),
                    Status.INTERNAL_SERVER_ERROR.getStatusCode(), e
                    .getResponse().getStatus());
        }
    }

    @Test
    public void testInvokeRpcWithNoPayloadRpc_FailWithRpcError() {
        List<RpcError> rpcErrors = new LinkedList<RpcError>();

        RpcError unknownError = mock(RpcError.class);
        when( unknownError.getTag() ).thenReturn( "bogusTag" );
        rpcErrors.add( unknownError );

        RpcError knownError = mock( RpcError.class );
        when( knownError.getTag() ).thenReturn( "in-use" );
        rpcErrors.add( knownError );

        RpcResult<CompositeNode> rpcResult = mock(RpcResult.class);
        when(rpcResult.isSuccessful()).thenReturn(false);
        when(rpcResult.getErrors()).thenReturn( rpcErrors  );

        ArgumentCaptor<CompositeNode> payload = ArgumentCaptor
                .forClass(CompositeNode.class);
        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(
                brokerFacade.invokeRpc(
                        eq(QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)cancel-toast")),
                        payload.capture())).thenReturn(rpcResult);

        restconfImpl.setBroker(brokerFacade);

        try {
            restconfImpl.invokeRpc("toaster:cancel-toast", "");
            fail("Expected an exception to be thrown.");
        } catch (ResponseException e) {
            //TODO: Change to a 409 in the future - waiting on additional BUG to enhance this.
            assertEquals(e.getMessage(), 500, e.getResponse().getStatus());
        }
    }

    @Test
    public void testInvokeRpcWithNoPayload_Success() {
        RpcResult<CompositeNode> rpcResult = mock(RpcResult.class);
        when(rpcResult.isSuccessful()).thenReturn(true);

        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(
                brokerFacade.invokeRpc(
                        eq(QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)cancel-toast")),
                        any( CompositeNode.class ))).thenReturn(rpcResult);

        restconfImpl.setBroker(brokerFacade);

        StructuredData output = restconfImpl.invokeRpc("toaster:cancel-toast",
                "");
        assertEquals(null, output);
        //additional validation in the fact that the restconfImpl does not throw an exception.
    }

    @Test
    public void testInvokeRpcMethodExpectingNoPayloadButProvidePayload() {
        try {
            restconfImpl.invokeRpc("toaster:cancel-toast", " a payload ");
            fail("Expected an exception");
        } catch (ResponseException e) {
            assertEquals(e.getMessage(),
                    Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), e
                    .getResponse().getStatus());
        }
    }

    @Test
    public void testInvokeRpcMethodWithBadMethodName() {
        try {
            restconfImpl.invokeRpc("toaster:bad-method", "");
            fail("Expected an exception");
        } catch (ResponseException e) {
            assertEquals(e.getMessage(), Status.NOT_FOUND.getStatusCode(), e
                    .getResponse().getStatus());
        }
    }

    @Test
    public void testInvokeRpcMethodWithInput() {
        RpcResult<CompositeNode> rpcResult = mock(RpcResult.class);
        when(rpcResult.isSuccessful()).thenReturn(true);

        CompositeNode payload = mock(CompositeNode.class);

        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(
                brokerFacade.invokeRpc(
                        eq(QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)make-toast")),
                        any(CompositeNode.class))).thenReturn(rpcResult);

        restconfImpl.setBroker(brokerFacade);

        StructuredData output = restconfImpl.invokeRpc("toaster:make-toast",
                payload);
        assertEquals(null, output);
        //additional validation in the fact that the restconfImpl does not throw an exception.
    }

    @Test
    public void testThrowExceptionWhenSlashInModuleName() {
        try {
            restconfImpl.invokeRpc("toaster/slash", "");
            fail("Expected an exception.");
        } catch (ResponseException e) {
            assertEquals(e.getMessage(), Status.NOT_FOUND.getStatusCode(), e
                    .getResponse().getStatus());
        }
    }

    @Test
    public void testInvokeRpcWithNoPayloadWithOutput_Success() {
        RpcResult<CompositeNode> rpcResult = mock(RpcResult.class);
        when(rpcResult.isSuccessful()).thenReturn(true);

        CompositeNode compositeNode = mock( CompositeNode.class );
        when( rpcResult.getResult() ).thenReturn( compositeNode );

        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when( brokerFacade.invokeRpc(
                eq(QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)testOutput")),
                any( CompositeNode.class ))).thenReturn(rpcResult);

        restconfImpl.setBroker(brokerFacade);

        StructuredData output = restconfImpl.invokeRpc("toaster:testOutput",
                "");
        assertNotNull( output );
        assertSame( compositeNode, output.getData() );
        assertNotNull( output.getSchema() );
    }

    @Test
    public void testMountedRpcCallNoPayload_Success() throws Exception
    {
        RpcResult<CompositeNode> rpcResult = mock(RpcResult.class);
        when(rpcResult.isSuccessful()).thenReturn(true);

        ListenableFuture<RpcResult<CompositeNode>> mockListener = mock( ListenableFuture.class );
        when( mockListener.get() ).thenReturn( rpcResult );

        QName cancelToastQName = QName.create( "cancelToast" );

        RpcDefinition mockRpc = mock( RpcDefinition.class );
        when( mockRpc.getQName() ).thenReturn( cancelToastQName );

        MountInstance mockMountPoint = mock( MountInstance.class );
        when( mockMountPoint.rpc( eq( cancelToastQName ), any( CompositeNode.class ) ) )
        .thenReturn( mockListener );

        InstanceIdWithSchemaNode mockedInstanceId = mock( InstanceIdWithSchemaNode.class );
        when( mockedInstanceId.getMountPoint() ).thenReturn( mockMountPoint );

        ControllerContext mockedContext = mock( ControllerContext.class );
        String cancelToastStr = "toaster:cancel-toast";
        when( mockedContext.urlPathArgDecode( cancelToastStr ) ).thenReturn( cancelToastStr );
        when( mockedContext.getRpcDefinition( cancelToastStr ) ).thenReturn( mockRpc );
        when( mockedContext.toMountPointIdentifier(  "opendaylight-inventory:nodes/node/"
                + "REMOTE_HOST/yang-ext:mount/toaster:cancel-toast" ) ).thenReturn( mockedInstanceId );

        restconfImpl.setControllerContext( mockedContext );
        StructuredData output = restconfImpl.invokeRpc(
                "opendaylight-inventory:nodes/node/REMOTE_HOST/yang-ext:mount/toaster:cancel-toast",
                "");
        assertEquals(null, output);

        //additional validation in the fact that the restconfImpl does not throw an exception.
    }


}
