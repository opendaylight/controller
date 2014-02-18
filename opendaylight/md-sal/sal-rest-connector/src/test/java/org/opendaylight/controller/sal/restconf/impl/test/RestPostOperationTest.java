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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.XML;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Set;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.CompositeNodeWrapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Charsets;

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

    private static MountService mountService;

    @BeforeClass
    public static void init() throws URISyntaxException, IOException {
        schemaContextYangsIetf = TestUtils.loadSchemaContext("/full-versions/yangs");
        schemaContextTestModule = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext = ControllerContext.getInstance();
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);

        Set<Module> modules = TestUtils.loadModulesFrom("/test-config-data/yang1");
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
        return resourceConfig;
    }

    @Test
    public void postOperationsStatusCodes() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextTestModule);
        mockInvokeRpc(cnSnDataOutput, true);
        String uri = "/operations/test-module:rpc-test";
        assertEquals(200, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));

        mockInvokeRpc(null, true);
        assertEquals(204, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));

        mockInvokeRpc(null, false);
        assertEquals(500, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));

        uri = "/operations/test-module:rpc-wrongtest";
        assertEquals(404, post(uri, MediaType.APPLICATION_XML, xmlDataRpcInput));
    }

    @Test
    public void postConfigOnlyStatusCodes() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.COMMITED);
        String uri = "/config";
        assertEquals(204, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));

        mockCommitConfigurationDataPostMethod(null);
        assertEquals(202, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));

        mockCommitConfigurationDataPostMethod(TransactionStatus.FAILED);
        assertEquals(500, post(uri, MediaType.APPLICATION_XML, xmlDataAbsolutePath));
    }

    @Test
    public void postConfigStatusCodes() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        mockCommitConfigurationDataPostMethod(TransactionStatus.COMMITED);
        String uri = "/config/ietf-interfaces:interfaces";
        assertEquals(204, post(uri, MediaType.APPLICATION_XML, xmlDataInterfaceAbsolutePath));

        mockCommitConfigurationDataPostMethod(null);
        assertEquals(202, post(uri, MediaType.APPLICATION_XML, xmlDataInterfaceAbsolutePath));

        mockCommitConfigurationDataPostMethod(TransactionStatus.FAILED);
        assertEquals(500, post(uri, MediaType.APPLICATION_XML, xmlDataInterfaceAbsolutePath));
    }

    @Test
    public void postDataViaUrlMountPoint() throws UnsupportedEncodingException {
        controllerContext.setSchemas(schemaContextYangsIetf);
        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(
                TransactionStatus.COMMITED).build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        when(
                brokerFacade.commitConfigurationDataPostBehindMountPoint(any(MountInstance.class),
                        any(InstanceIdentifier.class), any(CompositeNode.class))).thenReturn(dummyFuture);

        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = "/config/ietf-interfaces:interfaces/interface/0/";
        assertEquals(204, post(uri, Draft02.MediaTypes.DATA + XML, xmlData4));
        uri = "/config/ietf-interfaces:interfaces/interface/0/yang-ext:mount/test-module:cont";
        assertEquals(204, post(uri, Draft02.MediaTypes.DATA + XML, xmlData3));
    }

    private void mockInvokeRpc(CompositeNode result, boolean sucessful) {
        RpcResult<CompositeNode> rpcResult = new DummyRpcResult.Builder<CompositeNode>().result(result)
                .isSuccessful(sucessful).build();
        when(brokerFacade.invokeRpc(any(QName.class), any(CompositeNode.class))).thenReturn(rpcResult);
    }

    private void mockCommitConfigurationDataPostMethod(TransactionStatus statusName) {
        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(statusName)
                .build();
        Future<RpcResult<TransactionStatus>> dummyFuture = null;
        if (statusName != null) {
            dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        } else {
            dummyFuture = DummyFuture.builder().build();
        }

        when(brokerFacade.commitConfigurationDataPost(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(dummyFuture);
    }

    @Test
    public void createConfigurationDataTest() throws UnsupportedEncodingException, ParseException {
        initMocking();
        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(
                TransactionStatus.COMMITED).build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();

        when(brokerFacade.commitConfigurationDataPost(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(dummyFuture);

        ArgumentCaptor<InstanceIdentifier> instanceIdCaptor = ArgumentCaptor.forClass(InstanceIdentifier.class);
        ArgumentCaptor<CompositeNode> compNodeCaptor = ArgumentCaptor.forClass(CompositeNode.class);

        String URI_1 = "/config";
        assertEquals(204, post(URI_1, Draft02.MediaTypes.DATA + XML, xmlTestInterface));
        verify(brokerFacade).commitConfigurationDataPost(instanceIdCaptor.capture(), compNodeCaptor.capture());
        String identifier = "[(urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)interfaces]";
        assertEquals(identifier, instanceIdCaptor.getValue().getPath().toString());

        String URI_2 = "/config/test-interface:interfaces";
        assertEquals(204, post(URI_2, Draft02.MediaTypes.DATA + XML, xmlBlockData));
        verify(brokerFacade, times(2))
                .commitConfigurationDataPost(instanceIdCaptor.capture(), compNodeCaptor.capture());
        identifier = "[(urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)interfaces, (urn:ietf:params:xml:ns:yang:test-interface?revision=2014-07-01)block]";
        assertEquals(identifier, instanceIdCaptor.getValue().getPath().toString());
    }

    @Test
    public void createConfigurationDataNullTest() throws UnsupportedEncodingException {
        initMocking();

        when(brokerFacade.commitConfigurationDataPost(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(null);

        String URI_1 = "/config";
        assertEquals(202, post(URI_1, Draft02.MediaTypes.DATA + XML, xmlTestInterface));

        String URI_2 = "/config/test-interface:interfaces";
        assertEquals(202, post(URI_2, Draft02.MediaTypes.DATA + XML, xmlBlockData));
    }

    private static void initMocking() {
        controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContext);
        mountService = mock(MountService.class);
        controllerContext.setMountService(mountService);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
    }

    private int post(String uri, String mediaType, String data) {
        return target(uri).request(mediaType).post(Entity.entity(data, mediaType)).getStatus();
    }

    private static void loadData() throws IOException, URISyntaxException {
        InputStream xmlStream = RestconfImplTest.class
                .getResourceAsStream("/parts/ietf-interfaces_interfaces_absolute_path.xml");
        xmlDataAbsolutePath = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
        xmlStream = RestconfImplTest.class
                .getResourceAsStream("/parts/ietf-interfaces_interfaces_interface_absolute_path.xml");
        xmlDataInterfaceAbsolutePath = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
        String xmlPathRpcInput = RestconfImplTest.class.getResource("/full-versions/test-data2/data-rpc-input.xml")
                .getPath();
        xmlDataRpcInput = TestUtils.loadTextFile(xmlPathRpcInput);
        String xmlPathBlockData = RestconfImplTest.class.getResource("/test-config-data/xml/block-data.xml").getPath();
        xmlBlockData = TestUtils.loadTextFile(xmlPathBlockData);
        String xmlPathTestInterface = RestconfImplTest.class.getResource("/test-config-data/xml/test-interface.xml")
                .getPath();
        xmlTestInterface = TestUtils.loadTextFile(xmlPathTestInterface);
        cnSnDataOutput = prepareCnSnRpcOutput();
        String data3Input = RestconfImplTest.class.getResource("/full-versions/test-data2/data3.xml").getPath();
        xmlData3 = TestUtils.loadTextFile(data3Input);
        String data4Input = RestconfImplTest.class.getResource("/full-versions/test-data2/data7.xml").getPath();
        xmlData4 = TestUtils.loadTextFile(data4Input);
    }

    private static CompositeNodeWrapper prepareCnSnRpcOutput() throws URISyntaxException {
        CompositeNodeWrapper cnSnDataOutput = new CompositeNodeWrapper(new URI("test:module"), "output");
        CompositeNodeWrapper cont = new CompositeNodeWrapper(new URI("test:module"), "cont-output");
        cnSnDataOutput.addValue(cont);
        cnSnDataOutput.unwrap();
        return cnSnDataOutput;
    }
}
