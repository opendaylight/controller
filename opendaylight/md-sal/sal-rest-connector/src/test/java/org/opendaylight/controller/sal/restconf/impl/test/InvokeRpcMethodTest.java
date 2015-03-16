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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationNotAvailableException;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.sal.core.api.RpcProvisionRegistry;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.NormalizedNodeContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.util.SchemaNodeUtils;

public class InvokeRpcMethodTest {

    private RestconfImpl restconfImpl = null;
    private static ControllerContext controllerContext = null;
    private static UriInfo uriInfo;


    @BeforeClass
    public static void init() throws FileNotFoundException {
        final Set<Module> allModules = new HashSet<Module>(TestUtils.loadModulesFrom("/full-versions/yangs"));
        allModules.addAll(TestUtils.loadModulesFrom("/invoke-rpc"));
        assertNotNull(allModules);
        final Module module = TestUtils.resolveModule("invoke-rpc-module", allModules);
        assertNotNull(module);
        final SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext = spy(ControllerContext.getInstance());
        controllerContext.setSchemas(schemaContext);
        uriInfo = mock(UriInfo.class);
        final MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.put("prettyPrint", Collections.singletonList("true"));
        when(uriInfo.getQueryParameters(any(Boolean.class))).thenReturn(map);
    }

    @Before
    public void initMethod() {
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setControllerContext(controllerContext);
    }

    /**
     * Test method invokeRpc in RestconfImpl class tests if composite node as input parameter of method invokeRpc
     * (second argument) is wrapped to parent composite node which has QName equals to QName of rpc (resolved from
     * string - first argument).
     */
    @Test
    @Ignore
    public void invokeRpcMethodTest() {
        final ControllerContext contContext = controllerContext;
        try {
            contContext.findModuleNameByNamespace(new URI("invoke:rpc:module"));
        } catch (final URISyntaxException e) {
            assertTrue("Uri wasn't created sucessfuly", false);
        }

        final BrokerFacade mockedBrokerFacade = mock(BrokerFacade.class);

        final RestconfImpl restconf = RestconfImpl.getInstance();
        restconf.setBroker(mockedBrokerFacade);
        restconf.setControllerContext(contContext);

        final NormalizedNodeContext payload = prepareDomPayload();

        final NormalizedNodeContext rpcResponse = restconf.invokeRpc("invoke-rpc-module:rpc-test", payload, uriInfo);
        assertTrue(rpcResponse != null);
        assertTrue(rpcResponse.getData() == null);

    }

    private NormalizedNodeContext prepareDomPayload() {
        final SchemaContext schema = controllerContext.getGlobalSchema();
        final Module rpcModule = schema.findModuleByName("invoke-rpc-module", null);
        assertNotNull(rpcModule);
        final QName rpcQName = QName.create(rpcModule.getQNameModule(), "rpc-test");
        final QName rpcInputQName = QName.create(rpcModule.getQNameModule(),"input");
        final Set<RpcDefinition> setRpcs = rpcModule.getRpcs();
        ContainerSchemaNode rpcInputSchemaNode = null;
        for (final RpcDefinition rpc : setRpcs) {
            if (rpcQName.isEqualWithoutRevision(rpc.getQName())) {
                rpcInputSchemaNode = SchemaNodeUtils.getRpcDataSchema(rpc, rpcInputQName);
                break;
            }
        }
        assertNotNull(rpcInputSchemaNode);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> container = Builders.containerBuilder(rpcInputSchemaNode);

        final QName contQName = QName.create(rpcModule.getQNameModule(), "cont");
        final DataSchemaNode contSchemaNode = rpcInputSchemaNode.getDataChildByName(contQName);
        assertTrue(contSchemaNode instanceof ContainerSchemaNode);
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> contNode = Builders.containerBuilder((ContainerSchemaNode) contSchemaNode);

        final QName lfQName = QName.create(rpcModule.getQNameModule(), "lf");
        final DataSchemaNode lfSchemaNode = ((ContainerSchemaNode) contSchemaNode).getDataChildByName(lfQName);
        assertTrue(lfSchemaNode instanceof LeafSchemaNode);
        final LeafNode<Object> lfNode = (Builders.leafBuilder((LeafSchemaNode) lfSchemaNode).withValue("any value")).build();
        contNode.withChild(lfNode);
        container.withChild(contNode.build());

        return new NormalizedNodeContext(new InstanceIdentifierContext(null, rpcInputSchemaNode, null, schema), container.build());
    }

