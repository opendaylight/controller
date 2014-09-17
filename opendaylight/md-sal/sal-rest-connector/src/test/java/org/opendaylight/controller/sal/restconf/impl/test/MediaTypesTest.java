/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.JSON;
import static org.opendaylight.controller.sal.restconf.impl.test.RestOperationUtils.XML;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.rest.impl.JsonToCompositeNodeProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToJsonProvider;
import org.opendaylight.controller.sal.rest.impl.StructuredDataToXmlProvider;
import org.opendaylight.controller.sal.rest.impl.XmlToCompositeNodeProvider;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class MediaTypesTest extends JerseyTest {

    private static RestconfService restconfService;
    private static String jsonData;
    private static String xmlData;

    @BeforeClass
    public static void init() throws IOException {
        restconfService = mock(RestconfService.class);
        String jsonPath = RestconfImplTest.class.getResource("/parts/ietf-interfaces_interfaces.json").getPath();
        jsonData = TestUtils.loadTextFile(jsonPath);
        InputStream xmlStream = RestconfImplTest.class.getResourceAsStream("/parts/ietf-interfaces_interfaces.xml");
        xmlData = TestUtils.getDocumentInPrintableForm(TestUtils.loadDocumentFrom(xmlStream));
    }

    @Override
    protected Application configure() {
        /* enable/disable Jersey logs to console */
        // enable(TestProperties.LOG_TRAFFIC);
        // enable(TestProperties.DUMP_ENTITY);
        // enable(TestProperties.RECORD_LOG_LEVEL);
        // set(TestProperties.RECORD_LOG_LEVEL, Level.ALL.intValue());
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig = resourceConfig.registerInstances(restconfService, StructuredDataToXmlProvider.INSTANCE,
                StructuredDataToJsonProvider.INSTANCE, XmlToCompositeNodeProvider.INSTANCE,
                JsonToCompositeNodeProvider.INSTANCE);
        return resourceConfig;
    }

    @Test
    public void testPostOperationsWithInputDataMediaTypes() throws UnsupportedEncodingException {
        String uriPrefix = "/operations/";
        String uriPath = "ietf-interfaces:interfaces";
        String uri = uriPrefix + uriPath;
        when(restconfService.invokeRpc(eq(uriPath), any(CompositeNode.class), any(UriInfo.class))).thenReturn(null);
        post(uri, Draft02.MediaTypes.OPERATION + JSON, Draft02.MediaTypes.OPERATION + JSON, jsonData);
        verify(restconfService, times(1)).invokeRpc(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
        post(uri, Draft02.MediaTypes.OPERATION + XML, Draft02.MediaTypes.OPERATION + XML, xmlData);
        verify(restconfService, times(2)).invokeRpc(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
        post(uri, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON, jsonData);
        verify(restconfService, times(3)).invokeRpc(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
        post(uri, MediaType.APPLICATION_XML, MediaType.APPLICATION_XML, xmlData);
        verify(restconfService, times(4)).invokeRpc(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
        post(uri, MediaType.TEXT_XML, MediaType.TEXT_XML, xmlData);
        verify(restconfService, times(5)).invokeRpc(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
        post(uri, null, MediaType.TEXT_XML, xmlData);
        verify(restconfService, times(6)).invokeRpc(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));

        // negative tests
        post(uri, MediaType.TEXT_PLAIN, MediaType.TEXT_XML, xmlData);
        verify(restconfService, times(6)).invokeRpc(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
        post(uri, MediaType.TEXT_XML, MediaType.TEXT_PLAIN, xmlData);
        verify(restconfService, times(6)).invokeRpc(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
    }

    @Test
    public void testGetConfigMediaTypes() throws UnsupportedEncodingException {
        String uriPrefix = "/config/";
        String uriPath = "ietf-interfaces:interfaces";
        String uri = uriPrefix + uriPath;
        when(restconfService.readConfigurationData(eq(uriPath), any(UriInfo.class))).thenReturn(null);
        get(uri, Draft02.MediaTypes.DATA + JSON);
        verify(restconfService, times(1)).readConfigurationData(eq(uriPath), any(UriInfo.class));
        get(uri, Draft02.MediaTypes.DATA + XML);
        verify(restconfService, times(2)).readConfigurationData(eq(uriPath), any(UriInfo.class));
        get(uri, MediaType.APPLICATION_JSON);
        verify(restconfService, times(3)).readConfigurationData(eq(uriPath), any(UriInfo.class));
        get(uri, MediaType.APPLICATION_XML);
        verify(restconfService, times(4)).readConfigurationData(eq(uriPath), any(UriInfo.class));
        get(uri, MediaType.TEXT_XML);
        verify(restconfService, times(5)).readConfigurationData(eq(uriPath), any(UriInfo.class));

        // negative tests
        get(uri, MediaType.TEXT_PLAIN);
        verify(restconfService, times(5)).readConfigurationData(eq(uriPath), any(UriInfo.class));
    }

    @Test
    public void testGetOperationalMediaTypes() throws UnsupportedEncodingException {
        String uriPrefix = "/operational/";
        String uriPath = "ietf-interfaces:interfaces";
        String uri = uriPrefix + uriPath;
        when(restconfService.readOperationalData(eq(uriPath), any(UriInfo.class))).thenReturn(null);
        get(uri, Draft02.MediaTypes.DATA + JSON);
        verify(restconfService, times(1)).readOperationalData(eq(uriPath), any(UriInfo.class));
        get(uri, Draft02.MediaTypes.DATA + XML);
        verify(restconfService, times(2)).readOperationalData(eq(uriPath), any(UriInfo.class));
        get(uri, MediaType.APPLICATION_JSON);
        verify(restconfService, times(3)).readOperationalData(eq(uriPath), any(UriInfo.class));
        get(uri, MediaType.APPLICATION_XML);
        verify(restconfService, times(4)).readOperationalData(eq(uriPath), any(UriInfo.class));
        get(uri, MediaType.TEXT_XML);
        verify(restconfService, times(5)).readOperationalData(eq(uriPath), any(UriInfo.class));

        // negative tests
        get(uri, MediaType.TEXT_PLAIN);
        verify(restconfService, times(5)).readOperationalData(eq(uriPath), any(UriInfo.class));
    }

    @Test
    public void testPutConfigMediaTypes() throws UnsupportedEncodingException {
        String uriPrefix = "/config/";
        String uriPath = "ietf-interfaces:interfaces";
        String uri = uriPrefix + uriPath;
        when(restconfService.updateConfigurationData(eq(uriPath), any(CompositeNode.class))).thenReturn(null);
        put(uri, null, Draft02.MediaTypes.DATA + JSON, jsonData);
        verify(restconfService, times(1)).updateConfigurationData(eq(uriPath), any(CompositeNode.class));
        put(uri, null, Draft02.MediaTypes.DATA + XML, xmlData);
        verify(restconfService, times(2)).updateConfigurationData(eq(uriPath), any(CompositeNode.class));
        put(uri, null, MediaType.APPLICATION_JSON, jsonData);
        verify(restconfService, times(3)).updateConfigurationData(eq(uriPath), any(CompositeNode.class));
        put(uri, null, MediaType.APPLICATION_XML, xmlData);
        verify(restconfService, times(4)).updateConfigurationData(eq(uriPath), any(CompositeNode.class));
        put(uri, null, MediaType.TEXT_XML, xmlData);
        verify(restconfService, times(5)).updateConfigurationData(eq(uriPath), any(CompositeNode.class));
        put(uri, "fooMediaType", MediaType.TEXT_XML, xmlData);
        verify(restconfService, times(6)).updateConfigurationData(eq(uriPath), any(CompositeNode.class));
    }

    @Test
    public void testPostConfigWithPathMediaTypes() throws UnsupportedEncodingException {
        String uriPrefix = "/config/";
        String uriPath = "ietf-interfaces:interfaces";
        String uri = uriPrefix + uriPath;
        when(restconfService.createConfigurationData(eq(uriPath), any(CompositeNode.class), any(UriInfo.class))).thenReturn(null);
        post(uri, null, Draft02.MediaTypes.DATA + JSON, jsonData);
        verify(restconfService, times(1)).createConfigurationData(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
        post(uri, null, Draft02.MediaTypes.DATA + XML, xmlData);
        verify(restconfService, times(2)).createConfigurationData(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
        post(uri, null, MediaType.APPLICATION_JSON, jsonData);
        verify(restconfService, times(3)).createConfigurationData(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
        post(uri, null, MediaType.APPLICATION_XML, xmlData);
        verify(restconfService, times(4)).createConfigurationData(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
        post(uri, null, MediaType.TEXT_XML, xmlData);
        verify(restconfService, times(5)).createConfigurationData(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
        post(uri, "fooMediaType", MediaType.TEXT_XML, xmlData);
        verify(restconfService, times(6)).createConfigurationData(eq(uriPath), any(CompositeNode.class), any(UriInfo.class));
    }

    @Test
    public void testPostConfigMediaTypes() throws UnsupportedEncodingException {
        String uriPrefix = "/config/";
        String uri = uriPrefix;
        when(restconfService.createConfigurationData(any(CompositeNode.class), any(UriInfo.class))).thenReturn(null);
        post(uri, null, Draft02.MediaTypes.DATA + JSON, jsonData);
        verify(restconfService, times(1)).createConfigurationData(any(CompositeNode.class), any(UriInfo.class));
        post(uri, null, Draft02.MediaTypes.DATA + XML, xmlData);
        verify(restconfService, times(2)).createConfigurationData(any(CompositeNode.class), any(UriInfo.class));
        post(uri, null, MediaType.APPLICATION_JSON, jsonData);
        verify(restconfService, times(3)).createConfigurationData(any(CompositeNode.class), any(UriInfo.class));
        post(uri, null, MediaType.APPLICATION_XML, xmlData);
        verify(restconfService, times(4)).createConfigurationData(any(CompositeNode.class), any(UriInfo.class));
        post(uri, null, MediaType.TEXT_XML, xmlData);
        verify(restconfService, times(5)).createConfigurationData(any(CompositeNode.class), any(UriInfo.class));
        post(uri, "fooMediaType", MediaType.TEXT_XML, xmlData);
        verify(restconfService, times(6)).createConfigurationData(any(CompositeNode.class), any(UriInfo.class));
    }

    @Test
    public void testDeleteConfigMediaTypes() throws UnsupportedEncodingException {
        String uriPrefix = "/config/";
        String uriPath = "ietf-interfaces:interfaces";
        String uri = uriPrefix + uriPath;
        when(restconfService.deleteConfigurationData(eq(uriPath))).thenReturn(null);
        target(uri).request("fooMediaType").delete();
        verify(restconfService, times(1)).deleteConfigurationData(uriPath);
    }

    private int get(String uri, String acceptMediaType) {
        return target(uri).request(acceptMediaType).get().getStatus();
    }

    private int put(String uri, String acceptMediaType, String contentTypeMediaType, String data) {
        if (acceptMediaType == null) {
            return target(uri).request().put(Entity.entity(data, contentTypeMediaType)).getStatus();
        }
        return target(uri).request(acceptMediaType).put(Entity.entity(data, contentTypeMediaType)).getStatus();
    }

    private int post(String uri, String acceptMediaType, String contentTypeMediaType, String data) {
        if (acceptMediaType == null) {
            if (contentTypeMediaType == null || data == null) {
                return target(uri).request().post(null).getStatus();
            }
            return target(uri).request().post(Entity.entity(data, contentTypeMediaType)).getStatus();
        }
        if (contentTypeMediaType == null || data == null) {
            return target(uri).request(acceptMediaType).post(null).getStatus();
        }
        return target(uri).request(acceptMediaType).post(Entity.entity(data, contentTypeMediaType)).getStatus();
    }

}
