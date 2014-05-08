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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.util.concurrent.ListenableFuture;

public class RestconfImplTest {

    private RestconfImpl restconfImpl = null;
    private static ControllerContext controllerContext = null;

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils
                .loadModulesFrom("/full-versions/yangs");
        assertNotNull(allModules);
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

    @Test
    public void testExample() throws FileNotFoundException {
        CompositeNode loadedCompositeNode = TestUtils.readInputToCnSn(
                "/parts/ietf-interfaces_interfaces.xml",
                XmlToCompositeNodeProvider.INSTANCE);
        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(brokerFacade.readOperationalData(any(InstanceIdentifier.class)))
                .thenReturn(loadedCompositeNode);
        assertEquals(loadedCompositeNode,
                brokerFacade.readOperationalData(null));
    }

    @Test
    public void testInvokeRpcWithNoPayloadRpc_Fail() {
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