    @Test
    public void testInvokeRpcWithNoPayloadRpc_FailNoErrors() {
        final DOMRpcException exception = new DOMRpcImplementationNotAvailableException("testExeption");
        final CheckedFuture<DOMRpcResult, DOMRpcException> future = Futures.immediateFailedCheckedFuture(exception);

        final BrokerFacade brokerFacade = mock(BrokerFacade.class);

        final QName qname = QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)cancel-toast");
        final SchemaPath type = SchemaPath.create(true, qname);

        when(brokerFacade.invokeRpc(eq(type), any(NormalizedNode.class))).thenReturn(future);

        restconfImpl.setBroker(brokerFacade);

        try {
            restconfImpl.invokeRpc("toaster:cancel-toast", "", uriInfo);
            fail("Expected an exception to be thrown.");
        } catch (final RestconfDocumentedException e) {
            verifyRestconfDocumentedException(e, 0, ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED,
                    Optional.<String> absent(), Optional.<String> absent());
        }
    }

    void verifyRestconfDocumentedException(final RestconfDocumentedException e, final int index,
            final ErrorType expErrorType, final ErrorTag expErrorTag, final Optional<String> expErrorMsg,
            final Optional<String> expAppTag) {
        RestconfError actual = null;
        try {
            actual = e.getErrors().get(index);
        } catch (final ArrayIndexOutOfBoundsException ex) {
            fail("RestconfError not found at index " + index);
        }

        assertEquals("getErrorType", expErrorType, actual.getErrorType());
        assertEquals("getErrorTag", expErrorTag, actual.getErrorTag());
        assertNotNull("getErrorMessage is null", actual.getErrorMessage());

        if (expErrorMsg.isPresent()) {
            assertEquals("getErrorMessage", expErrorMsg.get(), actual.getErrorMessage());
        }

        if (expAppTag.isPresent()) {
            assertEquals("getErrorAppTag", expAppTag.get(), actual.getErrorAppTag());
        }
    }

    @Test
    public void testInvokeRpcWithNoPayloadRpc_FailWithRpcError() {
        final List<RpcError> rpcErrors = Arrays.asList(
            RpcResultBuilder.newError( RpcError.ErrorType.TRANSPORT, "bogusTag", "foo" ),
            RpcResultBuilder.newWarning( RpcError.ErrorType.RPC, "in-use", "bar",
                                         "app-tag", null, null ) );

        final DOMRpcResult resutl = new DefaultDOMRpcResult(rpcErrors);
        final CheckedFuture<DOMRpcResult, DOMRpcException> future = Futures.immediateCheckedFuture(resutl);

        final SchemaPath path = SchemaPath.create(true,
                QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)cancel-toast"));

        final BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(brokerFacade.invokeRpc(eq(path), any(NormalizedNode.class))).thenReturn(future);

        restconfImpl.setBroker(brokerFacade);

        try {
            restconfImpl.invokeRpc("toaster:cancel-toast", "", uriInfo);
            fail("Expected an exception to be thrown.");
        } catch (final RestconfDocumentedException e) {
            verifyRestconfDocumentedException(e, 0, ErrorType.TRANSPORT, ErrorTag.OPERATION_FAILED, Optional.of("foo"),
                    Optional.<String> absent());
            verifyRestconfDocumentedException(e, 1, ErrorType.RPC, ErrorTag.IN_USE, Optional.of("bar"),
                    Optional.of("app-tag"));
        }
    }

    @Test
    public void testInvokeRpcWithNoPayload_Success() {
        final NormalizedNode<?, ?> resultObj = null;
        final DOMRpcResult expResult = new DefaultDOMRpcResult(resultObj);
        final CheckedFuture<DOMRpcResult, DOMRpcException> future = Futures.immediateCheckedFuture(expResult);

        final QName qname = QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)cancel-toast");
        final SchemaPath path = SchemaPath.create(true, qname);

        final BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(brokerFacade.invokeRpc(eq(path), any (NormalizedNode.class))).thenReturn(future);

        restconfImpl.setBroker(brokerFacade);

        final NormalizedNodeContext output = restconfImpl.invokeRpc("toaster:cancel-toast", "", uriInfo);
        assertNotNull(output);
        assertEquals(null, output.getData());
        // additional validation in the fact that the restconfImpl does not
        // throw an exception.
    }

    @Test
    public void testInvokeRpcMethodExpectingNoPayloadButProvidePayload() {
        try {
            restconfImpl.invokeRpc("toaster:cancel-toast", " a payload ", uriInfo);
            fail("Expected an exception");
        } catch (final RestconfDocumentedException e) {
            verifyRestconfDocumentedException(e, 0, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                    Optional.<String> absent(), Optional.<String> absent());
        }
    }

    @Test
    public void testInvokeRpcMethodWithBadMethodName() {
        try {
            restconfImpl.invokeRpc("toaster:bad-method", "", uriInfo);
            fail("Expected an exception");
        } catch (final RestconfDocumentedException e) {
            verifyRestconfDocumentedException(e, 0, ErrorType.RPC, ErrorTag.UNKNOWN_ELEMENT,
                    Optional.<String> absent(), Optional.<String> absent());
        }
    }

    @Test
    @Ignore
    public void testInvokeRpcMethodWithInput() {
        final DOMRpcResult expResult = mock(DOMRpcResult.class);
        final CheckedFuture<DOMRpcResult, DOMRpcException> future = Futures.immediateCheckedFuture(expResult);
        final SchemaPath path = SchemaPath.create(true,
                QName.create("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)make-toast"));

        final SchemaContext schemaContext = controllerContext.getGlobalSchema();
        final Module rpcModule = schemaContext.findModuleByName("toaster", null);
        assertNotNull(rpcModule);
        final QName rpcQName = QName.create(rpcModule.getQNameModule(), "make-toast");
        final QName rpcInputQName = QName.create(rpcModule.getQNameModule(),"input");

        final Set<RpcDefinition> setRpcs = rpcModule.getRpcs();
        RpcDefinition rpcDef = null;
        ContainerSchemaNode rpcInputSchemaNode = null;

        for (final RpcDefinition rpc : setRpcs) {
            if (rpcQName.isEqualWithoutRevision(rpc.getQName())) {
                rpcInputSchemaNode = SchemaNodeUtils.getRpcDataSchema(rpc, rpcInputQName);
                rpcDef = rpc;
                break;
            }
        }

        assertNotNull(rpcDef);
        assertNotNull(rpcInputSchemaNode);
        assertTrue(rpcInputSchemaNode instanceof ContainerSchemaNode);
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> containerBuilder =
                Builders.containerBuilder(rpcInputSchemaNode);

        final NormalizedNodeContext payload = new NormalizedNodeContext(new InstanceIdentifierContext(null, rpcInputSchemaNode,
                null, schemaContext), containerBuilder.build());

        final BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(brokerFacade.invokeRpc(eq(path), any(NormalizedNode.class))).thenReturn(future);
        restconfImpl.setBroker(brokerFacade);

        final NormalizedNodeContext output = restconfImpl.invokeRpc("toaster:make-toast", payload, uriInfo);
        assertNotNull(output);
        assertEquals(null, output.getData());
        // additional validation in the fact that the restconfImpl does not
        // throw an exception.
    }

    @Test
    public void testThrowExceptionWhenSlashInModuleName() {
        try {
            restconfImpl.invokeRpc("toaster/slash", "", uriInfo);
            fail("Expected an exception.");
        } catch (final RestconfDocumentedException e) {
            verifyRestconfDocumentedException(e, 0, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                    Optional.<String> absent(), Optional.<String> absent());
        }
    }

    @Test
    public void testInvokeRpcWithNoPayloadWithOutput_Success() {
        final SchemaContext schema = controllerContext.getGlobalSchema();
        final Module rpcModule = schema.findModuleByName("toaster", null);
        assertNotNull(rpcModule);
        final QName rpcQName = QName.create(rpcModule.getQNameModule(), "testOutput");
        final QName rpcOutputQName = QName.create(rpcModule.getQNameModule(),"output");

        final Set<RpcDefinition> setRpcs = rpcModule.getRpcs();
        RpcDefinition rpcDef = null;
        ContainerSchemaNode rpcOutputSchemaNode = null;
        for (final RpcDefinition rpc : setRpcs) {
            if (rpcQName.isEqualWithoutRevision(rpc.getQName())) {
                rpcOutputSchemaNode = SchemaNodeUtils.getRpcDataSchema(rpc, rpcOutputQName);
                rpcDef = rpc;
                break;
            }
        }
        assertNotNull(rpcDef);
        assertNotNull(rpcOutputSchemaNode);
        assertTrue(rpcOutputSchemaNode instanceof ContainerSchemaNode);
        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> containerBuilder =
                Builders.containerBuilder(rpcOutputSchemaNode);
        final DataSchemaNode leafSchema = rpcOutputSchemaNode
                .getDataChildByName(QName.create(rpcModule.getQNameModule(), "textOut"));
        assertTrue(leafSchema instanceof LeafSchemaNode);
        final NormalizedNodeAttrBuilder<NodeIdentifier, Object, LeafNode<Object>> leafBuilder =
                Builders.leafBuilder((LeafSchemaNode) leafSchema);
        leafBuilder.withValue("brm");
        containerBuilder.withChild(leafBuilder.build());
        final ContainerNode container = containerBuilder.build();

        final DOMRpcResult result = new DefaultDOMRpcResult(container);
        final CheckedFuture<DOMRpcResult, DOMRpcException> future = Futures.immediateCheckedFuture(result);

        final BrokerFacade brokerFacade = mock(BrokerFacade.class);
        when(brokerFacade.invokeRpc(eq(rpcDef.getPath()), any(NormalizedNode.class))).thenReturn(future);

        restconfImpl.setBroker(brokerFacade);

        final NormalizedNodeContext output = restconfImpl.invokeRpc("toaster:testOutput", "", uriInfo);
        assertNotNull(output);
        assertNotNull(output.getData());
        assertSame(container, output.getData());
        assertNotNull(output.getInstanceIdentifierContext());
        assertNotNull(output.getInstanceIdentifierContext().getSchemaContext());
    }

    /**
     *
     * Tests calling of RestConfImpl method invokeRpc. In the method there is searched rpc in remote schema context.
     * This rpc is then executed.
     *
     * I wasn't able to simulate calling of rpc on remote device therefore this testing method raise method when rpc is
     * invoked.
     */
    @Test
    @Ignore // FIXME find how to use mockito for it or possibly delete this test
    public void testMountedRpcCallNoPayload_Success() throws Exception {
//        final RpcResult<CompositeNode> rpcResult = RpcResultBuilder.<CompositeNode>success().build();
//
//        final ListenableFuture<RpcResult<CompositeNode>> mockListener = mock(ListenableFuture.class);
//        when(mockListener.get()).thenReturn(rpcResult);
//
//        final QName cancelToastQName = QName.create("namespace", "2014-05-28", "cancelToast");
//
//        final RpcDefinition mockRpc = mock(RpcDefinition.class);
//        when(mockRpc.getQName()).thenReturn(cancelToastQName);
//
//        final DOMMountPoint mockMountPoint = mock(DOMMountPoint.class);
//        final RpcProvisionRegistry mockedRpcProvisionRegistry = mock(RpcProvisionRegistry.class);
//        when(mockedRpcProvisionRegistry.invokeRpc(eq(cancelToastQName), any(CompositeNode.class))).thenReturn(mockListener);
//        when(mockMountPoint.getService(eq(RpcProvisionRegistry.class))).thenReturn(Optional.of(mockedRpcProvisionRegistry));
//        when(mockMountPoint.getSchemaContext()).thenReturn(TestUtils.loadSchemaContext("/invoke-rpc"));
//
//        final InstanceIdentifierContext mockedInstanceId = mock(InstanceIdentifierContext.class);
//        when(mockedInstanceId.getMountPoint()).thenReturn(mockMountPoint);
//
//        final ControllerContext mockedContext = mock(ControllerContext.class);
//        final String rpcNoop = "invoke-rpc-module:rpc-noop";
//        when(mockedContext.urlPathArgDecode(rpcNoop)).thenReturn(rpcNoop);
//        when(mockedContext.getRpcDefinition(rpcNoop)).thenReturn(mockRpc);
//        when(
//                mockedContext.toMountPointIdentifier(eq("opendaylight-inventory:nodes/node/"
//                        + "REMOTE_HOST/yang-ext:mount/invoke-rpc-module:rpc-noop"))).thenReturn(mockedInstanceId);
//
//        restconfImpl.setControllerContext(mockedContext);
//        try {
//            restconfImpl.invokeRpc(
//                    "opendaylight-inventory:nodes/node/REMOTE_HOST/yang-ext:mount/invoke-rpc-module:rpc-noop", "",
//                    uriInfo);
//            fail("RestconfDocumentedException wasn't raised");
//        } catch (final RestconfDocumentedException e) {
//            final List<RestconfError> errors = e.getErrors();
//            assertNotNull(errors);
//            assertEquals(1, errors.size());
//            assertEquals(ErrorType.APPLICATION, errors.iterator().next().getErrorType());
//            assertEquals(ErrorTag.OPERATION_FAILED, errors.iterator().next().getErrorTag());
//        }

        // additional validation in the fact that the restconfImpl does not
        // throw an exception.
    }
}
