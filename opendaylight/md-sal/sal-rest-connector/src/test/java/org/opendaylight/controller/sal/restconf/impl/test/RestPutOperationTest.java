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
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.core.api.mount.MountService;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class RestPutOperationTest extends JerseyTest {

    private static String xmlData;
    private static String xmlData2;
    private static String xmlData3;

    private static BrokerFacade brokerFacade;
    private static RestconfImpl restconfImpl;
    private static SchemaContext schemaContextYangsIetf;
    private static SchemaContext schemaContextTestModule;

    @BeforeClass
    public static void init() throws IOException {
        schemaContextYangsIetf = TestUtils.loadSchemaContext("/full-versions/yangs");
        schemaContextTestModule = TestUtils.loadSchemaContext("/full-versions/test-module");
        ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContextYangsIetf);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
        loadData();
    }

    private static void loadData() throws IOException {
        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        xmlData = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
        InputStream xmlStream2 = RestconfImplTest.class.getResourceAsStream("/full-versions/test-data2/data2.xml");
        xmlData2 = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream2));
        InputStream xmlStream3 = RestconfImplTest.class.getResourceAsStream("/full-versions/test-data2/data7.xml");
        xmlData3 = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream3));
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

    /**
     * Tests of status codes for "/config/{identifier}".
     */
    @Test
    public void putConfigStatusCodes() throws UnsupportedEncodingException {
        String uri = "/config/ietf-interfaces:interfaces/interface/eth0";
        mockCommitConfigurationDataPutMethod(TransactionStatus.COMMITED);
        assertEquals(200, put(uri, MediaType.APPLICATION_XML, xmlData));

        mockCommitConfigurationDataPutMethod(TransactionStatus.FAILED);
        assertEquals(500, put(uri, MediaType.APPLICATION_XML, xmlData));
    }

    @Test
    public void testRpcResultCommitedToStatusCodesWithMountPoint() throws UnsupportedEncodingException,
            FileNotFoundException, URISyntaxException {

        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(
                TransactionStatus.COMMITED).build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        when(
                brokerFacade.commitConfigurationDataPutBehindMountPoint(any(MountInstance.class),
                        any(InstanceIdentifier.class), any(CompositeNode.class))).thenReturn(dummyFuture);

        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = "/config/ietf-interfaces:interfaces/interface/0/yang-ext:mount/test-module:cont";
        assertEquals(200, put(uri, MediaType.APPLICATION_XML, xmlData2));

        uri = "/config/ietf-interfaces:interfaces/yang-ext:mount/test-module:cont";
        assertEquals(200, put(uri, MediaType.APPLICATION_XML, xmlData2));
    }

    @Test
    public void putDataMountPointIntoHighestElement() throws UnsupportedEncodingException, URISyntaxException {
        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(
                TransactionStatus.COMMITED).build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        when(
                brokerFacade.commitConfigurationDataPutBehindMountPoint(any(MountInstance.class),
                        any(InstanceIdentifier.class), any(CompositeNode.class))).thenReturn(dummyFuture);

        MountInstance mountInstance = mock(MountInstance.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        MountService mockMountService = mock(MountService.class);
        when(mockMountService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(mountInstance);

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = "/config/ietf-interfaces:interfaces/yang-ext:mount";
        assertEquals(200, put(uri, MediaType.APPLICATION_XML, xmlData3));
    }

    private int put(String uri, String mediaType, String data) throws UnsupportedEncodingException {
        return target(uri).request(mediaType).put(Entity.entity(data, mediaType)).getStatus();
    }

    private void mockCommitConfigurationDataPutMethod(TransactionStatus statusName) {
        RpcResult<TransactionStatus> rpcResult = new DummyRpcResult.Builder<TransactionStatus>().result(statusName)
                .build();
        Future<RpcResult<TransactionStatus>> dummyFuture = DummyFuture.builder().rpcResult(rpcResult).build();
        when(brokerFacade.commitConfigurationDataPut(any(InstanceIdentifier.class), any(CompositeNode.class)))
                .thenReturn(dummyFuture);
    }

}
