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
import static org.mockito.Mockito.when;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.rest.impl.JsonNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeJsonBodyWriter;
import org.opendaylight.controller.sal.rest.impl.NormalizedNodeXmlBodyWriter;
import org.opendaylight.controller.sal.rest.impl.RestconfDocumentedExceptionMapper;
import org.opendaylight.controller.sal.rest.impl.XmlNormalizedNodeBodyReader;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
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
        final ControllerContext controllerContext = ControllerContext.getInstance();
        controllerContext.setSchemas(schemaContextYangsIetf);
        brokerFacade = mock(BrokerFacade.class);
        restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
        loadData();
    }

    private static void loadData() throws IOException {
        final InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        xmlData = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
        final InputStream xmlStream2 = RestconfImplTest.class.getResourceAsStream("/full-versions/test-data2/data2.xml");
        xmlData2 = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream2));
        final InputStream xmlStream3 = RestconfImplTest.class.getResourceAsStream("/full-versions/test-data2/data7.xml");
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
        resourceConfig = resourceConfig.registerInstances(restconfImpl,new XmlNormalizedNodeBodyReader(),
                new NormalizedNodeXmlBodyWriter(), new JsonNormalizedNodeBodyReader(), new NormalizedNodeJsonBodyWriter());
        resourceConfig.registerClasses(RestconfDocumentedExceptionMapper.class);
        return resourceConfig;
    }

    /**
     * Tests of status codes for "/config/{identifier}".
     */
    @Test
    public void putConfigStatusCodes() throws UnsupportedEncodingException {
        final String uri = "/config/ietf-interfaces:interfaces/interface/eth0";
        mockCommitConfigurationDataPutMethod(true);
        assertEquals(200, put(uri, MediaType.APPLICATION_XML, xmlData));

        mockCommitConfigurationDataPutMethod(false);
        assertEquals(500, put(uri, MediaType.APPLICATION_XML, xmlData));

        assertEquals(400, put(uri, MediaType.APPLICATION_JSON, ""));
    }

    @Test
    public void putConfigStatusCodesEmptyBody() throws UnsupportedEncodingException {
        final String uri = "/config/ietf-interfaces:interfaces/interface/eth0";
        final Response resp = target(uri).request(MediaType.APPLICATION_JSON).put(
                Entity.entity("", MediaType.APPLICATION_JSON));
        assertEquals(400, put(uri, MediaType.APPLICATION_JSON, ""));
    }

    @Test
    @Ignore // jenkins has problem with JerseyTest - we expecting problems with singletons ControllerContext as schemaContext holder
    public void testRpcResultCommitedToStatusCodesWithMountPoint() throws UnsupportedEncodingException,
            FileNotFoundException, URISyntaxException {

        final CheckedFuture<Void, TransactionCommitFailedException> dummyFuture = mock(CheckedFuture.class);

        when(
                brokerFacade.commitConfigurationDataPut(any(DOMMountPoint.class), any(YangInstanceIdentifier.class),
                        any(NormalizedNode.class))).thenReturn(dummyFuture);

        final DOMMountPoint mountInstance = mock(DOMMountPoint.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        final DOMMountPointService mockMountService = mock(DOMMountPointService.class);
        when(mockMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountInstance));

        ControllerContext.getInstance().setMountService(mockMountService);

        String uri = "/config/ietf-interfaces:interfaces/interface/0/yang-ext:mount/test-module:cont";
        assertEquals(200, put(uri, MediaType.APPLICATION_XML, xmlData2));

        uri = "/config/ietf-interfaces:interfaces/yang-ext:mount/test-module:cont";
        assertEquals(200, put(uri, MediaType.APPLICATION_XML, xmlData2));
    }

    @Test
    public void putDataMountPointIntoHighestElement() throws UnsupportedEncodingException, URISyntaxException {
        final CheckedFuture<Void, TransactionCommitFailedException> dummyFuture = mock(CheckedFuture.class);
        when(
                brokerFacade.commitConfigurationDataPut(any(DOMMountPoint.class), any(YangInstanceIdentifier.class),
                        any(NormalizedNode.class))).thenReturn(dummyFuture);

        final DOMMountPoint mountInstance = mock(DOMMountPoint.class);
        when(mountInstance.getSchemaContext()).thenReturn(schemaContextTestModule);
        final DOMMountPointService mockMountService = mock(DOMMountPointService.class);
        when(mockMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountInstance));

        ControllerContext.getInstance().setMountService(mockMountService);

        final String uri = "/config/ietf-interfaces:interfaces/yang-ext:mount";
        assertEquals(200, put(uri, MediaType.APPLICATION_XML, xmlData3));
    }

    @Test
    public void putWithOptimisticLockFailedException() throws UnsupportedEncodingException {

        final String uri = "/config/ietf-interfaces:interfaces/interface/eth0";

        doThrow(OptimisticLockFailedException.class).
            when(brokerFacade).commitConfigurationDataPut(
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));

        assertEquals(500, put(uri, MediaType.APPLICATION_XML, xmlData));

        doThrow(OptimisticLockFailedException.class).doReturn(mock(CheckedFuture.class)).
            when(brokerFacade).commitConfigurationDataPut(
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));

        assertEquals(200, put(uri, MediaType.APPLICATION_XML, xmlData));
    }

    @Test
    public void putWithTransactionCommitFailedException() throws UnsupportedEncodingException {

        final String uri = "/config/ietf-interfaces:interfaces/interface/eth0";

        doThrow(TransactionCommitFailedException.class).
            when(brokerFacade).commitConfigurationDataPut(
                any(YangInstanceIdentifier.class), any(NormalizedNode.class));

        assertEquals(500, put(uri, MediaType.APPLICATION_XML, xmlData));
    }

    private int put(final String uri, final String mediaType, final String data) throws UnsupportedEncodingException {
        return target(uri).request(mediaType).put(Entity.entity(data, mediaType)).getStatus();
    }

    private void mockCommitConfigurationDataPutMethod(final boolean noErrors) {
        if (noErrors) {
            doReturn(mock(CheckedFuture.class)).when(brokerFacade).commitConfigurationDataPut(
                    any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        } else {
            doThrow(RestconfDocumentedException.class).when(brokerFacade).commitConfigurationDataPut(
                    any(YangInstanceIdentifier.class), any(NormalizedNode.class));
        }
    }

}
