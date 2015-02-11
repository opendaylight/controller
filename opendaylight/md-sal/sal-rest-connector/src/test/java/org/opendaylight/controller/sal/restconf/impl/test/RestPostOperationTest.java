/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.XML;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.rest.impl.RestconfDocumentedExceptionMapper;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestPostOperationTest extends JerseyTest {

    private static String xmlDataAbsolutePath;
    private static String xmlDataInterfaceAbsolutePath;
    private static String xmlDataRpcInput;
    private static String xmlBlockData;
    private static String xmlTestInterface;
    private static CompositeNodeWrapper cnSnDataOutput;
    private static String xmlData3;
    private static String xmlData4;

    private static ControllerContext controllerContext;
    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextYangsIetf;
    private static SchemaContext schemaContextTestModule;
    private static SchemaContext schemaContext;

    private static DOMMountPointService mountService;

    @BeforeClass
    public static void init() throws URISyntaxException, IOException {
        schemaContextYangsIetf = TestUtils.loadSchemaContext("/full-versions/yangs");
        schemaContextTestModule = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext = ControllerContext.getInstance();
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);

        final Set<Module> modules = TestUtils.loadModulesFrom("/test-config-data/yang1");
        schemaContext = TestUtils.loadSchemaContext(modules);

        loadData();
    }

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
        // enable(TestProperties.LOG_TRAFFIC);
        // enable(TestProperties.DUMP_ENTITY);
        // enable(TestProperties.RECORD_LOG_LEVEL);
        // set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfImpl, StructuredDataToXmlProvider.INSTANCE,
                StructuredDataToJsonProvider.INSTANCE, XmlToCompositeNodeProvider.INSTANCE,
                JsonToCompositeNodeProvider.INSTANCE);
        resourceConfig.registerClasses(RestconfDocumentedExceptionMapper.class);
        return resourceConfig;
    }

    @Test
    public void postOperationsStatusCodes() throws IOException {
        controllerContext.setSchemas(schemaContextTestModule);
        mockInvokeRpc(cnSnDataOutput, true);
        String uri = "/operations/test-module:rpc-test";
        assertEquals(200, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));

        mockInvokeRpc(null, true);
        assertEquals(204, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));

        mockInvokeRpc(null, false);
        assertEquals(500, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));

        final List<RpcError> rpcErrors = new ArrayList<>();
        rpcErrors.add(RpcResultBuilder.newError(ErrorType.RPC, "tag1", "message1", "applicationTag1", "info1", null));
        rpcErrors.add(RpcResultBuilder.newWarning(ErrorType.PROTOCOL, "tag2", "message2", "applicationTag2", "info2",
                null));
        mockInvokeRpc(null, false, rpcErrors);
        assertEquals(500, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));

        uri = "/operations/test-module:rpc-wrongtest";
        assertEquals(400, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));
    }

    @Test
    public void postConfigOnlyStatusCodes() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        final String uri = "/config";
        mockCommitConfigurationDataPostMethod(true);
        assertEquals(204, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));

        mockCommitConfigurationDataPostMethod(false);
        assertEquals(500, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));

        assertEquals(400, post(uri, MediaType.APPLICATION_XML, ""));
    }

    @Test
    public void postConfigStatusCodes() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        final String uri = "/config/ietf-interfaces:interfaces";

        mockCommitConfigurationDataPostMethod(true);
        assertEquals(204, post(uri, MediaType.APPLICATION_XML, xmlDataInterfaceAbsolutePath));

        mockCommitConfigurationDataPostMethod(false);
        assertEquals(500, post(uri, MediaType.APPLICATION_XML, xmlDataInterfaceAbsolutePath));

        assertEquals(400, post(uri, MediaType.APPLICATION_JSON, ""));
    }

    @Test
    public void postDataViaUrlMountPoint() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        when(
                brokerFacade.commitConfigurationDataPost(any(DOMMountPoint.class), any(YangInstanceIdentifier.class),
                        any(NormalizedNode.class))).thenReturn(mock(CheckedFuture.class));

        final DOMMountPoint mountInstance = mock(DOMMountPoint.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        final DOMMountPointService mockMountService = mock(DOMMountPointService.class);
        when(mockMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountInstance));

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = "/config/ietf-interfaces:interfaces/interface/0/";
        assertEquals(204, post(uri, Draft02.MediaTypes.DATA + XML, xmlData4));
        uri = "/config/ietf-interfaces:interfaces/interface/0/yang-ext:mount/test-module:cont";
        assertEquals(204, post(uri, Draft02.MediaTypes.DATA + XML, xmlData3));

        assertEquals(400, post(uri, MediaType.APPLICATION_JSON, ""));
    }

    private void mockInvokeRpc(final CompositeNode result, final boolean sucessful, final Collection<RpcError> errors) {

        final DummyRpcResult.Builder<CompositeNode> builder = new DummyRpcResult.Builder<CompositeNode>().result(result)
                .isSuccessful(sucessful);
        if (!errors.isEmpty()) {
            builder.errors(errors);
        }
        final RpcResult<CompositeNode> rpcResult = builder.build();
        when(brokerFacade.invokeRpc(any(QName.class), any(CompositeNode.class))).thenReturn(
                Futures.<RpcResult<CompositeNode>> immediateFuture(rpcResult));
    }

    private void mockInvokeRpc(final CompositeNode result, final boolean sucessful) {
        mockInvokeRpc(result, sucessful, Collections.<RpcError> emptyList());
    }

    private void mockCommitConfigurationDataPostMethod(final boolean succesfulComit) {
        if (succesfulComit) {
            doReturn(mock(CheckedFuture.class)).when(brokerFacade).commitConfigurationDataPost(any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        } else {
            doThrow(RestconfDocumentedException.class).when(brokerFacade).commitConfigurationDataPost(
                    any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        }
    }

    @Test
    public void createConfigurationDataTest() throws UnsupportedEncodingException, ParseException {
        initMocking();
        final RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(
                TransactionStatus.COMMITED).build();

        when(brokerFacade.commitConfigurationDataPost(any(YangInstanceIdentifier.class), any(NormalizedNode.class)))
                .thenReturn(mock(CheckedFuture.class));

        final ArgumentCaptor<YangInstanceIdentifier> instanceIdCaptor = ArgumentCaptor.forClass(YangInstanceIdentifier.class);
        final ArgumentCaptor<NormalizedNode> compNodeCaptor = ArgumentCaptor.forClass(NormalizedNode.class);

        final String URI_1 = "/config";
        assertEquals(204, post(URI_1, Draft02.MediaTypes.DATA + XML, xmlTestInterface));
        verify(brokerFacade).commitConfigurationDataPost(instanceIdCaptor.capture(), compNodeCaptor.capture());
        String identifier = "[(urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)interfaces]";
        assertEquals(identifier, ImmutableList.copyOf(instanceIdCaptor.getValue().getPathArguments()).toString());

        final String URI_2 = "/config/test-interface:interfaces";
        assertEquals(204, post(URI_2, Draft02.MediaTypes.DATA + XML, xmlBlockData));
        verify(brokerFacade, times(2))
                .commitConfigurationDataPost(instanceIdCaptor.capture(), compNodeCaptor.capture());
        identifier = "[(urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)interfaces, (urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)block]";
        assertEquals(identifier, ImmutableList.copyOf(instanceIdCaptor.getValue().getPathArguments()).toString());
    }

    @Test
    public void createConfigurationDataNullTest() throws UnsupportedEncodingException {
        initMocking();

        when(brokerFacade.commitConfigurationDataPost(any(YangInstanceIdentifier.class), any(NormalizedNode.class)))
                .thenReturn(null);

        final String URI_1 = "/config";
        assertEquals(204, post(URI_1, Draft02.MediaTypes.DATA + XML, xmlTestInterface));

        final String URI_2 = "/config/test-interface:interfaces";
        assertEquals(204, post(URI_2, Draft02.MediaTypes.DATA + XML, xmlBlockData));
    }

    private static void initMocking() {
        controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContext);
        mountService = mock(DOMMountPointService.class);
        controllerContext.setMountService(mountService);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
    }

    private int post(final String uri, final String mediaType, final String data) {
        return target(uri).request(mediaType).post(Entity.entity(data, mediaType)).getStatus();
    }

    private static void loadData() throws IOException, URISyntaxException {
        InputStream xmlStream = RestconfImplTest.class
                .getResourceAsStream("/parts/ietf-interfaces_interfaces_absolute_path.xml");
        xmlDataAbsolutePath = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
        xmlStream = RestconfImplTest.class
                .getResourceAsStream("/parts/ietf-interfaces_interfaces_interface_absolute_path.xml");
        xmlDataInterfaceAbsolutePath = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
        final String xmlPathRpcInput = RestconfImplTest.class.getResource("/full-versions/test-data2/data-rpc-input.xml")
                .getPath();
        xmlDataRpcInput = TestUtils.loadTextFile(xmlPathRpcInput);
        final String xmlPathBlockData = RestconfImplTest.class.getResource("/test-config-data/xml/block-data.xml").getPath();
        xmlBlockData = TestUtils.loadTextFile(xmlPathBlockData);
        final String xmlPathTestInterface = RestconfImplTest.class.getResource("/test-config-data/xml/test-interface.xml")
                .getPath();
        xmlTestInterface = TestUtils.loadTextFile(xmlPathTestInterface);
        cnSnDataOutput = prepareCnSnRpcOutput();
        final String data3Input = RestconfImplTest.class.getResource("/full-versions/test-data2/data3.xml").getPath();
        xmlData3 = TestUtils.loadTextFile(data3Input);
        final String data4Input = RestconfImplTest.class.getResource("/full-versions/test-data2/data7.xml").getPath();
        xmlData4 = TestUtils.loadTextFile(data4Input);
    }

    private static CompositeNodeWrapper prepareCnSnRpcOutput() throws URISyntaxException {
        final CompositeNodeWrapper cnSnDataOutput = new CompositeNodeWrapper(new URI("test:module"), "output");
        final CompositeNodeWrapper cont = new CompositeNodeWrapper(new URI("test:module"), "cont-output");
        cnSnDataOutput.addValue(cont);
        cnSnDataOutput.unwrap();
        return cnSnDataOutput;
    }
}
